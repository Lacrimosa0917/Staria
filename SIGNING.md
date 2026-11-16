# 签名与发布说明

> 这份文档是给**仓库维护者**看的，不属于开源内容。
> 它本身不含任何密钥，可以入库；实际的 `.jks` 与 `keystore.properties` 已被 `.gitignore` 排除。

## 签名配置位置

| 文件 | 是否入库 | 说明 |
|---|---|---|
| `keystore/staria-release.jks` | ❌ 排除 | 签名密钥库（PKCS12，别名 `staria`，RSA 4096，有效期至 2056 年） |
| `keystore.properties` | ❌ 排除 | 密钥库路径与密码，由 `app/build.gradle.kts` 读取 |
| `keystore/README-签名说明.md` | ❌ 排除 | 本目录下的详细备份指引 |

`app/build.gradle.kts` 的读取逻辑：

```kotlin
val keystorePropsFile = rootProject.file("keystore.properties")
val hasReleaseSigning = keystoreProps.getProperty("storeFile") != null
```

- 文件存在 → release 用正式签名
- 文件缺失 → 不打 release 包（避免误用 debug 签名发布）

这样纯 clone 仓库的人依然能 `assembleDebug`，不必持有私钥。

## 证书指纹

```
SHA1:   0A:E4:76:1A:BE:29:7E:4D:8A:3B:37:A8:52:DA:89:16:6D:CC:4A:C6
SHA256: A7:DF:80:47:7F:01:53:A2:32:0B:5F:E8:72:88:DD:4D:30:89:E2:D5:2F:EC:26:D5:C4:01:75:15:81:98:4E:6C
```

## ⚠️ 密钥库必须立即备份

**`.jks` 丢失 = 应用永远无法更新。**

Android 用签名证书判定「同一个应用」。换签名会导致覆盖安装被系统拒绝
（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`），老用户必须卸载重装——而卸载会清空
全部日记、摘录、随笔数据。Google Play 同样不接受换签名的更新。

备份要求：**至少两份，且不在同一块磁盘**。

1. `keystore/staria-release.jks` + `keystore.properties` 一起复制
2. 存进密码管理器（Bitwarden / 1Password / KeePass）的附件
3. 或存离线介质（U 盘 / 移动硬盘）
4. 绝不提交到 Git，也不要发到聊天软件

## 发布新版本流程

1. 提升 `app/build.gradle.kts` 的 `versionCode`（每次 +1，**不可回退**）
2. 按需改 `versionName`（如 `1.0.1`）
3. `./gradlew assembleRelease`
4. 产物：`app/build/outputs/apk/release/app-release.apk`
5. 在 GitHub 创建 tag + Release，上传该 APK

忘记提升 `versionCode` 会导致用户无法安装新包（提示版本更低）。

## 已验证的升级安全性

以下结论来自实机测试（Android 17 模拟器，API 37）：

| 场景 | 结果 |
|---|---|
| 同签名 `versionCode 1 → 2` 覆盖安装 | ✅ 成功，`firstInstallTime` 不变 |
| 升级后原有数据 | ✅ 日记 / 纸条 / 随笔 / 昵称 / 设置全部保留 |
| 升级后新建内容 | ✅ 正常写入 |
| debug 包与 release 包共存 | ✅ 包名不同（`.debug` 后缀），互不影响，数据各自独立 |

数据存储位置：`/data/data/<包名>/shared_prefs/staria_library.xml`
（另有 `staria_profile.xml`、`staria_settings.xml`），
`shared_prefs` 不随 APK 替换而清除。
