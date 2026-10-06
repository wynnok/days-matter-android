# 应用持续精进方案索引

日期：2026-10-06。原综合方案已按用户确认拆分为以下两份；本文保留导航和依赖，不再重复维护功能规格。两份可执行 spec 已发布，功能尚未实施。

| 方案 | 维护内容 | 后续任务如何使用 |
| --- | --- | --- |
| [网页端与 Android 共通精进](2026-10-06-shared-improvement.md) | 基础能力、日期与摘要规则、资料／分类／站外提醒、账号备份、外观、共通帮助、两端缺口及跨端验收 | 网页、Android 与 Worker 相关任务共同引用；共通需求唯一来源 |
| [Android 专属精进与平台适配](2026-10-06-android-improvement.md) | 设备提醒与权限、离线缓存、系统刷新、事件路由、桌面小组件、“我的”及文件交互的平台适配 | Android 任务在共通方案基础上读取；只补充平台能力与验收 |

两份方案现阶段均维护在本仓库 `docs/plans`，不向网页仓库复制另一份可独立修改的共通规格。向另一仓库交接任务时提供共通文档及对应版本或提交引用。

## 可执行 spec

- [Issue #2：两端共通方案](https://github.com/wynnok/days-matter-android/issues/2)：C01–C08 与 CV01–CV10，共通业务规则的实施依据。
- [Issue #3：Android 专属方案](https://github.com/wynnok/days-matter-android/issues/3)：A01–A06、W01／W02 与 AV01–AV14，引用共通规则并补充平台要求。

两条均标记 `ready-for-agent`；后续实施和完成验收以 spec 为准，方案文档保留规划依据，不另复制一套可执行需求。

## 实施 ticket

19 条 ticket 已按确认的拆分发布，全部标记 `ready-for-agent`，10 条原生阻塞关系已核对。以下仅为导航；验收以各 ticket 正文为准，最新阻塞状态以 GitHub 原生依赖为准。父 spec 正文、标题、标签和状态保持不变。

两端共通方案（11 条）：

| Ticket | 阻塞票 |
| --- | --- |
| [#4：两端日期展示与跨日天数一致](https://github.com/wynnok/days-matter-android/issues/4) | 无 |
| [#5：两端账号摘要与近期数量一致](https://github.com/wynnok/days-matter-android/issues/5) | [#4](https://github.com/wynnok/days-matter-android/issues/4) |
| [#6：两端资料保存反馈与输入保留](https://github.com/wynnok/days-matter-android/issues/6) | 无 |
| [#7：两端事件与分类管理结果一致](https://github.com/wynnok/days-matter-android/issues/7) | 无 |
| [#8：两端加载、失败与登录失效状态明确](https://github.com/wynnok/days-matter-android/issues/8) | 无 |
| [#9：两端站外提醒渠道启停与测试一致](https://github.com/wynnok/days-matter-android/issues/9) | 无 |
| [#10：两端三种外观模式一致](https://github.com/wynnok/days-matter-android/issues/10) | 无 |
| [#11：两端共通帮助、版本与声明入口](https://github.com/wynnok/days-matter-android/issues/11) | 无 |
| [#12：两端账号备份导出与真实文件反馈](https://github.com/wynnok/days-matter-android/issues/12) | 无 |
| [#13：两端追加导入预览、确认与完整刷新](https://github.com/wynnok/days-matter-android/issues/13) | 无 |
| [#14：两端导入未知结果与刷新失败处理](https://github.com/wynnok/days-matter-android/issues/14) | [#13](https://github.com/wynnok/days-matter-android/issues/13) |

Android 专属方案（8 条）：

| Ticket | 阻塞票 |
| --- | --- |
| [#15：离线阅读与前台联网恢复](https://github.com/wynnok/days-matter-android/issues/15) | [#4](https://github.com/wynnok/days-matter-android/issues/4)、[#8](https://github.com/wynnok/days-matter-android/issues/8) |
| [#16：通知点击定位事件与账号回退](https://github.com/wynnok/days-matter-android/issues/16) | 无 |
| [#17：本地提醒概览、权限与测试通知](https://github.com/wynnok/days-matter-android/issues/17) | 无 |
| [#18：我的资料摘要与管理分组](https://github.com/wynnok/days-matter-android/issues/18) | [#5](https://github.com/wynnok/days-matter-android/issues/5) |
| [#19：重要日子小组件完整首版](https://github.com/wynnok/days-matter-android/issues/19) | [#15](https://github.com/wynnok/days-matter-android/issues/15)、[#16](https://github.com/wynnok/days-matter-android/issues/16) |
| [#20：近期日程小组件完整首版](https://github.com/wynnok/days-matter-android/issues/20) | [#19](https://github.com/wynnok/days-matter-android/issues/19) |
| [#21：两类实例管理与独立外观](https://github.com/wynnok/days-matter-android/issues/21) | [#20](https://github.com/wynnok/days-matter-android/issues/20) |
| [#22：小组件后台跨日与重启恢复](https://github.com/wynnok/days-matter-android/issues/22) | [#20](https://github.com/wynnok/days-matter-android/issues/20) |

Android A06 文件适配随共通备份 ticket 完成，不另建重复 ticket。发布时有 11 条无阻塞的可执行票；日期票 #4 是摘要和离线阅读的共同前置，可优先开始。

## 一致性与执行原则

基础能力、业务规则、数据含义和操作结果保持一致；页面、入口和设备偏好可按平台不同。允许分批落地，但须记录未对齐项，不能用单端完成代替共通完成。

先明确共通 C01–C03 的日期、状态及备份契约，再逐端补缺口。Android 先建立日期／路由基础和管理入口，再完成 W01 重要日子，最后完成 W02 近期日程。已确认的个人使用定位、轻量摘要、两类小组件与日期边界保留，不重新扩展范围。

## 原编号的归属

| 原方案条目 | 新归属 |
| --- | --- |
| M01 同步 | 共通 C02 状态语义；Android A01 缓存与重试 |
| M02 提醒 | 共通 C05 站外提醒；Android A02 本地提醒与权限 |
| M03 备份 | 共通 C03；Android A06 文件适配 |
| M04 资料、M05 摘要、M06 分类 | 共通 C04／C06；Android A04 入口适配 |
| M07 小组件 | Android A05／W01／W02 |
| M08 外观与帮助 | 共通 C07／C08；Android A04 设备帮助 |
| M09 头像与偏好 | 共通 S06-C 头像候选；Android 设备偏好候选 |
| S01–S07 整体完善 | 共通后续路线；本地提醒、设备迁移及小组件样式拆入 Android 后续路线 |
| V01–V16 验收 | 按共通 CV01–CV10 与 Android AV01–AV14 重新组织 |

领域术语维护在 [CONTEXT.md](../../CONTEXT.md)。日期展示边界见 [ADR-0002](../adr/0002-cached-occurrence-day-display.md)，账号数据及设备提醒边界继续遵循 [ADR-0001](../adr/0001-native-client-and-reminders.md)。
