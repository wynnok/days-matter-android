> 最新实施记录：[2026-10-07 #20–#22](2026-10-07-tickets-20-22.md)。

> 后续 #10–#15 已实施，当前进度见 [新交接](2026-10-07-tickets-10-15.md)。以下为 #9 完成时的历史记录。

# Ticket 执行交接：#9 收尾

日期：2026-10-07。此记录接续 [2026-10-06 交接](2026-10-06-ticket-handoff.md)，本次完成当前进行中的 #9；#10–#22 尚未实施。

## 仓库与提交

- Android：`/home/wynn/公共/DevHub/codes/personal/days-matter-android`，当前 `main`。#9 实现、测试与本文件一起提交。
- 网页／Worker：`/home/wynn/公共/DevHub/codes/personal/DaysMatter`，当前 `main`。已恢复 `codex/issue-9-channel-controls` 的 `f502e6f` WIP，修复提交为 `a533b25`，并快进合并到本机 `main`。
- 两仓库本次提交均未推送，未部署服务。父 spec #2、#3 保持原正文与状态。

## #9 完成内容

- 普通用户入口使用“站外提醒”，通过二级说明保留 Webhook 渠道含义；设备本地提醒仍独立。
- Android 渠道列表新增启停开关，提交期间禁用；成功写入后更新并保存渠道快照，后续刷新失败也保留已确认状态。失败或 401 不伪装成功，旧账号迟到响应不影响新账号。
- 两端显示开启站外提醒的主事件引用数量；未取得事件数据时显示“引用数量待获取”。网页事件刷新也更新渠道引用展示。
- 网页开关在实际值改变后提交，失败恢复服务器确认的原状态，重新渲染期间继续防重。
- 两端编辑失败保留输入，区分写入成功与后续同步失败；测试成功统一为“测试请求已完成，请检查接收端”，不采用旧服务端的“已发送”文案。
- 新渠道沿用接口默认启用行为，网页与 Android 都只在编辑时提供启停选择，并说明新建默认值。
- Worker 删除渠道前检查当前账号仍开启提醒的事件引用，停用渠道也受保护；关闭相关事件提醒后才可删除。
- 网页保存、测试和删除反馈隔离旧会话；保存后刷新只释放自身提交状态，不能解除下一次测试的防重状态。

## 验证与审查

- 网页：`npm run build` 成功；`npm test` **135 项通过**。渠道 Worker 测试使用真实隔离 D1 与受控接收端，启停状态通过真实 GET 接口读回，未向真实用户发送消息。
- Android：`testDebugUnitTest` **46 项通过**，其中 `ChannelScreenTest` 9 项覆盖启停、拒绝、401、引用保护、草稿保留、刷新失败、测试防重和账号切换。
- Android：`assembleRelease` 成功，包含 Release 编译与 lint vital 检查；产物为 `app/build/outputs/apk/release/app-release-unsigned.apk`，未签名发布。
- 双轴审查基点：Android `8f343fdd1cc28e9511fa916632de05436a1d7529`；网页 `0bcd9a28512e7728daad66ad3be4e92e6be89602`。审查包含网页 WIP 和本次工作区修改。
- Standards：0 项发现。Spec 首轮发现网页新建开关无效、旧保存刷新解除新测试防重两项；均已修复，竞争场景补了失败再通过的回归测试，复审 0 项发现。

## 本机环境

- SDK 实际路径：`/home/wynn/公共/DevHub/software/android-studio/sdk`；仅修正被忽略的 `local.properties`。
- Android Studio 自带 JDK 为 25，本项目构建要求 21。本次临时使用 `/tmp/days-matter-jdk21`，未全局安装；后续可在该目录仍存在时复用。
- 本机原无 `gh`，临时 CLI 位于 `/tmp/gh_2.65.0_linux_amd64/bin/gh`。使用现有 Git 凭据，不把凭据写入文件或日志。`gh issue view` 使用 `--json` 避开旧 CLI 的 Projects classic 查询错误。
- `.tools/`、`.kotlin/` 和 APK 均是构建／测试产物，不提交。项目依赖继续使用现有 Gradle 缓存。

## 下一张 ticket

按原交接顺序推进 [#10：三种外观模式一致](https://github.com/wynnok/days-matter-android/issues/10)，先读取最新 issue 正文与评论；网页补跟随系统，Android 复用并验收。后续顺序和阻塞关系仍见旧交接，逐票实施、双轴审查、独立提交，保持不推送。
