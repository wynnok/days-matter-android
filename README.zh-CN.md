# Days Matter Android

[English](README.md) | [简体中文](README.zh-CN.md)

使用 Kotlin + Jetpack Compose 开发的原生倒数日应用，后端为 [Days Matter Cloudflare Workers 项目](https://github.com/wynnok/DaysMatter)。网页端和 Android 端连接同一个 Worker 时，共用账号和 D1 数据。

## 功能

- 登录和注册，支持已有账号。密码框请求英文密码键盘，仅接受英文字母、数字和半角符号（ASCII 33–126，不含空格）。
- 倒数日列表和详情、下拉刷新、分类抽屉筛选、网格/列表切换、置顶、重复规则和子事件。
- 公历和农历日期、分类配色和 83 个分类图标标识。
- Webhook 提醒渠道、个人资料、默认头像、JSON 导入导出，以及浅色/深色/跟随系统外观。
- 手机本地提醒和离线缓存阅读。修改数据需要联网，恢复网络或下拉刷新后同步。

## 架构

```text
Android 应用 ── HTTPS /api/ ── Cloudflare Worker ── D1
网页端       ── /api/ ────────┘        │
                                Cron → Webhook
Android 设备 → 本地通知
```

Worker 负责账号、分类、事件、Webhook 渠道、农历及重复事件的下次日期计算和服务端备份。Android 使用 Android Keystore 加密保存会话，并保留私有离线快照。本地提醒偏好和外观设置保存在设备上。

## Cloudflare 后端配置

### 1. 获取后端代码

本仓库是 Android 客户端。请从[后端仓库](https://github.com/wynnok/DaysMatter)获取 Worker 源码，下文需要其中的 `package.json`、`wrangler.toml`、`schema.sql` 和 `build.js`。本地如果存在 `DaysMatter-CF/`，它只是参考副本，不包含在本仓库的受版本控制文件中。

在**后端项目根目录**运行以下命令，Node.js 版本需满足其 Wrangler 依赖要求：

```bash
npm install --cache .tools/npm-cache
npx wrangler login
```

Wrangler 安装在后端的 `node_modules` 中，无需全局安装。登录命令会打开浏览器进行 Cloudflare 授权。

### 2. 绑定 D1 数据库

如果网页端已有正常运行的 Worker 和 D1，直接复用现有部署，跳过创建和初始化数据库。按照下方构建说明，把 Android 指向现有后端的 `/api/` 地址即可。

全新部署时创建数据库：

```bash
npx wrangler d1 create days-matter-db
```

将返回的数据库 ID 写入后端的 `wrangler.toml`。保留后端的兼容性配置，关键配置如下：

```toml
name = "days-matter"
main = "src/index.ts"
compatibility_date = "2024-01-01"
compatibility_flags = ["nodejs_compat"]

[[d1_databases]]
binding = "DB"
database_name = "days-matter-db"
database_id = "YOUR_DATABASE_ID"

[triggers]
crons = ["*/5 * * * *"]

[vars]
ENVIRONMENT = "production"
```

绑定名必须为 `DB`，与 Worker 中的 `env.DB` 一致。Worker 名称和数据库 ID 请使用你自己的值。目前认证实现没有读取 `JWT_SECRET`，类型中的可选声明不代表必须配置该密钥。Webhook 地址和渠道凭据通过应用内的提醒渠道配置。

### 3. 初始化并部署

针对**新建的远程数据库**执行：

```bash
npx wrangler d1 execute days-matter-db --remote --file=./schema.sql
npm run deploy
```

数据库包含 `users`、`categories`、`remind_channels`、`events`、`sub_events` 五张表。`npm run deploy` 会先构建内嵌网页和农历资源，再部署生成的 `src/index.ts`。已有数据库如果缺少字段，需要执行对应迁移；重复运行 `CREATE TABLE IF NOT EXISTS` 不会给已有表补充字段。

部署后打开 Worker 地址即可使用网页端。访问 `https://YOUR_WORKER.YOUR_SUBDOMAIN.workers.dev/api/health`，应返回 `status: "running"`。再注册/登录并创建事件，验证数据库绑定；健康检查本身不会查询 D1。也可以使用自定义 HTTPS 域名替代 `workers.dev` 地址。

当前 cron 每五分钟扫描一次提醒，Worker 的 `scheduled` 处理器已启用，业务日期和提醒时间按 Asia/Shanghai 计算。启用 Webhook 渠道并在事件中选用后，即可接收服务端提醒。手机本地通知独立于这个定时任务运行。

### 4. 后端本地开发

在后端项目根目录执行：

```bash
npx wrangler d1 execute days-matter-db --local --file=./schema.sql
npm run build
npm run dev
```

本地 D1 和远程 D1 相互独立，本地写入不会同步到生产库。修改网页或生成资源后需要重新构建。Android 模拟器使用 `http://10.0.2.2:8787/api/` 访问开发服务器；真机需要可访问的局域网地址，并让 Wrangler 监听对应的网络接口。

数据库命令和本地/远程参数可参考 Cloudflare 的 [D1 入门文档](https://developers.cloudflare.com/d1/get-started/)和 [D1 命令参考](https://developers.cloudflare.com/d1/wrangler-commands/)。

## 构建 Android 应用

### 环境准备

用 Android Studio 打开本仓库并同步 Gradle。项目使用 AGP 9.4、Gradle 9.6、Compose BOM 2026.09.00，`compileSdk` 为 37、`targetSdk` 为 36、`minSdk` 为 26（Android 8.0），Gradle 守护进程使用 JDK 21，编译目标为 JDK 17 字节码。

在 Android Studio 或未纳入版本控制的 `local.properties` 中设置 SDK 路径。需要项目内隔离时，可使用 `.tools/android-sdk`，并将 **Gradle user home** 设置为 `.tools/gradle-home`，两者均填写绝对路径。Gradle user home 用于存储缓存，无需添加到 `PATH`。机器专属配置不要提交到 Git。

使用 SDK Manager 安装 Android SDK Platform 37、Build Tools 36.0.0 和 Platform Tools。如果项目内已安装 Android 命令行工具，可在 Android 仓库根目录执行：

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" ./.tools/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/.tools/android-sdk" --licenses
ANDROID_USER_HOME="$PWD/.tools/android-user-home" ./.tools/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/.tools/android-sdk" --install 'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools'
```

### 配置 API 地址并构建

默认 API 地址为 `https://dm.zwtx.top/api/`。使用自己的后端时，传入包含 `/api/` 的地址：

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" GRADLE_USER_HOME="$PWD/.tools/gradle-home" ./gradlew :app:assembleDebug -PapiBaseUrl=https://YOUR_WORKER.YOUR_SUBDOMAIN.workers.dev/api/
```

模拟器连接本地 Wrangler：

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" GRADLE_USER_HOME="$PWD/.tools/gradle-home" ./gradlew :app:assembleDebug -PapiBaseUrl=http://10.0.2.2:8787/api/
```

APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。API 地址编译在 APK 内，修改后需要重新构建。Debug 允许本地开发使用 HTTP，Release 必须使用 HTTPS。

通过 Android Studio 构建时，可以在已配置的 Gradle user home 下的 `gradle.properties`（例如 `.tools/gradle-home/gradle.properties`）写入 `apiBaseUrl=https://YOUR_WORKER.YOUR_SUBDOMAIN.workers.dev/api/`，然后同步。不要将它写入 `local.properties`，这里该文件只用于 SDK 路径。

### 可直接安装的正式 APK

在 Android Studio 中选择 **Build → Generate Signed App Bundle / APK → APK**，选择 `app` 模块，选择或创建签名密钥库，然后构建 `release`。向导会显示输出目录；完成后点击 **Locate** 定位已签名 APK。仅创建密钥库不会构建应用。本项目没有自动配置 Release 签名，单独执行 `assembleRelease` 会生成未签名 APK。

请妥善保管并备份密钥库和密码，后续更新需要相同签名。不要提交密钥库或生成的 APK。每次发布更新时，增加 `app/build.gradle.kts` 中的 `versionCode`。

### 验证

```bash
ANDROID_USER_HOME="$PWD/.tools/android-user-home" GRADLE_USER_HOME="$PWD/.tools/gradle-home" ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

密码过滤测试覆盖合法 ASCII、中文/全角/表情过滤、空白字符和混合粘贴输入。实际键盘布局由已安装的输入法决定：应用请求英文密码输入并关闭自动纠错，字符过滤同样适用于粘贴。网页端已有密码若包含不支持的字符，将无法在 Android 客户端输入。

## 提醒行为与故障排查

手机本地提醒使用 Worker 返回的下一次日期，按 Asia/Shanghai 时区安排通知。启用时会申请通知权限，重启后恢复待发送提醒。系统省电策略可能延迟通知；长期离线时不会无限计算后续重复提醒。本地提醒偏好不包含在服务端备份中。

| 现象 | 排查方向 |
| --- | --- |
| 登录或同步失败 | API 地址是否包含 `/api/`、Worker 是否可访问、账号是否属于该后端。 |
| 健康检查正常，但事件接口失败 | D1 绑定名是否为 `DB`、数据库表和所需字段是否完整。 |
| 正式包无法连接服务器 | 使用 HTTPS；Release 禁止明文 HTTP。 |
| 模拟器无法连接 localhost | 使用 `10.0.2.2` 访问宿主机，不能使用 `localhost`。 |
| Webhook 提醒未送达 | 检查 cron、`scheduled` 处理器、渠道启用状态、事件提醒时间，以及公网 HTTPS Webhook 地址；在后端目录运行 `npx wrangler tail` 查看日志。 |
| 手机本地通知未送达 | 检查通知权限、本地提醒开关、省电限制及最近一次同步是否成功。 |

## 设计规范

使用蓝白底色、蓝色主操作、珊瑚色点缀和蓝绿渐变摘要建立层次。页面边距 20 dp，卡片内边距 18 dp，条目间距 12 dp，分区间距 20 dp；表单控件高 56 dp，操作按钮高 48 dp。共享尺寸位于 `ui/Theme.kt`。玻璃质感用于悬浮导航和操作栏，内容区域优先保证阅读清晰。

列表突出事件名称和天数；详情先展示倒计时和下一次日期，再分组展示原始日期、重复规则和提醒信息。

## 项目记录

- [领域术语](CONTEXT.md)
- [后端边界与本地提醒归属](docs/adr/0001-native-client-and-reminders.md)
- [第三方声明](THIRD_PARTY_NOTICES.md)
