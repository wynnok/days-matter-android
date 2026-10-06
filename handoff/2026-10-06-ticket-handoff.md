# Ticket 执行交接

最新进度见 [2026-10-07：#9 收尾](2026-10-07-ticket-handoff.md)。以下保留原交接作为历史记录。

日期：2026-10-06
目标：继续按已发布 ticket 执行应用精进，优先完成当前未关闭的 [#9](https://github.com/wynnok/days-matter-android/issues/9)，再按阻塞关系推进后续票。
范围约束：只提交各 ticket 相关文件；不要提交仓库里已有的方案、术语、版本号、构建产物或临时工具改动，除非对应 ticket 明确要求。

## 建议调用技能

1. `implement`：继续按 ticket 实施。
2. `tdd`：在已有测试入口上先补验收，再改行为。
3. `code-review`：每张 ticket 完成后做双轴审查。
4. `diagnosing-bugs`：如果 #9 当前测试失败或后续遇到难定位缺陷。

## 仓库与分支

- Android 仓库：`/Users/milkyway/Public/DevHub/code/personal/days-matter/days-matter-android`
  - 当前分支：`main`，领先 `origin/main` 5 个提交，未推送。
  - 最近已完成票提交：
    - `8dc63ce fix: distinguish data loading failures and expired sessions (#8)`
    - `653645c fix: protect category references and report CRUD outcomes (#7)`
    - `0a19a4 fix: preserve profile drafts and confirmed save results (#6)`
    - `e1bee0d fix: keep account overview independent of categories (#5)`
    - `2c78f95 Fix confirmed occurrence date display (#4)`
  - 工作区已有与本轮 ticket 无关的未提交内容：`CONTEXT.md`、`app/build.gradle.kts`、`docs/adr/0002-cached-occurrence-day-display.md`、`docs/plans/`、`app/release/`。
  - `DaysMatter-CF` 是指向网页／Worker 仓库的软链接；不要把它作为内容提交。
- 网页／Worker 仓库：`/Users/milkyway/Public/DevHub/code/personal/days-matter/DaysMatter-CF`
  - 当前分支：`main`，领先 `origin/main` 5 个提交，未推送。
  - 最近已完成票提交：#5、#6、#7、#8 各有一个对应提交。
  - #9 网页／Worker 草稿已提交并推送到 `codex/issue-9-channel-controls`，提交 `f502e6f`；`main` 不包含该 WIP。
  - 工作区仍有与本轮无关的 `.gitignore`、`CONTEXT.md`、`.tools/` 改动。

## 规则来源

- Ticket 索引：[docs/plans/2026-10-06-continuous-improvement.md](../docs/plans/2026-10-06-continuous-improvement.md)
- 共通规格：[docs/plans/2026-10-06-shared-improvement.md](../docs/plans/2026-10-06-shared-improvement.md)
- Android 专属规格：[docs/plans/2026-10-06-android-improvement.md](../docs/plans/2026-10-06-android-improvement.md)
- 可执行 spec：GitHub Issue #2、#3；执行票：GitHub Issue #4–#22。
- 领域术语：[CONTEXT.md](../CONTEXT.md)
- 日期边界：[docs/adr/0002-cached-occurrence-day-display.md](../docs/adr/0002-cached-occurrence-day-display.md)
- Issue 流程：[docs/agents/issue-tracker.md](../docs/agents/issue-tracker.md)

用户已确认的执行方式：以固定 HEAD 为审查基点，逐票审查；实现提交到各自仓库当前 `main`，不推送。父 spec 保持正文和状态不变。

## 当前进度

- #4–#8 已实现、测试、审查、提交并关闭。
- #9 两端站外提醒渠道启停与测试一致：进行中，未提交、未审查、未关闭。
- #10 及之后未开始。

#4–#8 的验证基线：

- 网页／Worker 仓库最后完整测试为 #8 时 125 项通过。
- Android 仓库最后完整测试为 #8 时 37 项通过，APK 构建通过。
- #9 修改后尚未重新跑完整套件，不能沿用上述结果作为当前通过状态。

## #9 当前状态

规格要点来自 C05／CV08：两端可配置、启停和测试渠道；列表可看事件引用关系；渠道启用状态决定账号提醒是否发送；测试请求成功只表示请求已完成或消息已交给接收端，不等于实际送达；不要把本地提醒合入渠道总开关。

### 网页／Worker 已做

源码位置：`public/index.html`；构建产物：`src/index.ts`。

- 渠道编辑表单新增“启用渠道”开关，编辑时把 `is_active` 提交给 `PUT /remind-channels/:id`。
- 渠道列表显示“已启用／已停用”、事件引用数量，并新增行内启停开关。
- 行内启停成功后更新本地渠道状态并重新渲染；失败时回滚复选框并显示错误。
- 删除渠道的失败路径现在会显示错误提示。
- 测试渠道成功文案改为“测试请求已完成，请检查接收端”；表单说明补充测试成功不等于实际送达。
- `npm run build` 已成功重新生成 `src/index.ts`。
- `tests/channel-form.test.js` 已扩展：草稿测试防重、编辑保存提交启停、启停成功/失败状态、引用数量展示、无效地址拦截。
- `tests/channel-worker.test.js` 追加了启停更新和账号隔离测试。

### 网页／Worker 当前验证问题

先切到 `DaysMatter-CF` 仓库的 `codex/issue-9-channel-controls` 分支继续，不要在 `main` 上重新实现。

- `node --test tests/channel-form.test.js`：6 项中 5 项通过；“启用开关成功后更新列表并渲染”失败。最后失败点是成功路径没有把测试里的复选框 `checked` 置为 true；需要核对 `toggleChannel` 成功分支与测试上下文。
- `node --test tests/channel-worker.test.js`：旧有 2 项 Worker 测试通过；新追加的“真实 Worker 更新渠道启停并按账号隔离”失败。最后观察是第二个账号请求仍返回 200。这是测试替身问题：`first()` 虽按当前用户返回渠道存在性，但 `run()` 无条件为 `UPDATE remind_channels` 构造成功响应，没有检查 SQL `WHERE` 绑定中的 `user_id`。不要据此误判产品路由缺少账号隔离；真实 SQL 已包含 `WHERE channel_id = ? AND user_id = ?`。
- 建议下一步简化这个 Worker 测试：要么只对账号 1 的成功更新断言 SQL 和绑定（`is_active` 在索引 3，渠道 ID 在索引 6，账号 ID 在索引 7），要么让替身在账号不匹配时不返回成功响应；不要继续通过真实路由响应携带内部 SQL 的方式做断言。
- 之后运行 `npm test`，并让 `code-review` 以本会话开始前两仓库 HEAD 为基点审查 #9。

### Android 待做

相关源码：

- `app/src/main/java/top/zwtx/daysmatter/ui/AdminScreens.kt`
- `app/src/main/java/top/zwtx/daysmatter/MainViewModel.kt`
- `app/src/test/java/top/zwtx/daysmatter/ui/`

现状：Android 编辑页已有 `active` 开关，编辑 payload 会带 `is_active`；列表已显示启停状态，但没有行内启停，也没有和网页一致的引用数量展示。测试渠道仍显示“测试消息已发送”，需要改为“测试请求已完成，请检查接收端”或等价明确语义。

待补验收：

1. 渠道列表行内启停：请求中禁用；成功更新本地快照；失败或 401 时不伪装成功，并处理账号隔离。
2. 列表显示每个账号渠道被多少个主事件引用；事件数据未获取时显示明确的待获取状态。
3. 测试请求成功文案与网页一致；失败不暗示实际送达。
4. 编辑保存失败时保留表单输入；写入成功但刷新失败时区分两个结果。
5. 旧账号迟到响应不得影响新账号。
6. 建议新增 `ChannelScreenTest`，复用现有 Robolectric + Compose + 本地 `HttpServer` 模式。
7. 运行 Android 单测和 `assembleRelease` 或项目现有构建任务，再双轴审查并提交。

## 后续 ticket 顺序

按已发布阻塞关系推进：

1. #9：两端站外提醒渠道启停与测试一致。
2. #10：三种外观模式一致；网页需要补系统模式，Android 复用并验收。
3. #11：共通帮助、版本与声明入口。
4. #12：账号备份导出与真实文件反馈。
5. #13：追加导入预览、确认与完整刷新。
6. #14：导入未知结果与刷新失败处理，依赖 #13。
7. #15：Android 离线阅读与前台联网恢复，依赖 #4、#8。
8. #16：通知点击定位事件与账号回退。
9. #17：本地提醒概览、权限与测试通知。
10. #18：我的资料摘要与管理分组，依赖 #5。
11. #19–#22：小组件链路；#19 依赖 #15、#16，#20 依赖 #19，#21、#22 依赖 #20。

Android A06 文件适配随 #12–#14 一并完成，不重复开票。

## 测试命令

网页／Worker 仓库：

```sh
npm run build
npm test
node --test tests/channel-form.test.js tests/channel-worker.test.js
```

Android 仓库：

```sh
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

如命令因环境失败，先核对 JDK、Android SDK、依赖缓存和当前目录；不要全局安装工具。

## 提交与审查约定

- #9 的网页／Worker WIP 在 `codex/issue-9-channel-controls`；完成并通过审查后可合并回该仓库 `main` 或按项目流程开 PR。
- 每张 ticket 独立提交；提交信息引用对应 issue 编号。
- 当前两仓库都在 `main`，继续按用户已授权方式提交到 `main`，不要推送。
- 不要提交 `.tools/`、`app/release/`、`DaysMatter-CF` 软链接、以及与 ticket 无关的既有未提交文件。
- 每票完成后用 `code-review`，基点为本会话开始时两仓库各自 HEAD；审查通过后关闭对应 issue。
- `gh` 有时无法访问 GitHub API；遇到时先继续本地可验证工作，稍后重试 issue 状态更新。

## 环境注意

- 当前机器上 Android 仓库可写；网页／Worker 仓库需要通过已授权的提升写入执行修改。
- 另一台电脑只需要把交接文档中的两个实际仓库路径替换为本机 checkout；不要依赖 `days-matter-android/DaysMatter-CF` 软链接作为持久结构。
- 敏感信息：无。测试中的 token 都是测试用途的合成值，不需要替换为真实凭据。
