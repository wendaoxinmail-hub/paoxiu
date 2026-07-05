# 跑修 MVP 模块实现说明

> 对齐产品方案 v2.0（功法 §5、洞府 §9、修炼流程 §10）。本文描述**当前代码已实现**的行为与后续扩展点。

## 修炼（Tab · 首页）

- **入口**：`CultivateScreen` — Keep 式大卡 +「开始修炼」
- **流程**：选功法 → 户外跑步 → 收功结算 → 熟练度 +1（`RunRewardService`）
- **当前功法卡片**：展示名称、修炼方式摘要，点击进入 **修炼法门详情**
- **数据**：`UserProfileDto.activeTechniqueId`、`techniqueProficiency`

## 功法 · 藏经阁

### 分层

| 类型 | 说明 | 领悟方式 |
|------|------|----------|
| **基础功法** (`tier=basic`) | 基础步法、金刚诀·热身篇、长春功、洗髓经 | 洞府等级满足后 **免费领悟** |
| **高阶功法** (`tier=advanced`) | 御风诀 | 消耗灵石兑换 |

### 配置源

`server/.../game/GameCatalog.java` — 每条功法包含：

- `practiceMethod` 修炼方式
- `practiceCondition` 修炼条件
- `practiceFlow` 修炼流程
- `masteryEffect` 熟练度效果说明
- `unlockRealm` / `requiredGrottoLevel` 解锁门槛

### API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/techniques` | 列表（含 owned / canComprehend） |
| GET | `/api/v1/techniques/{id}` | 法门详情 |
| POST | `/api/v1/techniques/{id}/comprehend` | 领悟（基础免费） |
| POST | `/api/v1/techniques/{id}/equip` | 装备为当前功法 |

### Android

- **洞府 · 藏经阁**：`GrottoScreen` — 卡片 +「查看法门」「领悟」
- **法门详情**：`TechniqueDetailScreen` — 完整修炼方式/条件/流程/熟练度

### 熟练度

- 每次有效跑步 +1，上限 100（`RunRewardService.incrementTechniqueProficiency`）
- 档位加成与条件校验：**MVP 仅展示文案**，后续在结算时按配速/灵根 enforcement

## 洞府

- **灵田**：4 小时冷却，收获 `80 × 洞府等级` 灵石
- **闭关**：即时获得修为 `50 × 等级`
- **升级**：`500 × 当前等级` 灵石
- **灵兽**：奇遇获得后喂养升级
- **藏经阁**：练气期（洞府 Lv.1）开放基础功法领悟

配置与逻辑：`GrottoService.java` · `GrottoScreen.kt`

## 装备库

- 4 件入门装备，灵石购买 + 穿戴
- 收益乘数叠加在 `RunRewardService`

## 宗门

- 创建/加入、师徒、每日宗门任务（贡献里程领奖）
- `SectScreen` · `/api/v1/sects/*`

## 奇遇

- 跑步结算 18% 触发，`GameCatalog.ADVENTURES`
- 「残破功法」文案引导至藏经阁领悟基础功法

## 后续迭代（产品方案已有、代码待深化）

1. 按功法条件校验配速/时长/心率（长春功、御风诀等）
2. 熟练度 30/60/100 档实际加成与轨迹特效
3. 灵根对功法效率加减（长春功 ±50%）
4. 功法组合（元婴+）
