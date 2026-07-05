# 跑修 · 环境搭建

## 前置要求

- JDK 17+
- Android Studio Ladybug+ / SDK 35
- Android 设备或模拟器（API 26+）

## 百度地图 Key

1. 生成 debug 签名指纹：

```bash
cd android && ./gradlew :app:signingReport
```

2. 登录 [百度地图开放平台](https://lbsyun.baidu.com/apindex.htm) → 控制台 → 创建应用 → 添加 Android Key  
   - 包名：`com.wendao.run`（调试包还需填 `com.wendao.run.debug`）  
   - 发布版 SHA1：Debug 证书的 SHA1（开发期先用 debug）

3. 写入 `android/local.properties`：

```properties
BAIDU_MAP_API_KEY=你的Key
```

> 百度 SDK 要求用户同意隐私政策后再调用 `SDKInitializer.initialize()`，已在 `PaoxiuApplication` 中处理。

## 微信登录（可后补）

无企业主体时，首期使用 **游客登录**；微信 AppID 就绪后填入 `local.properties` 的 `WECHAT_APP_ID`。

## 构建与测试

```bash
cd android
./gradlew assembleDebug          # 调试包
./gradlew testDebugUnitTest      # 单元测试
./gradlew assembleRelease        # Release（需自行签名）
```

## 辅助脚本

```bash
./scripts/debug-signing-report.sh   # 查看签名 SHA1
./scripts/get-debug-sha1.sh         # 快速取 debug SHA1
./scripts/demo-emulator.sh          # 启动模拟器联调
```
