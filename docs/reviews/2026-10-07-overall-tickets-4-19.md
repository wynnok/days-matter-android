# #4–#19 整体实施审查

日期：2026-10-07。依据 GitHub Issues 的正文、评论和最新交接文件检查需求覆盖、实现方向、组合流程及优化空间。执行 implement、TDD 和 code-review 技能；Standards 与 Spec 由独立审查 agent 并行审查，修复后复审。

## 范围与固定基点

- Android：`git diff 1b6c1be...762f8599888feee86202e4e170850b29077fb38e`，实施前基点至独立分支 `codex/tickets-16-19` 的最终代码。
- 网页／Worker：`git diff 48d9283...3f7b1ac80a179f0300fd32eb82bcd059e4cbba96`，共通 ticket 实施前基点至最终代码。
- 需求来源：GitHub #4–#19，以及父规格 #2、#3 中对应部分。最初审查代码为 Android `734c0cd`、网页 `075498f`，后续针对修复增量复审。
- 用户澄清 #20–#22 当时未实施，本轮未将其作为已完成任务验收。收尾发现另一会话已在主目录新增 #20、#21 提交及未提交的小组件后台修改，这些并行变化不属于本轮固定审查范围。
- 总体结论：未发现 #4–#19 明确的整项遗漏或偏离需求的功能扩张；发现的组合流程缺陷已修复。自动化通过不替代实体设备与真实桌面验收，也不代表父规格全部完成。

## 需求覆盖

| Ticket | 核对内容 | 主要自动化证据 |
|---|---|---|
| #4 | 两端日期展示、发生日与跨日天数一致 | OccurrenceDisplayTest；date-display、date、mirror |
| #5 | 账号摘要、事件与近期数量 | AccountOverviewScreenTest、EventOverviewTest；account-overview |
| #6 | 资料提交、反馈、失败输入保留、权威资料刷新 | ProfileSaveScreenTest；profile-save，补充资料读写竞态回归 |
| #7 | 事件／子事件／分类增删改与刷新反馈 | CrudScreenTest；crud-feedback、category-delete，补充账号切换竞态回归 |
| #8 | 加载、空内容、失败与登录失效 | LoadStatesScreenTest；loading-state |
| #9 | 提醒渠道启停、校验、测试与反馈 | ChannelScreenTest；channel-form、channel-test、channel-worker |
| #10 | 浅色／深色／跟随系统外观 | AppearanceScreenTest；appearance |
| #11 | 帮助、版本、声明入口及共通说明 | HelpScreenTest；网页入口及 appearance 检查 |
| #12 | 备份导出、真实文件结果与取消反馈 | BackupDocumentsTest、BackupSelectionLifecycleTest；backup-export |
| #13 | 追加导入预览、确认、完整数据刷新 | BackupPreviewTest、BackupImportScreenTest、BackupSelectionLifecycleTest；backup-import、backup-worker |
| #14 | 导入未知结果与刷新失败区分 | BackupImportScreenTest；backup-import、backup-worker |
| #15 | 离线阅读与前台联网恢复 | ForegroundRecoveryTest |
| #16 | 通知定位事件、过期事件与账号回退 | NotificationRoutingTest、EventEntryScreenTest、ActivityEntryLifecycleTest |
| #17 | 本地提醒概览、系统权限与测试通知 | DeviceManagementScreenTest |
| #18 | 我的资料摘要与管理分组 | DeviceManagementScreenTest |
| #19 | 重要日子小组件、实例配置、账号／事件失效与管理入口 | ImportantDayWidgetTest、WidgetConfigurationFlowTest、WidgetSyncIntegrationTest，上一轮原生 RemoteViews 预览 |

以上是主要证据索引，不将测试文件存在等同于所有设备验收完成。

## Standards

初审发现 1 项硬规则问题：分类不存在的 Worker 分支在业务响应中填写 404，但实际 HTTP 状态仍为 200，不符合网页仓库规定的 HTTP 错误语义。已在 `src/routes/api.ts` 修正为实际 HTTP 404，并增加真实路由回归测试。

最终复审：硬规则问题 0 项，需要修改的 heuristic 0 项。网页遗留的客户端事件对象周期计算路径属于可选维护优化，未要求本轮重构。

## Spec

初审及修复后的竞态复审共发现 6 组需求行为缺陷，均已修复：

1. 网页刷新账号数据没有获取服务器资料，跨端修改昵称后仍显示旧 localStorage。加入资料读取、账号归属检查与统一资料更新路径。
2. 网页 CRUD 的确认、请求结果及刷新回调缺少会话检查，旧账号响应可能关闭新账号编辑器、展示错误反馈，或在账号切换后继续删除。请求前后、异常、收尾与刷新反馈均检查所属会话。
3. Android 文件选择期间的导出内容、导入归属放在 Compose remember 中，界面重建后丢失，导致导出没有真正写文件或导入没有预览。将待处理选择交给 ViewModel，保留取消、写入、读取与账号归属反馈。
4. 网页较早发起的资料 GET／登录检查可能在资料 PUT 成功后回写旧昵称。加入资料版本标记及提交期间保护，较旧读取不得覆盖已确认保存。
5. Android 登出后若重建同类选择，新账号选择可能被旧 URI 回调消费。清除旧账号与导出内容，但保留无敏感内容的待回调标记；旧回调完成前拒绝替换，随后允许新账号重新选择。
6. 网页旧账号资料保存的 finally 会解锁新账号仍在提交的按钮。按钮和保存状态恢复均放入当前会话检查，避免重复提交。

最终复审：缺失／部分实现、需求外扩张和错误实现的剩余问题均为 0 项；这表示固定范围内未发现剩余问题，不作无缺陷保证。

## 修复提交

Android 独立分支：

- `b5be43c`：保留文件选择上下文，增加公共页面至实际 ActivityResult 回调的生命周期回归。
- `762f859`：避免账号切换后旧文件 URI 消费新的选择，补充两种选择的跨账号回归。

网页 main：

- `958d625`：服务器资料刷新、CRUD 会话隔离、实际 HTTP 404。
- `e2dfae9`：资料保存与延迟读取之间的版本保护。
- `3f7b1ac`：旧保存回调不解锁新会话提交按钮。

新增回归均先复现错误，再修复并运行对应测试；未增加依赖或直接编辑生成的网页入口文件。

## 最终验证

- Android：`testDebugUnitTest assembleRelease --offline` 成功；XML 汇总 23 个测试类、99 项测试，失败／错误／跳过均为 0。生成 Release APK。
- 网页：HTML 修改后执行 `npm run build`；最终完整测试 170 项通过，失败／跳过均为 0。
- Worker：Wrangler deploy dry-run 打包成功，仅本地验证，没有部署。
- Android 验证位于上述独立 worktree 的最终代码。网页验证位于上述最终提交。没有把并行主目录新增代码包含在这些数字内。

## 交付边界及后续优化

1. **整合审查修复**：收尾时 Android 主目录 HEAD 为 `fd8af5f`，包含合并原 #16–#19 的 `f52131f`、#20 的 `08e9fe9` 和 #21 的 `fd8af5f`，另有未提交后台相关修改。本轮 Android 修复 `b5be43c`、`762f859` 尚在独立分支，需在保留并行工作的前提下整合，再验证组合版本。本轮未移动主目录 HEAD 或修改其工作区。
2. **实体设备验收**：实际通知权限／点击、SAF 系统选择器与旋转、不同 Android 版本、真实 launcher 的添加／调整尺寸／配置行为仍需验证。本轮 Robolectric、Compose 和 RemoteViews 证据覆盖框架行为，未宣称真实设备验收通过。
3. **周期计算遗留路径清理（可选）**：网页 `DateUtils.getNextOccurrence(eventObj)` 及相关周期算法仍保留；当前事件派生使用服务器 `server_next_occurrence`，对象计算路径没有生产调用。可在独立维护任务中核对调用与测试依赖后删除，减少重复业务规则，保留确有调用的字符串日期路径。
4. **文档状态维护（可选）**：历史计划中的“尚未实施”应明确为当时快照；以 ticket、最新交接和本报告描述当前状态，避免读者把原计划状态视为当前结论。
5. **#20–#22 单独验收**：需要对另一会话的最终提交和组合版本另设固定基点审查，不以本轮结论覆盖。父规格的全部完成状态应待这些任务验收后判断。

本轮没有推送、发布、部署或改变 GitHub issue 状态。

审查结论：Standards 剩余 0 项；Spec 剩余 0 项；各轴已发现的最高影响问题均已修复。
