package top.zwtx.daysmatter.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Upload
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
    AlertDialog(onDismissRequest = vm::cancelImport, icon = { Icon(Icons.Outlined.Upload, null) }, title = { Text("确认追加导入") },
      text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("备份文件中的数据", style = MaterialTheme.typography.labelLarge)
        BackupCounts(preview.data.getJSONObject("data"))
        Text("追加，不覆盖现有数据", style = MaterialTheme.typography.titleSmall)
        SettingsNote("以上是文件数量，并非实际导入数。重复导入可能产生重复数据；不恢复本地提醒、外观或小组件实例。")
      } },
      confirmButton = { Button(onClick = vm::confirmImport, enabled = !vm.busy) { Text(if (vm.busy) "导入中…" else "确认追加") } },
      dismissButton = { TextButton(onClick = vm::cancelImport, enabled = !vm.busy) { Text("取消") } })
  }
}
