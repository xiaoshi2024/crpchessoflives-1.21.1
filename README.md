# CRP: Chess Of Lives — 尸兄剧情模组

## 项目概述

本模组为《尸兄》（CRP: Chess Of Lives）剧情 Minecraft 模组，基于 NeoForge 21.1.235 开发，适用于 Minecraft 1.21.1。

模组核心内容围绕**尸兄剧情阵营「黑色火线」的克隆科技体系**展开，实现了完整的玩家克隆、意识转移、克隆体管理等玩法机制。

---

## 开源致谢：基于 Player-Shells 二次开发

本模组的克隆科技系统**内置修改并集成了开源模组 [Player-Shells](https://github.com/Ultramega/Player-Shells) 的核心代码**（原路径：`d:\Player-Shells-master\Player-Shells-master`），在此基础上根据尸兄剧情世界观进行了深度定制。

### 原始模组信息
- **原作者**：Ultramega
- **原项目**：Player-Shells — 玩家外壳/替身系统
- **原始许可**：遵循原项目开源协议

### 主要修改与定制内容
1. **命名空间迁移**：所有注册名、资源路径从 `playershells` 迁移至 `crpchessoflives`，作为黑色火线阵营的专属科技
2. **代码结构重组**：克隆相关类统一归入 `com.xiaoshi2022.crpchessoflives.clone` 子包
3. **伪装身份系统**：实现了「真·意识转移」——进入他人克隆体时显示原主的皮肤和名称（而非使用者身份），通过反射修改 GameProfile 并广播 `ClientboundPlayerInfoUpdatePacket` 实现
4. **剧情向功能裁剪**：移除了径向菜单快捷切换、克隆体自然衰变等非核心机制，保留最纯粹的黑色火线克隆流程
5. **资源文件重映射**：所有 blockstates、models、lang、textures 资源均适配 `crpchessoflives` 命名空间

---

## 克隆科技系统（黑色火线阵营）

### 核心方块

| 方块 | 用途 |
|------|------|
| **克隆舱 (Shell Forge)** | 黑色火线核心设备，多结构方块。负责注入基因样本、培育克隆体、玩家意识转移进出克隆体 |
| **离心机 (Centrifuge)** | 从血液样本中离心分离提取 DNA 基因样本 |

### 核心物品

| 物品 | 用途 |
|------|------|
| **空注射器** | 用于采集玩家血液样本 |
| **血液注射器** | 装有玩家血液的注射器，需放入离心机提纯 |
| **DNA 样本** | 离心机提纯产物，用于在克隆舱中培育对应玩家的克隆体 |

### 克隆流程

1. **采血**：使用空注射器右键点击目标玩家，获取血液注射器
2. **离心提纯**：将血液注射器放入离心机并供能，产出 DNA 样本
3. **培育克隆体**：在克隆舱中放入 DNA 样本，启动培育流程（CREATE → CREATING → ACTIVE 状态机）
4. **意识转移**：玩家站在激活的克隆舱上，选择已培育的克隆体进入（玩家外观变为克隆体原主身份）
5. **退出 / 切换**：可随时通过克隆舱退出当前克隆体，或切换至其他可用克隆体
6. **死亡复活**：在克隆体内死亡时，意识自动回传至本体或最近可用克隆体
7. **销毁克隆体**：通过克隆舱的销毁功能（ACTIVE → DISPOSING）处理废弃克隆体

### 技术架构

- **状态机驱动**：`ShellForgeBlockEntity` 内建两套独立状态机（`ShellStates` 克隆体生命周期 / `PlayerStates` 玩家进出舱流程）
- **跨端网络同步**：
  - C2S 包：`ValidateShellForgePacket`（结构校验）、`ShellButtonPressedPacket`（界面操作）、`TransferPlayerPacket`（意识转移）、`LeaveShellForgePacket`（出舱）
  - S2C 包：`SyncShellDataPacket`（克隆数据同步）、`AfterDeathPacket`（死亡回传动画）、`FinishedSyncPacket`（同步完成回调）
- **数据持久化**：`ShellSavedData`（服务器端全局克隆存档）/ `ClientShellData`（客户端缓存）
- **Mixin 注入**：
  - `MixinServerPlayer`：死亡时意识转移、GameProfile 反射实现皮肤/名称伪装
  - `MixinCamera`：意识转移时的相机过渡动画
  - `MixinEntityRenderDispatcher`：克隆体渲染调度
- **自定义渲染**：`ShellForgeBlockEntityRenderer` 配合 `create_shader` 着色器实现克隆舱培育时的能量视觉效果

---

## 技术信息

### 环境与依赖

| 项目 | 版本 |
|------|------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.235 |
| Java | 21 |
| Gradle | 8.x (MDK 内置) |
| Parchment 映射 | 2024.11.17 |

### 代码结构

```
src/main/java/com/xiaoshi2022/crpchessoflives/
├── CRPChessOfLives.java          # 主入口类，注册所有内容
├── CRPChessOfLivesClient.java    # 客户端初始化
├── Config.java                   # 模组配置（能量容量、冷却等）
└── clone/                        # 黑色火线克隆科技子系统（基于 Player-Shells 修改）
    ├── blocks/                   # 克隆舱、离心机方块
    ├── blockentities/            # 方块实体 + 状态机逻辑 + 渲染
    ├── items/                    # 注射器、DNA、带所有者物品基类
    ├── entities/                 # 克隆体实体 + 渲染器
    ├── container/                # 克隆舱/离心机菜单 + 物品槽处理
    ├── gui/                      # 屏幕渲染、选择界面、进度条控件
    ├── registry/                 # 所有注册器（方块/物品/BE/菜单/音效...）
    ├── mixin/                    # 服务器玩家/相机/渲染分发器 Mixin
    ├── packet/                   # C2S / S2C 网络包定义与处理
    ├── storage/                  # 克隆数据服务器存档 + 客户端缓存
    ├── events/                   # 通用/客户端事件总线处理器
    ├── shaders/                  # 克隆舱自定义着色器
    └── utils/                    # 工具类（相机动画、音效、ShellBundle 等）
```

---

## 构建与运行

### IDE 开发环境

1. 克隆本仓库后，使用 IntelliJ IDEA 或 Eclipse 打开项目根目录
2. 等待 Gradle 自动同步完成（若出现依赖缺失可执行）：
   ```bash
   gradlew --refresh-dependencies
   ```
3. 通过 IDE 内置的 `runClient` / `runServer` 任务启动游戏

### 命令行构建

```bash
# 清理旧产物
gradlew clean

# 构建 mod jar（输出到 build/libs/）
gradlew build

# 仅编译 Java 代码
gradlew compileJava
```

### 常见问题

- **依赖下载失败（SSL 握手错误）**：检查网络环境，必要时为 Gradle 配置代理，或多次重试 `gradlew --refresh-dependencies`
- **映射名解析失败**：确认 `gradle.properties` 中 Minecraft 版本与 NeoForge 版本匹配（当前为 1.21.1 + 21.1.235）
- **Mixin 不生效**：确认 `src/main/resources/crpchessoflives.mixins.json` 中 Mixin 类路径与实际类一致

---

## 许可说明

- 本模组整体代码版权归属于 CRP: Chess Of Lives 项目（All Rights Reserved）
- 其中源自 **Player-Shells** 的代码部分，**严格遵循原项目的开源许可协议** 使用与分发
- 如需单独复用 `clone/` 子包下的代码，请同时遵守原 Player-Shells 项目的许可条款
