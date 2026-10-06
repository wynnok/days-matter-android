package top.zwtx.daysmatter.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import top.zwtx.daysmatter.MainViewModel
import top.zwtx.daysmatter.data.ImportOutcome

@Composable
fun BackupImportDialog(vm: MainViewModel) {
  if (vm.showingImportOutcome && (vm.importOutcome == ImportOutcome.UNKNOWN || vm.importOutcome == ImportOutcome.REFRESH_FAILED)) {
    val unknown = vm.importOutcome == ImportOutcome.UNKNOWN
    AlertDialog(onDismissRequest = vm::viewImportedData,
      title = { Text(if (unknown) "核实导入结果" else "刷新账号数据") },
      text = { Text(if (unknown) "导入结果未知，可能已追加，请先刷新并核实数据，避免重复导入" else "导入请求已完成，但刷新失败，请仅刷新账号数据") },
      confirmButton = { TextButton(onClick = vm::refreshImportData, enabled = !vm.busy) { Text("仅刷新账号数据") } },
      dismissButton = { TextButton(onClick = vm::viewImportedData, enabled = !vm.busy) { Text("查看账号数据") } })
  }
  vm.importPreview?.let { preview ->
    AlertDialog(onDismissRequest = vm::cancelImport, title = { Text("确认追加导入") },
      text = { Text("${preview.summary}\n以上是文件数量，并非实际导入数。追加、不覆盖，可能重复；不恢复本地提醒、外观或小组件实例。") },
      confirmButton = { TextButton(onClick = vm::confirmImport, enabled = !vm.busy) { Text(if (vm.busy) "导入中…" else "确认追加") } },
      dismissButton = { TextButton(onClick = vm::cancelImport, enabled = !vm.busy) { Text("取消") } })
  }
}
