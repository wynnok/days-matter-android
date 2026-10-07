# #4–#22 整体实施复核

日期：2026-10-07。本报告接续 [#4–#19 审查](2026-10-07-overall-tickets-4-19.md)，纳入另一会话完成的 #20–#22，并复核此前修复与新代码的组合行为。历史交接中的“尚未实施”不代表本报告时点的状态。

## 范围与依据

- 用户确认的固定基点：Android `1b6c1be9f7cb1a03ba3e07012873c1cbecbbed21`；网页／Worker `48d9283`。
- 本轮完整审查代码：独立 worktree 将 main 的 `ba7fa41` 合入此前审查修复分支，得到 `7ea6ea1`；修复后的生产代码为 `b253287ec74fc82be40e1251cc186c2edbca4cbb`。
- Android 命令：`git diff 1b6c1be...b253287`；网页命令：`git diff 48d9283...3f7b1ac`。复审修复增量：`git diff 7ea6ea1...b253287`。
- 通过仓库规定的 gh CLI 重新读取 GitHub #2–#22 正文、评论、标签与状态，以 #4–#22 各 ticket 和父规格 #2、#3 为需求依据；最新实施交接为 `handoff/2026-10-07-tickets-20-22.md`。
- implement 要求的 Standards／Spec 两路独立审查已完成；修复采用已确认公共边界的 TDD，均有失败复现和通过验证。

结论：未发现整项功能遗漏或需求外扩张；本轮发现的状态、取消及布局问题均已修复。真实设备／Launcher 验收尚未完成，不能将代码检查或模拟框架测试等同于所有验收条件已满足。

## Ticket 覆盖

| Ticket | 核对的核心行为 | 主要验证入口 |
|---|---|---|
| #4 | 两端确认发生日期、跨日天数、重复过期待同步 | OccurrenceDisplay；网页 date/date-display/mirror |
| #5 | 账号摘要和近期数量不受当前分类影响 | AccountOverviewScreen、EventOverview；account-overview |
| #6 | 资料保存、失败保留输入、权威刷新和读写竞态 | ProfileSaveScreen；profile-save |
| #7 | 事件、子事件、分类 CRUD、确认与刷新结果及账号隔离 | CrudScreen；crud-feedback、category-delete |
| #8 | 加载、空数据、失败、登录失效区分 | LoadStatesScreen；loading-state |
| #9 | 站外提醒启停、引用保护、测试请求反馈 | ChannelScreen；channel-form/test/worker |
| #10 | 浅色、深色、跟随系统的设备外观 | AppearanceScreen；appearance |
| #11 | 共通帮助、版本、声明入口 | HelpScreen；网页入口核对 |
| #12 | 账号备份真实文件结果、取消、重建及跨账号归属 | BackupDocuments、BackupSelectionLifecycle；backup-export |
| #13 | 追加导入预览、确认、完整刷新及选择生命周期 | BackupPreview、BackupImportScreen、BackupSelectionLifecycle；backup-import/worker |
| #14 | 导入未知结果与已导入但刷新失败分别反馈 | BackupImportScreen；backup-import/worker |
| #15 | 离线缓存阅读、前台联网恢复、草稿保留 | ForegroundRecovery |
| #16 | 通知事件详情定位、旧账号和失效事件回退 | NotificationRouting、EventEntryScreen、ActivityEntryLifecycle |
| #17 | 本地提醒概览、权限入口及测试通知 | DeviceManagementScreen |
| #18 | 我的资料摘要与管理分组 | DeviceManagementScreen |
| #19 | 重要日子实例绑定、配置、阅读、失效和管理入口 | ImportantDayWidget、WidgetConfigurationFlow、WidgetSyncIntegration |
| #20 | 7／30 天、分类、3／5 条稳定排序、日期未知／空状态、详情与首页点击 | UpcomingWidget、WidgetConfigurationFlow；原生尺寸与长名渲染 |
| #21 | 两类预览、低版本重配、多实例独立外观、取消和帮助 | DeviceManagementScreen、WidgetConfigurationFlow、WidgetPlatformCompatibility |
| #22 | 共享快照、小时任务、跨日／重启／调时、离线恢复、401遮蔽、写入代次 | WidgetBackgroundRefresh、WidgetSyncIntegration、ForegroundRecovery |

#19–#22 的实际桌面、系统添加／重配和后台延迟检查仍缺实体设备证据，详见交付边界。

## Standards

完整范围初审与修复增量复审均为：**硬规则 0 项、需要修改的 heuristic 0 项**。共享读取、实例外观与两类重绘复用现有边界；发生日期仍由服务端决定，未增加本地重复周期算法。网页上轮 HTTP 404、会话及资料版本保护保持有效。

可选建议：网页对象版重复周期算法没有生产调用，可后续清理。Android ADR 中过时的“尚未实现”说明已在本轮更新。

## Spec

本轮发现并修正 4 组行为问题：

1. **后台失败被无关前台操作清除**。后台记录 FAILED／OFFLINE 后，仍存活的 ViewModel 可能没有相应内存标志，导出结束或重绘会把桌面状态改成普通缓存。重绘现在保留持久状态，前台恢复读取该状态以决定重试，真正成功获取才清除失败。回归验证后台 503 → 前台导出 → 两类仍显示失败且成功时间不变 → 真正同步成功后恢复。
2. **共享后台请求取消误伤前台读取**。系统停止后台任务时，共用请求的活跃前台调用收到后台取消异常而失败。共享层现在区分调用方自身取消与共享请求所属方取消；仍活跃的调用重新读取，自身已取消的调用正常结束。回归使用受控 HTTP 暂停和真实协程取消，验证前台最终取得新快照并更新两类实例。
3. **正常前台生命周期结束误报失败**。ViewModel 生命周期结束会取消读取，但通用异常分支将其标为同步失败。前台刷新现单独传播取消异常；正常取消不写入 FAILED。通过公开 ViewModelStore 生命周期与两类实际 RemoteViews 验证。
4. **近期日程长名和最小尺寸裁切**。原有“日期 · 名称 · 天数”在合法长名下会省略天数；250×160、1.5 倍字体时，重复底部入口还会将三条日程挤到首行不完整。日期和天数移至行首，名称在尾部省略；小空间单行、大空间两行；最小高度隐藏重复“查看全部”，标题仍进入分类首页；标题自适应字号。原生布局断言覆盖 250×160、280×180、320×360，确认完整行、日期、天数、名称前缀可见及条目／标题点击正确。实际预览也已人工查看。

修复后增量 Spec 复审：**已报告代码问题全部解决，剩余 0 项，未发现范围扩张**。实机验收不足单独记录，不包含在上述代码问题计数中。

## 最终验证

- Android：`testDebugUnitTest assembleRelease --offline` 成功（3 分 50 秒）；26 个测试类、**116 项测试**，失败／错误／跳过均为 0。Release Kotlin／Java 编译、lint vital、APK 打包通过；生成 `app/build/outputs/apk/release/app-release-unsigned.apk`，未签名。

- 网页：重新执行 `npm run build` 和完整测试，**170 项通过，失败、取消、跳过均为 0**。
- Worker：Wrangler deploy dry-run 打包成功，没有部署。
- 本轮相关组合测试：22 项通过，涵盖后台同步、前台恢复、两类实例状态及近期列表原生布局；最终完整套件包含这些测试及上轮文件选择回归。
- 原生预览为忽略的构建产物 `app/build/widget-previews/upcoming-long-name-*.png`，检查了最小空间内容。Robolectric API26／34／36 的测试不代表真实 Launcher 行为。
- 本轮没有升级产品依赖。构建沿用 JDK21、现有 SDK、Gradle 缓存。

## 整合与交付边界

- 已将完整组合版本及两轮审查修复快进合回本地 main；独立 worktree 和 `codex/tickets-16-19` 分支保留。整合前 main 为 `ba7fa41`，本轮测试的生产代码为 `b253287`，其后的报告提交只更新文档。两类小组件、上轮文件选择生命周期修复和本轮修复已在同一个版本验证。

- 网页 main 保持 `3f7b1ac`；本轮没有新增网页修改。
- `adb devices -l` 返回空设备列表，故没有执行真实 OS／Launcher 的人工验收，也未取得桌面型号／版本。仍需验证添加、长按重配、多实例缩放、大字体、系统主题、后台受限／断网恢复、重启及调时后的实际延迟，不能承诺零点精确刷新。
- 父规格的完全验收应以这些设备证据为补充；本轮未改动 GitHub issue 状态或父规格正文。
- 没有推送、部署或签名发布。

## 值得后续优化的地方

1. 优先补实际最低／目标 Android 版本和常用 Launcher 的验收记录；这是当前主要证据缺口。
2. 网页遗留 `DateUtils.getNextOccurrence(eventObj)` 等对象版周期推导，可核对调用与测试依赖后清理，减少重复业务规则；保留有调用的字符串日期路径。本轮不扩大为日期算法重写。
3. 历史计划和交接保留原时点状态，入口已指向本报告；后续维护应继续明确“历史记录”和“当前结论”，避免重复判断任务是否实施。

Standards 剩余 0 项；Spec 已发现代码问题剩余 0 项；真实设备／Launcher 验收仍待完成。
