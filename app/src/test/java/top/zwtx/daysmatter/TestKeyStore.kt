package top.zwtx.daysmatter

import org.junit.rules.ExternalResource
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.Security
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import javax.crypto.spec.SecretKeySpec

/** Only replaces the device key vault; LocalStore still encrypts and decrypts real test files. */
class TestKeyStore : ExternalResource() {
  override fun before() {
    Security.addProvider(object : Provider("AndroidKeyStore", 1.0, "Test device vault") {
      init { put("KeyStore.AndroidKeyStore", DeviceKeyVault::class.java.name) }
    })
  }
  override fun after() { Security.removeProvider("AndroidKeyStore") }
}

class DeviceKeyVault : KeyStoreSpi() {
  override fun engineGetKey(alias: String?, password: CharArray?): Key = SecretKeySpec(ByteArray(32) { 1 }, "AES")
  override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
  override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit
  override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null
  override fun engineGetCertificate(alias: String?): Certificate? = null
  override fun engineGetCreationDate(alias: String?): Date = Date(0)
  override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<Certificate>?) = Unit
  override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<Certificate>?) = Unit
  override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) = Unit
  override fun engineDeleteEntry(alias: String?) = Unit
  override fun engineAliases() = Collections.enumeration(listOf("days_matter_session_v1"))
  override fun engineContainsAlias(alias: String?) = true
  override fun engineSize() = 1
  override fun engineIsKeyEntry(alias: String?) = true
  override fun engineIsCertificateEntry(alias: String?) = false
  override fun engineGetCertificateAlias(cert: Certificate?): String? = null
}
