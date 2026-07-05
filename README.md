# 跑修

凡人修仙意境的跑步游戏化 Android 应用。

## 仓库结构

```
wendao-run/
├── android/          # Kotlin + Jetpack Compose 客户端
├── docs/             # 产品与架构说明
└── scripts/          # 本地开发辅助脚本
```

## 快速开始

### 1. 申请百度地图 Key

```bash
cd android
./gradlew :app:signingReport
```

将 **SHA1** 用于 [百度地图开放平台](https://lbsyun.baidu.com/) 申请 Android Key。  
包名：`com.wendao.run`（debug 包为 `com.wendao.run.debug`）

### 2. 配置客户端

```bash
cp android/local.properties.example android/local.properties
```

填入 `BAIDU_MAP_API_KEY`（微信 AppID 有则一并填入）。  
如需联调后端，可设置 `API_BASE_URL`（默认模拟器 `http://10.0.2.2:8080`）。

### 3. 运行 Android

用 Android Studio 打开 `android/` 目录，连接设备或模拟器运行 `app`。

```bash
cd android
./gradlew assembleDebug
```

## 文档

- [架构说明](docs/ARCHITECTURE.md)
- [环境搭建](docs/SETUP.md)
- [产品模块](docs/PRODUCT_MODULES.md)

## 首期范围

- 户外跑步：轨迹、距离、配速、修炼结算
- 全模块（境界、功法、奇遇、装备、洞府、灵兽、宗门、师徒等）
- **不含**：道侣 Tab、真实支付
