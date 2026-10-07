package top.zwtx.daysmatter.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Share overlapping reads, but never accept a read from before a mutation finished. */
internal object SnapshotRequests {
  private data class Account(val backend: String, val userId: Int)
  private data class Request(val account: Account, val token: String, val generation: Long)
  private val lock = Any()
  private val generations = mutableMapOf<Account, Long>()
  private val pending = mutableMapOf<Request, CompletableDeferred<Snapshot>>()

  fun invalidate(backend: String, session: Session) = synchronized(lock) {
    val account = Account(backend.trimEnd('/'), session.userId)
    generations[account] = (generations[account] ?: 0L) + 1
  }

  suspend fun share(backend: String, session: Session, fetch: suspend () -> Snapshot): Snapshot {
    val account = Account(backend.trimEnd('/'), session.userId)
    while (true) {
      val (key, request, leader) = synchronized(lock) {
        val key = Request(account, session.token, generations[account] ?: 0L)
        pending[key]?.let { Triple(key, it, false) }
          ?: CompletableDeferred<Snapshot>().let { pending[key] = it; Triple(key, it, true) }
      }
      val result = runCatching {
        if (leader) fetch().also { request.complete(it) } else request.await()
      }
      if (leader) {
        result.exceptionOrNull()?.let { request.completeExceptionally(it) }
        synchronized(lock) { pending.remove(key) }
      }
      if (result.exceptionOrNull() is CancellationException) {
        // A stopped background owner must not cancel another active caller sharing its read.
        currentCoroutineContext().ensureActive()
        continue
      }
      if (synchronized(lock) { key.generation == (generations[account] ?: 0L) }) return result.getOrThrow()
      // A write completed while this read was pending. Read the new generation instead.
    }
  }
}
