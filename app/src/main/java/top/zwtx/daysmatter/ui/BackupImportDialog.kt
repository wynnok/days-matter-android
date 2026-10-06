package top.zwtx.daysmatter.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import top.zwtx.daysmatter.MainViewModel

@Composable
fun BackupImportDialog(vm: MainViewModel) {
  vm.importPreview?.let { preview ->
    AlertDialog(onDismissRequest = vm::cancelImport, title = { Text("确认追加导入") },
      text = { Text("${preview.summary}\n以上是文件数量，并非实际导入数。追加、不覆盖，可能重复；不恢复本地提醒、外观或小组件实例。") },
      confirmButton = { TextButton(onClick = vm::confirmImport, enabled = !vm.busy) { Text(if (vm.busy) "导入中…" else "确认追加") } },
      dismissButton = { TextButton(onClick = vm::cancelImport, enabled = !vm.busy) { Text("取消") } })
  }
}
