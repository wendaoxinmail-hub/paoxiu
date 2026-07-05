# 跑修 · 架构说明

## 客户端（Android）

- **Kotlin** + **Jetpack Compose** + **Hilt**
- **Room**：离线跑步记录、同步队列
- **Retrofit**：REST API
- **百度地图 SDK**：定位与地图（GCJ-02）
- **BLE / Health Connect**：心率；无设备时使用「气机指数」

## 服务端（Spring Boot）

- **JWT** 鉴权（游客 / 微信 openid）
- **MySQL**：业务数据
- **Redis**：Session、排行榜
- **OSS**（可选）：轨迹文件

## 同步策略

1. 跑步中：轨迹写 Room
2. 结束：本地预览 → 有网上传 `/api/v1/run/finish`
3. 服务端反作弊 → 写正式记录 → 返回境界/灵石/奇遇结果
4. 冲突：**服务端为准**

## 模块边界

| 模块 | Android 包 | Server 包 |
|------|-------------|-----------|
| 认证 | `feature.auth` | `auth` |
| 跑步 | `feature.run` | `run` |
| 境界 | `feature.realm` | `realm` |
| 宗门 | `feature.sect` | `sect` |
| 钱包 | `feature.wallet` | `wallet` |
