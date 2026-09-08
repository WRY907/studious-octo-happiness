# 华为手机全息数据驾驶舱 · Code Wiki

> 仓库：[WRY907/studious-octo-happiness](https://github.com/WRY907/studious-octo-happiness)
> 版本基线：`main` 分支，HEAD `21d72ca`（背景音乐系统 v3）
> 文档生成日期：2026-09-08
> 文档定位：面向开发者的全息代码参考——架构、模块职责、关键类与函数、依赖关系、运行方式，并附项目开发背景解读

---

## 0. 文档说明与证据来源

本文档由代码仓库与一段 TraeWork 开发会话记录共同提炼而成。为遵循"判断与证据纪律"，下文对信息来源做明确标注：

| 标记 | 含义 |
|------|------|
| ✅ 已核验 | 直接读取了仓库源码（GitHub raw / API tree）或仓库内 README、设计文档原文 |
| 📋 推断 | 未读到该文件原文，依据调用方代码、README 描述、设计文档、开发会话上下文推断；如需精确细节请以源码为准 |

文件读取覆盖范围（✅ 已核验）：
- 后端：`pom.xml`、`application.yml`、`application-local.example.yml`、`HoloCockpitApplication`、`common/Result`、`config/JwtUtil`、5 个 Controller（Auth/Cockpit/Admin/Import/Ai）、7 个 Service（Auth/Cockpit/ArkClient/Ai/Alert/Import/Report）、`entity/SysUser`
- 前端：`package.json`、`vite.config.js`、`src/main.js`、`src/router/index.js`、`src/api/index.js`、`src/views/Cockpit.vue`
- 辅助：`crawler/vmall_crawler.py`、`docs/superpowers/specs/2026-09-03-huawei-cockpit-design.md`、`README.md`、GitHub 完整文件树
- 背景：TraeContent 分享会话原文（全量抓取）

未读到原文、采用 📋 推断的关键文件：`config/JwtFilter`、`config/CorsConfig`、`config/MybatisPlusConfig`、`config/WebConfig`、`service/ImportResult`、其余 entity、所有 mapper、前端 `audio/manager.js` 与各 chart 组件、`sql/init.sql`、`tools/generate_sql.js`、`start.bat`/`start.sh`。

---

## 1. 项目概览

### 1.1 定位 ✅

面向**华为终端手机业务**（以陕西省为主要业务区域）的全栈数据可视化平台，由三大部分组成：

1. **全息数据大屏** —— 深色科技风大屏，实时展示销售额、订单、用户、流量等核心指标，配套粒子星空、矩阵雨、全息扫描线等全套动效与 Web Audio 程序化音频
2. **管理后台** —— 基于 JWT 的两层权限体系（管理员 ADMIN / 商家 MERCHANT），支持手机型号、订单、用户等业务的增删改查，以及 8 类业务表的 Excel 批量导入
3. **AI 智能分析** —— 接入火山方舟（豆包大模型），提供 AI 对话助手、自然语言查数（NL2SQL）、智能预警、一键分析报告四大能力

演示数据：内置 30 天陕西省为主的仿真业务数据（西安领跑、真实华为机型定价、含周末效应），开箱即用。

### 1.2 技术栈清单 ✅

| 层 | 技术 | 版本 |
|----|------|------|
| 前端框架 | Vue 3（Composition API）+ Vue Router | Vue 3.4 / Router 4.2.5 |
| 构建工具 | Vite | 5.x |
| 可视化 | ECharts + echarts-gl + Three.js | 5.4.3 / 2.0.9 / 0.185 |
| 网络 | Axios（自动携带 JWT） | 1.6.2 |
| 后端框架 | Spring Boot | 2.7.18（Java 8+） |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 认证 | JWT（jjwt 0.9.1）+ Servlet Filter | — |
| Excel | Alibaba EasyExcel | 3.3.4 |
| AI | 火山方舟 OpenAI 兼容接口（豆包 doubao-seed 系列） | model `doubao-seed-2-0-lite-260215` |
| 数据库 | MySQL | 5.6+（驱动 mysql-connector-j，兼容 5.6~8.x） |
| 工具 | Hutool 5.8.22、Lombok | — |

---

## 2. 项目背景解读（TraeWork 开发会话）✅

本节源自一段 TraeContent 分享会话（`share/DDE3P-J7P22NT1`），记录了项目于 **2026-09-04 至 2026-09-07** 通过 TraeWork 完成的主要开发历程。它解释了仓库当前形态的由来，是理解代码"为什么这样写"的关键背景。

### 2.1 开发方法论

- 开发工具：**TraeWork**（AI Agent 全栈开发环境），会话中组合使用多类插件：web-data-visualization、frontend-design、web-app-development、superpowers、github、computer-use 等
- 开发模式：自然语言下达任务 → Agent 自主读取文件 / 执行命令 / 操作浏览器 / 调用技能 → 提交审核
- 典型一次任务耗时 8~22 分钟（项目迁移 8m38s、GitHub 发布 18m49s、销售趋势页 22m17s）

### 2.2 关键里程碑

| 时间 | 节点 | 成果 |
|------|------|------|
| 09-04 10:05 | Excel 数据导入 | 8 类业务表批量导入接口、模板下载、逐行校验 |
| 09-04 13:09 | 5 项修改需求 | 流量来源改柱状图、订单轮播降速、AI 可用性、登录后访问大屏、动态交互与背景音乐 |
| 09-04 13:25 | 开通 doubao-seed-2-0-lite | AI 能力正式接入 |
| 09-04 14:56 | 项目定位澄清 | 确认"华为手机全息数据驾驶舱" |
| 09-04 14:58~15:11 | 项目介绍书 | 生成 Word 版介绍书 + 小组分工 |
| 09-04 15:46 | 背景音乐 | 从开源站点爬取科技感 BGM（CC0） |
| 09-07 09:45 | 服务迁移 | 项目从 TraeWork 工作区复制到桌面独立目录并重启服务 |
| 09-07 11:07 | GitHub 发布 | 仓库 `WRY907/studious-octo-happiness` 公开、103 文件初始提交、敏感信息脱敏 |
| 09-07 11:26 | 移除介绍书 | 因含组员真实姓名，从仓库移除项目介绍书 docx |
| 09-07 11:51 | 销售趋势页 | 新增 `/trend` 独立页 + 角色差异化接口，提交 `4fc2b54` 推送 |

### 2.3 关键技术决策与踩坑（影响代码现状）

- **敏感信息脱敏方案**：`application.yml` 中数据库密码改为 `${DB_PASSWORD:123456}`、火山方舟 Key 改为 `${ARK_API_KEY:}` 占位；真实值存放于 `application-local.yml`（已被 `.gitignore` 忽略）；并提供 `application-local.example.yml` 模板。爬虫 `vmall_crawler.py` 的密码也改为环境变量读取。这是仓库能安全公开的前提。
- **ECharts "Initialize failed: invalid dom"**：`TrendAnalysis.vue` 的 `onMounted` 里过早调用 `echarts.init()`，此时图表容器在 `v-else-if="ready"` 分支内、DOM 尚未渲染 → init 抛错并中断后续 `loadData()`，导致页面空白。修复方式：**将图表初始化延迟到数据就绪、DOM 渲染之后**。
- **加载状态时序 Bug**：`loadData` 中 `loading.value = false` 在 `renderAll()` 之后才执行，导致 `v-if="loading"` 分支仍占 DOM、`nextTick` 后图表容器为 null、`renderAll` 被跳过。修复方式：**调整状态时序，先置 ready/loading 再渲染**。这两个时序 Bug 是理解 `TrendAnalysis.vue`（及同类分析页）挂载逻辑的关键。
- **Vite HMR 未感知路由变更**：修改 `router/index.js` 后 Vite 未热更新路由模块，需重启前端服务。这是开发期常见现象，不影响生产构建。
- **GitHub 推送网络问题**：国内访问 `github.com` 解析到的 IP（20.205.243.166）线路间歇性中断，会话中通过"连接备用 IP + Host 头伪装"完成推送。这是发布期工程问题，与代码逻辑无关。
- **角色差异化设计**：商家（MERCHANT）的金额类字段在**后端 Controller 层**即过滤（如 `/admin/trend` 管理员返回 5 字段含 sales/avgOrderValue，商家仅 3 字段），而非仅前端隐藏——这是安全设计要点。

> ⚠️ 一处需注意的不一致：会话概览曾提及博物馆相关实体（Activity/Exhibit/Guide/Reservation/Ticket/Venue、MuseumController、DataGlobe 等），但仓库实际代码为**华为手机**业务实体（PhoneModel/RealtimeOrder/HotProduct 等、CockpitController/AdminController）。会话本身在 09-04 14:56 已澄清定位为"华为手机全息数据驾驶舱"，故本 Wiki 以**仓库实际代码**为准；博物馆实体属于会话上下文中的另一变体，不在本仓库范围内。

---

## 3. 整体架构 ✅

### 3.1 架构图

```
┌─────────────────────────────────────────────────────────┐
│                    前端 holo-cockpit-frontend             │
│          Vue 3 + Vite 5 + Vue Router + Axios             │
│  ┌──────────────────┐      ┌──────────────────────────┐  │
│  │   全息数据大屏     │      │        管理后台           │  │
│  │  ECharts 5 图表   │      │  登录 / CRUD / 数据导入    │  │
│  │  Canvas 动效粒子  │      │  两层权限路由守卫          │  │
│  │  Web Audio 音频   │      │  Excel 上传 / 模板下载     │  │
│  └──────────────────┘      └──────────────────────────┘  │
│              Vite DevServer :3000  ──/api 代理──►         │
└──────────────────────────────┬──────────────────────────┘
                               │ HTTP / JSON (JWT Bearer)
┌──────────────────────────────▼──────────────────────────┐
│                  后端 holo-cockpit-backend                │
│              Spring Boot 2.7.18 (Java 8+)                │
│  ┌─────────┐ ┌──────────────┐ ┌─────────────────────┐   │
│  │JWT 认证  │ │ MyBatis-Plus │ │   EasyExcel 3.3     │   │
│  │角色权限   │ │  CRUD / 分页  │ │  Excel 解析 / 生成   │   │
│  └─────────┘ └──────────────┘ └─────────────────────┘   │
│  ┌─────────────────────┐  ┌──────────────────────────┐   │
│  │  Cockpit 大屏数据    │  │  ArkClient 火山方舟       │   │
│  │  Admin 后台管理       │  │  (OpenAI 兼容 / 流式)     │   │
│  └─────────────────────┘  └──────────────────────────┘   │
└──────────────────────────────┬──────────────────────────┘
                               │ JDBC
                    ┌──────────▼──────────┐
                    │  MySQL 5.6+         │
                    │  库：huawei_cockpit  │
                    │  12 张业务表         │
                    └─────────────────────┘
```

### 3.2 分层职责

| 层 | 职责 | 关键约束 |
|----|------|---------|
| 前端视图层 | 大屏渲染、后台 CRUD、登录注册 | 路由守卫做粗粒度鉴权；金额脱敏以"后端过滤为主、前端展示兜底" |
| 前端网络层 | Axios 封装、JWT 自动注入、401 跳登录 | blob 响应（模板下载）直接透传不解包 |
| 后端 Filter | JWT 解析、角色注入 | 仅拦截 `/admin/*`（📋 推断）；大屏 `/cockpit/*` 与 AI `/ai/*` 在 Controller 内自行判角色 |
| 后端 Controller | 路由、参数校验、角色差异化脱敏 | 不写复杂业务，薄层 |
| 后端 Service | 业务逻辑、AI 编排、Excel 解析、规则引擎 | NL2SQL 强制白名单与 SELECT 校验 |
| 数据层 | MyBatis-Plus + JdbcTemplate | 多数 CRUD 走 MP；MAX(stat_date) 等走 JdbcTemplate |

### 3.3 典型请求链路

1. **大屏加载**：前端 `cockpitApi.getAll()` → `GET /api/cockpit/all` → `CockpitController.getAll` → 若 MERCHANT 则 `desensitize()` 脱敏 → `CockpitService.getAll()` 一次性聚合 8 组数据返回
2. **登录**：`POST /api/auth/login` → `AuthService.login` MD5 校验 + 状态拦截 → `JwtUtil.createToken` 签发 24h 令牌 → 前端存 `hw_token`/`hw_role`
3. **AI 对话**：`fetch('/api/ai/chat')` → `AiController.chat` → `AiService.chatStream` → `buildContext()` 注入大屏实时数据 → `ArkClient.chatStream` SSE 转发 → 前端打字机渲染
4. **NL2SQL**：`POST /api/ai/query` → `AiService.query` → Ark 生成 SQL → `validateSql`（SELECT + 白名单 + 禁关键字）→ `executeQuery`（限 50 行/10s）→ Ark 二次解读
5. **Excel 导入**：`POST /api/admin/import/{table}` → `JwtFilter` 校验 ADMIN → `ImportController.importExcel` → `ImportService.importExcel` 表头映射 + 逐行校验 + 落库

---

## 4. 项目结构总览 ✅

```
.
├── start.bat                    # Windows 一键启动（自动安装依赖）
├── start.sh                     # macOS / Linux 一键启动
├── README.md                    # 项目说明
├── .gitignore
│
├── holo-cockpit-backend/        # 后端服务（Spring Boot，端口 8080）
│   ├── mvnw / mvnw.cmd          # Maven Wrapper（无需安装 Maven）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/holocockpit/
│       │   ├── HoloCockpitApplication.java   # 启动类
│       │   ├── common/Result.java            # 统一响应
│       │   ├── config/                       # CORS / MybatisPlus / JWT 过滤器与工具
│       │   ├── controller/                   # Auth / Cockpit / Admin / Import / Ai
│       │   ├── entity/                       # 12 个实体
│       │   ├── mapper/                       # MyBatis-Plus Mapper（10+ 接口）
│       │   └── service/                      # 业务逻辑 + ArkClient + ImportResult
│       └── resources/
│           ├── application.yml               # 主配置（密码/Key 已脱敏）
│           └── application-local.example.yml # 本地私密配置模板
│
├── holo-cockpit-frontend/       # 前端服务（Vite，端口 3000）
│   ├── package.json
│   ├── vite.config.js           # 端口 3000 + /api 代理到 8080
│   └── src/
│       ├── App.vue / main.js
│       ├── api/index.js         # Axios 封装 + 全部接口
│       ├── audio/manager.js     # Web Audio 程序化音频管理
│       ├── router/index.js      # 路由 + 登录守卫
│       ├── components/          # 大屏图表组件（9 个）
│       ├── styles/              # global.css / admin.css
│       └── views/
│           ├── Cockpit.vue      # 全息数据大屏
│           ├── Login.vue / Register.vue
│           ├── TrendAnalysis.vue / HotModels.vue / TrafficAnalysis.vue / UserProfileAnalysis.vue  # 角色差异化分析页
│           └── admin/           # 后台页面（AdminLayout + 6 子页）
│
├── sql/init.sql                 # 建库建表 + 30 天仿真数据
├── crawler/vmall_crawler.py     # 华为商城机型价格爬虫（可选）
├── tools/generate_sql.js        # 仿真数据生成器（可选）
├── holo-music/                  # 背景音乐素材（index.html + 图片）
└── docs/
    ├── images/                  # 界面截图
    └── superpowers/specs/2026-09-03-huawei-cockpit-design.md  # 设计文档
```

---

## 5. 后端模块详解（holo-cockpit-backend）

包根：`com.holocockpit`

### 5.1 启动类与配置 ✅

#### `HoloCockpitApplication` ✅
```java
@SpringBootApplication
@MapperScan("com.holocockpit.mapper")
@EnableScheduling
public class HoloCockpitApplication
```
- `@MapperScan` 扫描 mapper 包
- `@EnableScheduling` 开启定时（📋 预警检测可能用到，未见显式 @Scheduled，推断为手动触发）
- 启动后输出接口地址 `http://localhost:8080/api`

#### `application.yml` ✅（关键配置摘录）
```yaml
server:
  port: 8080
  servlet:
    context-path: /api          # 所有接口以 /api 为前缀
spring:
  config:
    import: optional:file:./application-local.yml   # 可选加载本地私密配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/huawei_cockpit?...&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}      # 环境变量注入，默认 123456
  servlet:
    multipart:
      max-file-size: 20MB                 # Excel 导入上限
      max-request-size: 20MB
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted         # 逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0
ark:                          # 火山方舟 OpenAI 兼容接口
  api-key: ${ARK_API_KEY:}    # 不配置时 AI 功能降级
  base-url: https://ark.cn-beijing.volces.com/api/v3
  model: doubao-seed-2-0-lite-260215
```
- context-path `/api` 决定所有接口 URL 前缀
- `allowPublicKeyRetrieval=true` 已默认开启，兼容 MySQL 8.x 认证
- `logic-delete-field: deleted` 开启全局逻辑删除（📋 推断相关表含 deleted 字段）

#### `application-local.example.yml` ✅
本地私密配置模板：`spring.datasource.username/password`、`ark.api-key`。复制为 `application-local.yml`（gitignored）后填真实值。

### 5.2 common 包 ✅

#### `Result<T>` ✅ — 统一响应包装
```java
@Data
public class Result<T> implements Serializable {
    private Integer code;     // 200=成功
    private String message;
    private T data;
    // 工厂方法
    static <T> Result<T> success()
    static <T> Result<T> success(T data)
    static <T> Result<T> success(String message, T data)
    static <T> Result<T> error(String message)            // code=500
    static <T> Result<T> error(Integer code, String message)
}
```
所有 Controller 返回 `Result<T>`，前端 Axios 拦截器据 `code===200` 解包 `data`。

### 5.3 config 包

#### `JwtUtil` ✅ — JWT 工具（HS256）
```java
public final class JwtUtil {
    private static final String SECRET = "huawei-cockpit-secret-2026";
    private static final long EXPIRE_MS = 24 * 60 * 60 * 1000L;   // 24 小时
    public static String createToken(Long userId, String username, String role)
    public static Claims parseToken(String token)   // 无效/过期抛异常
}
```
- claims：`userId` / `username` / `role`
- ⚠️ 安全提示：密钥硬编码于源码，且仓库公开。生产应改为环境变量/非对称签名。本项目为课程设计用途。

#### `JwtFilter` 📋 — JWT 过滤器
未读到原文。依据 `ImportController` 中 `request.getAttribute("role")` 用法、`CockpitController.isMerchant()` 自行解析 Bearer、以及开发会话"JwtFilter 只拦 /admin/*"的描述，推断：
- 注册路径：`/admin/*`（仅管理后台接口受其强制保护）
- 逻辑：从 `Authorization: Bearer <token>` 解析 → `JwtUtil.parseToken` → 将 `role`/`userId` 设为 request 属性
- `/cockpit/*` 与 `/ai/*` 不经此 Filter，角色判断在 Controller 内调用 `JwtUtil.parseToken` 完成（见 `CockpitController.isMerchant`）

#### `CorsConfig` / `MybatisPlusConfig` / `WebConfig` 📋
未读到原文。依据命名与常规实践推断：
- `CorsConfig`：放开跨域（允许前端 :3000 访问 :8080）
- `MybatisPlusConfig`：注册分页插件 `PaginationInnerInterceptor`（`AdminController` 用到 `Page<>`）
- `WebConfig`：注册 `JwtFilter` 到 `/admin/*`（与上述推断一致）

### 5.4 controller 包 ✅

所有 Controller 路径前缀为 `/api`（context-path）。

#### `AuthController` ✅ — `/auth/*` 认证
| 方法 | 路径 | 入参 | 返回 | 说明 |
|------|------|------|------|------|
| POST | `/auth/login` | `{username, password}` | `{token, role, username, merchantName}` | 登录签发 JWT |
| POST | `/auth/register` | `{username, password, merchantName}` | `Void` | 商家注册，进入 PENDING 待审 |

异常处理：`IllegalArgumentException`→400，其余→500。

#### `CockpitController` ✅ — `/cockpit/*` 大屏数据（角色感知）
| 方法 | 路径 | 返回 | 商家脱敏 |
|------|------|------|---------|
| GET | `/cockpit/all` | `Map` 含 8 组数据 | overview 今日/累计销售额置空；hotProducts 销售额置空；realtimeOrders 金额置空；`masked=true` |
| GET | `/cockpit/region?date=` | `List<RegionSales>` | sales 置空 |
| GET | `/cockpit/city?date=` | `List<CitySales>` | sales 置空 |

- `isMerchant(request)`：解析可选 Bearer 令牌判断是否 MERCHANT（无令牌/解析失败视为非商家）
- `desensitize(data)`：金额类字段置 null + `masked=true`，前端据此进入脱敏展示

#### `AdminController` ✅ — `/admin/*` 管理后台（JWT 保护，角色差异化）
核心方法（均注入对应 Mapper 与 `CockpitService`）：

**机型管理（增删改仅 ADMIN）**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/phones?page=&size=&keyword=` | 分页列表，keyword 匹配机型名/系列 |
| POST | `/admin/phones` | 新增（ADMIN） |
| PUT | `/admin/phones` | 修改（ADMIN） |
| DELETE | `/admin/phones/{id}` | 删除（ADMIN） |

**订单管理**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/orders?page=&size=` | 分页倒序；MERCHANT 金额脱敏（amount=null, masked=true） |
| PUT | `/admin/orders/{id}/status` | 改状态 1-4（ADMIN/MERCHANT 均可） |
| DELETE | `/admin/orders/{id}` | 删除（ADMIN） |

**用户与统计**
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/users?page=&size=` | 用户列表（仅 ADMIN，密码置 null） |
| GET | `/admin/stats` | 统计（仅 ADMIN，📋） |

**四类角色差异化分析接口（关键，见 2.3）**
| 方法 | 路径 | 公开字段 | ADMIN 附加 |
|------|------|---------|-----------|
| GET | `/admin/trend` | statDate/orders/visits | sales/avgOrderValue |
| GET | `/admin/hotmodels` | rankNo/modelName/salesCount/growth/series/rating/imageUrl/price | salesAmount |
| GET | `/admin/traffic` | sources/categorySummary/hourlyPulse（24h 加权推导） | funnel（5 阶段转化）+ channelMetrics（转化率/客单价/获客成本/GMV/ROI） |
| GET | `/admin/profile` | genders/ages 等 | 消费能力分层 + 年龄价值矩阵（金额类，📋） |

> `/admin/traffic` 的 24h 流量脉搏由 `hourlyShape(h)` 加权函数按总访问量推导，转化漏斗与渠道价值系数为硬编码经验值（搜索引擎 2.8%/5899/12 等）。

**商户审批（📋 依据前端 API 推断）**
前端 `adminApi` 含 `getAuditList/getAuditStats/approveAudit/approveAllAudit/rejectAudit(id,reason)/disableMerchant/enableMerchant`，对应后端应有 `/admin/audit/*` 接口（未读到 Controller 原文，可能位于 AdminController 后半段）。

#### `ImportController` ✅ — `/admin/import/*` Excel 导入（仅 ADMIN）
| 方法 | 路径 | 入参 | 返回 |
|------|------|------|------|
| POST | `/admin/import/{table}` | multipart `file` | `ImportResult{total,success,fail,errors[]}` |
| GET | `/admin/import/{table}/template` | — | xlsx 文件流（Content-Disposition 附件下载） |
| GET | `/admin/import/meta` | — | `List<{table,name,columns:[{label,field,required}]}>` |

- `isAdmin(request)`：取 `request.getAttribute("role")` 判 ADMIN（依赖 JwtFilter 注入）
- 模板下载为 blob，前端 `responseType:'blob'` 透传

#### `AiController` ✅ — `/ai/*` AI 智能接口
| 方法 | 路径 | 入参 | 返回 |
|------|------|------|------|
| POST | `/ai/chat` | `{message, history[]}` | `SseEmitter`（TEXT_EVENT_STREAM） |
| POST | `/ai/query` | `{question}` | `{sql, columns, rows, reply}` |
| GET | `/ai/alerts` | — | `List<AlertRecord>`（最近 20） |
| POST | `/ai/alerts/check` | — | `{count}` 新增预警数 |
| POST | `/ai/report` | — | `{html}` HTML 报告 |

- `chat` 产 SSE：data 为 JSON 字符串文本片段，`[DONE]` 表结束
- 内部静态类 `ChatRequest`/`QueryRequest` 作请求体

### 5.5 service 包 ✅

#### `AuthService` ✅ — 登录与注册
```java
Map<String, Object> login(String username, String password)
void register(String username, String password, String merchantName)
```
- 登录：MD5（Hutool `SecureUtil.md5`）小写 hex 比对；状态拦截 PENDING/REJECTED/DISABLED 抛 `IllegalArgumentException`；成功签发 JWT
- 注册：用户名 3-32 字符（中文/英文/数字/_/-），密码 6-64，商户名 ≤50；同名账号仅 REJECTED 状态允许重新提交（覆盖资料回 PENDING）
- 角色固定为 `MERCHANT`，初始状态 `PENDING`

#### `CockpitService` ✅ — 大屏数据聚合
```java
Map<String, Object> getAll()           // 聚合 8 组：overview/salesTrend/regionSales/citySales/hotProducts/trafficSources/userProfiles/realtimeOrders
OverviewStats getOverview()            // 最新一条
List<SalesTrend> getSalesTrend30()     // 最近 30 天升序
List<RegionSales> getRegionSales(date) // 默认 MAX(stat_date)
List<CitySales> getCitySales(date)     // 默认最新日期 TOP10
List<HotProduct> getHotProducts()     // rank_no 升序
List<TrafficSource> getTrafficSources()// visits 倒序
List<UserProfile> getUserProfiles()
List<RealtimeOrder> getRealtimeOrders()// 最近 20 条
LocalDate maxDate(String table)        // JdbcTemplate 查 MAX(stat_date)
```
- 用 `JdbcTemplate.maxDate` 兜底"今日"=最新统计日期，避免无当日数据时空屏
- `getAll` 返回 `LinkedHashMap` 保证字段顺序稳定

#### `ArkClient` ✅ — 火山方舟客户端（OpenAI 兼容）
```java
String chat(List<Map<String,String>> messages)                                   // 非流式，返回首条 content
void chatStream(List<Map<String,String>> messages, SseEmitter emitter)            // 流式，逐行解析 SSE 转发 delta.content
static String toJsonString(String s)                                              // 转带引号 JSON 字符串（换行转义，防破坏 SSE 报文）
```
- 配置来自 `application.yml`：`ark.api-key`/`base-url`/`model`
- `RestTemplate` 连接超时 10s，读超时 60s
- 流式：读 `data:` 行，`[DONE]` 终止，`extractDeltaContent` 取 `choices[0].delta.content`

#### `AiService` ✅ — AI 对话与 NL2SQL（核心）
```java
String buildContext()                       // 构建系统提示词：今日总览 + TOP3 省份/城市/机型
SseEmitter chatStream(String message, history)  // 流式对话，Ark 失败降级 localReply
Map<String,Object> query(String question)   // NL2SQL：生成→校验→执行→解读
String localReply(String message)           // Ark 不可用时的本地数据概况模板
void validateSql(String sql)                // SQL 安全校验（包级可见）
```

**NL2SQL 安全模型（重点）**：
- 表白名单 `ALLOWED_TABLES`：8 张业务表（phone_model/region_sales/city_sales/realtime_order/hot_product/traffic_source/user_profile/sales_trend）
- 禁止关键字正则（词边界，大小写不敏感）：`insert|update|delete|drop|alter|truncate|create|grant|into`
- `validateSql` 规则：必须 SELECT 开头、禁多语句(`;`)、禁注释(`--`/`/*`/`*/`/`#`)、表白名单
- `executeQuery`：`PreparedStatement.setMaxRows(50)` + `setQueryTimeout(10)`，最多 50 行
- `cleanSql`：去除 markdown 代码块与结尾分号
- 三步流程：① Ark 生成 SQL → ② 校验+执行 → ③ 结果回传 Ark 生成中文解读

**对话上下文**：`buildContext()` 注入实时大屏数据（累计/今日销售额、订单、用户、转化率、环比、TOP3 省份/城市/机型），使 AI 回答有数据依据；历史对话最多携带 12 条。

#### `AlertService` ✅ — 智能预警规则引擎
```java
int checkAlerts()                 // 返回新生成预警数
List<AlertRecord> getAlerts()     // 最近 20 条
```
- 阈值：**下跌 >25% → SALES_DROP**；**上涨 >40% → SALES_SURGE**
- 维度：省份（DROP level2/SURGE level1）、城市（均 level1）
- 对比：最新统计日 vs 前一统计日（`maxDate`/`prevDate`）
- 去重：当天相同 title 跳过
- AI 建议：`generateAdvice` 调 Ark 生成 80 字内应对建议，失败降级模板

#### `ImportService` ✅ — Excel 批量导入（EasyExcel）
```java
List<Map<String,Object>> getMeta()        // 8 表元信息
byte[] getTemplate(String table)          // 内存生成模板（表头+2行示例）
String templateFilename(String table)     // {table}_template.xlsx
ImportResult importExcel(String table, MultipartFile file)   // 核心
```
内部结构：
- `TableMeta` / `ColumnMeta`（label/field/required）静态注册 8 张表
- `parseRow`：按表 switch 逐行转实体，校验必填/数字/日期/枚举（如订单状态 1-4、profile 类型 gender/age）
- `readExcel`：EasyExcel 简单模式 + 中文表头映射（`Map<Integer,String>`）
- `saveAll`：逐条插入；**phone 机型名重复则更新价格，trend 日期重复则更新**
- 全部失败不插入；错误行记录行号+原因返回 `ImportResult{total,success,fail,errors:[{row,msg}]}`

8 张表元信息：phone(机型信息)/region(省份销售)/city(城市销售)/order(实时订单)/hot(热销机型)/traffic(流量来源)/profile(用户画像)/trend(销售趋势)。

#### `ReportService` ✅ — AI 数据报告
```java
String generate()    // 返回完整 HTML 报告字符串
```
- 汇总：30 天累计销售额/订单/访问 + 今日 KPI + TOP3 省份/城市/机型 + 30 天趋势表
- `generateSummary`：调 Ark 生成 150 字内经营总结 + 3 条建议，失败降级模板
- 输出：深色科技风 HTML（内联 CSS，含 KPI 卡片、榜单、趋势表、AI 分析区），前端直接渲染/下载

#### `ImportResult` 📋 — 导入结果 DTO
依据用法：字段 `total/success/fail`，内部类 `RowError{row, message}`。

### 5.6 entity 包 ✅（部分核验）

| 实体 | 表 | 关键字段 | 核验状态 |
|------|----|---------|---------|
| `SysUser` | sys_user | id/username/password(MD5)/role/merchantName/status/rejectReason/auditTime/createTime | ✅ 全文 |
| `PhoneModel` | phone_model | modelName/series/price/rating/imageUrl | 📋（依 ImportService/TABLE_DDL） |
| `RegionSales` | region_sales | province/sales/orders/users/statDate | 📋 |
| `CitySales` | city_sales | city/sales/orders/users/statDate | 📋 |
| `RealtimeOrder` | realtime_order | orderNo/userName/modelName/amount/province/city/status(1-4)/createTime + masked | 📋（含 `masked` 布尔字段，用于脱敏标记） |
| `HotProduct` | hot_product | rankNo/modelName/salesCount/salesAmount/growth | 📋 |
| `TrafficSource` | traffic_source | category/sourceName/visits/ratio | 📋 |
| `UserProfile` | user_profile | profileType(gender/age)/profileName/userCount/ratio | 📋 |
| `SalesTrend` | sales_trend | statDate/sales/orders/visits/avgOrderValue | 📋 |
| `OverviewStats` | overview_stats | todaySales/totalSales/todayOrders/totalOrders/todayNewUsers/todayVisits/conversionRate/salesGrowth/ordersGrowth/updateTime | 📋 |
| `AlertRecord` | alert_record | alertType/title/content/level/aiAdvice/createTime | 📋 |
| `AiChatLog` | ai_chat_log | 📋（对话日志，未见直接引用） |

### 5.7 mapper 包 📋
全部为 MyBatis-Plus `BaseMapper<T>` 接口扩展（如 `PhoneModelMapper extends BaseMapper<PhoneModel>`），无自定义 SQL 方法（CRUD/分页/条件构造均由 MP 提供）。`@MapperScan` 在启动类扫描。

---

## 6. 前端模块详解（holo-cockpit-frontend）

### 6.1 入口与构建 ✅

#### `vite.config.js` ✅
```js
export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@': './src' } },
  server: { port: 3000, host: '0.0.0.0',
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } } },
  build: { outDir: 'dist', minify: 'terser',
    rollupOptions: { output: { manualChunks: { echarts, vue, axios } } } }
})
```
- `/api` 代理到后端 8080，前端用相对路径 `/api/...` 调用，避免跨域
- 构建分包：echarts/vue/axios 独立 chunk

#### `main.js` ✅
创建 app → 注册 router → 引入 global.css → mount `#app`。不引入状态管理库（Pinia/Vuex），状态用 localStorage + 组件内 ref。

### 6.2 路由与守卫 ✅ `router/index.js`

- 模式：`createWebHashHistory`（hash 路由，部署无需服务器配置）
- 守卫 `guard(to, from, next)`：
  - `meta.requiresAuth` 且无 `hw_token` → 跳 `/login`
  - `meta.adminOnly` 且 role≠ADMIN → 跳 `/admin/dashboard`
  - 已登录访问 `/login`/`/register` → 跳 `/`（大屏）

| 路径 | 组件 | 鉴权 | 说明 |
|------|------|------|------|
| `/` | Cockpit.vue | requiresAuth | 全息数据大屏 |
| `/trend` | TrendAnalysis.vue | requiresAuth | 销售趋势分析（角色差异化） |
| `/hotmodels` | HotModels.vue | requiresAuth | 热销机型分析（角色差异化） |
| `/traffic` | TrafficAnalysis.vue | requiresAuth | 流量来源分析（角色差异化） |
| `/profile` | UserProfileAnalysis.vue | requiresAuth | 用户画像分析（角色差异化） |
| `/login` `/register` | Login/Register | — | 登录/商家注册（注册需审） |
| `/admin` | AdminLayout.vue | requiresAuth | redirect `/admin/dashboard` |
| `/admin/dashboard` | admin/Dashboard.vue | — | 后台首页 |
| `/admin/phones` | admin/PhoneManage.vue | — | 机型管理 |
| `/admin/orders` | admin/OrderManage.vue | — | 订单管理 |
| `/admin/users` | admin/UserManage.vue | adminOnly | 用户管理 |
| `/admin/audit` | admin/MerchantAudit.vue | adminOnly | 商户审批 |
| `/admin/import` | admin/DataImport.vue | adminOnly | Excel 导入 |

### 6.3 API 封装 ✅ `api/index.js`

```js
const http = axios.create({ baseURL: '/api', timeout: 30000 })
// 请求拦截：附 JWT
http.interceptors.request.use(config => {
  const token = localStorage.getItem('hw_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
// 响应拦截：code===200 解包 data；blob 透传；401 清 token 跳登录
```

导出 4 组 API：
- `cockpitApi`：getAll/getRegion/getCity
- `aiApi`：chat（fetch 手动读流）/query/getAlerts/checkAlerts/getReport
- `authApi`：login/register
- `adminApi`：phones CRUD/orders/users/stats + **getTrend/getHotModels/getTraffic/getProfile**（4 个角色差异化） + audit 系列 + import 系列

- `aiApi.chat` 用原生 `fetch`（非 axios）以便手动读取 SSE 流
- `importApi.downloadTemplate` 用 `responseType:'blob'`
- `importApi.upload` 超时放宽至 120s

### 6.4 大屏视图 ✅ `views/Cockpit.vue`（部分核验）

三列布局：
- 背景层：`ParticleBackground`/`MatrixRain`/`ScanOverlay` + grid/radial/vignette
- 顶部：`HeaderBar`（标题/时钟/音频控制/后台入口/AI 按钮）
- 左列：4 统计卡 `StatCard`（2×2）+ 30 日趋势 `SalesTrendChart`（含"详情分析→"跳 `/trend`）
- 中央：`SalesHero`（销售额翻牌+环比）+ `DualMapChart`（陕西⇄全国双地图）
- 右列：`HotProductRank` + `TrafficBarChart`（旭日/柱状）+ `UserProfileChart`
- 底部：`OrderMarquee`（实时订单跑马灯）
- 弹窗：`AIChat`（AI 对话）+ `AlertToast`（预警）

### 6.5 组件清单 📋（依 README + 文件树）

| 组件 | 职责 |
|------|------|
| `StatCard.vue` | 指标卡，数字滚动 + sparkline |
| `SalesHero.vue` | 中央销售额翻牌主视觉 |
| `DualMapChart.vue` | 双地图（陕西地市/全国）切换 |
| `SalesTrendChart.vue` | 30 日趋势折线 |
| `HotProductRank.vue` | 热销 TOP10 横向条形 |
| `TrafficBarChart.vue` | 流量来源旭日/柱状 |
| `UserProfileChart.vue` | 用户画像环形 |
| `OrderMarquee.vue` | 实时订单跑马灯 |
| `AIChat.vue` | AI 对话窗（流式打字机） |
| `AlertToast.vue` | 预警弹窗 |
| `HeaderBar.vue` | 顶栏（含音频控制） |
| `ParticleBackground.vue`/`MatrixRain.vue`/`ScanOverlay.vue` | 背景动效 |

### 6.6 角色差异化分析页 📋

`TrendAnalysis.vue`（销售趋势）/`HotModels.vue`（热销机型）/`TrafficAnalysis.vue`（流量来源）/`UserProfileAnalysis.vue`（用户画像）四个独立全屏页，均：
- 调用对应 `/admin/{trend|hotmodels|traffic|profile}` 接口
- 据 `localStorage.hw_role` 区分 ADMIN/MERCHANT 视图（商家隐藏金额、显示"🔒 金额指标不可见"声明）
- ⚠️ 挂载时序要点（见 2.3）：图表 init 必须延迟到 `ready` 渲染出 DOM 后，且 `loading=false` 要在 `renderAll` 前置

### 6.7 管理后台 📋

`AdminLayout.vue`（侧边栏导航 + 退出登录）+ 6 子页：Dashboard/PhoneManage/OrderManage/UserManage/MerchantAudit/DataImport。侧边栏含"📈 销售趋势分析"入口跳 `/trend`。

### 6.8 音频系统 📋 `audio/manager.js`

依 README 与设计文档：Web Audio API 程序化合成（非音频文件回放，但 `holo-music/` 与 `public/audio/*.mp3` 为 v3 背景音乐素材）。
- 背景音乐：科技氛围电子（Pad+琶音）/ v3 为 CC0 三曲连播
- 按键音/交互反馈音
- Ducking：交互音触发时背景音压低 60% 后渐恢复
- 首次交互后启动（浏览器自动播放策略）；音量滑杆 + 静音 + 偏好记忆（v3）

---

## 7. 数据层与辅助模块

### 7.1 数据库 📋 `sql/init.sql`（70KB）

依设计文档与 `AiService.TABLE_DDL`，库 `huawei_cockpit` 含 12 张表：

| 表 | 说明 |
|----|------|
| phone_model | 机型（name/series/price/rating/image_url） |
| region_sales | 省份销售（province/sales/orders/users/stat_date） |
| city_sales | 城市销售 |
| realtime_order | 实时订单（order_no/user_name/model_name/amount/province/city/status/create_time） |
| hot_product | 热销机型（rank_no/model_name/sales_count/sales_amount/growth） |
| traffic_source | 流量来源（category/source_name/visits/ratio，两级支撑旭日图） |
| user_profile | 用户画像（profile_type gender/age, profile_name, user_count, ratio） |
| sales_trend | 销售趋势（stat_date/sales/orders/visits/avg_order_value） |
| overview_stats | 总览统计（今日/累计指标 + 环比） |
| sys_user | 系统用户（含 status 审批状态） |
| ai_chat_log | AI 对话日志 |
| alert_record | 预警记录（alert_type/title/content/level/ai_advice） |

数据生成规则：陕西占 55%（西安 38% 领跑，陕西 10 地市按 GDP 权重）、真实华为机型定价、30 天趋势含周末效应。

### 7.2 爬虫 ✅ `crawler/vmall_crawler.py`

```python
# 爬取 vmall.com/list-36 公开商品页 → phone_model 表（存在则更新价格）
LIST_URL = 'https://www.vmall.com/list-36'
REQUEST_INTERVAL = (2, 5)   # 礼貌爬取 2~5s/次
# parse_products: 解析 ul#productList li，提取机型名/价格/系列
# upsert_to_db: pymysql 连接 huawei_cockpit，存在则 UPDATE price，否则 INSERT
```
- 合规边界：仅爬公开页面、控制频率、不碰登录接口
- DB 密码读 `os.environ.get('DB_PASSWORD', '123456')`
- ⚠️ 注释提示：vmall 为动态渲染页面，结构变化或被反爬时会失败

### 7.3 数据生成器 📋 `tools/generate_sql.js`

依 README：`node tools/generate_sql.js` 重新生成 `sql/init.sql`。生成陕西为主（55%）省份权重、陕西 10 地市 GDP 权重、30 天业务数据。另有 `tools/diag_sse.js`/`test_sse.js` 用于 SSE 诊断测试。

### 7.4 背景音乐素材 📋 `holo-music/` 与 `public/audio/`

- `holo-music/index.html`：音乐播放器页面（含 `user-image.png`）
- `public/audio/bgm-1-tech.mp3`/`bgm-2-dark.mp3`/`bgm-3-calm.mp3`：CC0 三曲（v3 连播）
- `public/audio/LICENSE.md`：CC0 许可说明
- `public/channels/*.ico`：流量渠道图标（百度/B站/京东/天猫/微博/微信/小红书等）
- `public/phones/*.png`：华为机型图片（Mate/nova/Pura/Pocket/畅享 等）
- `public/china.json`/`shaanxi.json`：ECharts 地图 GeoJSON

---

## 8. 依赖关系

### 8.1 后端 Maven 依赖 ✅

| 依赖 | 用途 |
|------|------|
| spring-boot-starter-web | Web/MVC |
| spring-boot-starter-validation | 参数校验 |
| mybatis-plus-boot-starter 3.5.3.1 | ORM/CRUD/分页 |
| mysql-connector-j | MySQL 驱动（runtime，8.0.33，兼容 5.6） |
| lombok | 简化 POJO |
| hutool-all 5.8.22 | 工具（`SecureUtil.md5`/`JSONUtil`） |
| jjwt 0.9.1 | JWT 签发解析 |
| jaxb-api 2.3.1 | jjwt 0.9.1 在 JDK9+ 的依赖（JDK8 无影响） |
| easyexcel 3.3.4 | Excel 解析与生成 |
| spring-boot-starter-test | 测试 |

### 8.2 前端 npm 依赖 ✅

| 依赖 | 用途 |
|------|------|
| vue 3.4 / vue-router 4.2.5 | 框架/路由 |
| axios 1.6.2 | HTTP（含 JWT 拦截） |
| echarts 5.4.3 + echarts-gl 2.0.9 | 可视化（含 3D/地图） |
| three 0.185 | 3D（设计文档原含 DataGlobe，当前仓库 📋 未确认是否启用） |
| vite 5 / @vitejs/plugin-vue 5 / terser | 构建 |

### 8.3 外部服务依赖

| 服务 | 用途 | 可选性 |
|------|------|--------|
| MySQL 5.6+ | 数据存储 | 必需 |
| 火山方舟（豆包）`ark.cn-beijing.volces.com/api/v3` | AI 对话/NL2SQL/预警建议/报告摘要 | 可选（不配 Key 时 AI 降级，其余功能不受影响） |
| vmall.com | 爬虫爬真实机型（可选） | 可选 |

---

## 9. 关键流程时序

### 9.1 登录与角色
```
前端 Login.vue → authApi.login(username,password)
  → POST /api/auth/login
  → AuthService.login: 查 SysUser → MD5 比对 → 状态拦截 → JwtUtil.createToken(24h)
  → 返回 {token, role, username, merchantName}
前端存 localStorage: hw_token, hw_role → 路由守卫放行
```

### 9.2 大屏加载（含脱敏）
```
Cockpit.vue mounted → cockpitApi.getAll()
  → GET /api/cockpit/all
  → CockpitController.getAll: CockpitService.getAll() 聚合 8 组
  → 若 isMerchant: desensitize(金额置 null, masked=true)
  → 前端据 masked 切换脱敏展示
```

### 9.3 AI 对话（SSE 流式）
```
AIChat.vue → aiApi.chat(message, history)  // 原生 fetch
  → POST /api/ai/chat (TEXT_EVENT_STREAM)
  → AiController.chat → AiService.chatStream
    → buildContext() 注入实时大屏数据为 system prompt
    → 携带最近 12 条 history
    → ArkClient.chatStream: 逐行读 SSE，delta.content 转发 SseEmitter
    → Ark 失败: localReply 本地数据概况模板
    → 发送 [DONE] + complete
  → 前端读 ReadableStream 打字机渲染
```

### 9.4 NL2SQL 智能查询
```
AiService.query(question)
  ① Ark 生成 SQL（system: TABLE_DDL + 规则；user: question）
  ② cleanSql（去 markdown/分号）
  ③ validateSql（SELECT 开头/无 ; /无注释/禁关键字/表白名单）
  ④ executeQuery（setMaxRows 50, setQueryTimeout 10s, ColumnMapRowMapper）
  ⑤ Ark 二次解读（system: buildContext；user: 问题+SQL+结果 JSON）
  → 返回 {sql, columns, rows, reply}
```

### 9.5 Excel 导入
```
DataImport.vue → importApi.upload(table, file)
  → POST /api/admin/import/{table} (multipart)
  → JwtFilter 注入 role=ADMIN
  → ImportController.importExcel: isAdmin 校验
  → ImportService.importExcel:
    - readExcel（EasyExcel 简单模式 + 中文表头映射）
    - 校验表头列齐全
    - 逐行 parseRow（必填/数字/日期/枚举），错误行记 RowError
    - saveAll（phone 重名更新价格，trend 日期重复更新）
  → 返回 ImportResult{total,success,fail,errors}
```

### 9.6 预警检测
```
AlertService.checkAlerts()
  - checkRegionAlerts: latest vs prev（MAX/MIN stat_date）
    跌>25% → SALES_DROP(level2)；涨>40% → SALES_SURGE(level1)
  - checkCityAlerts: 同上（均 level1）
  - createAlert: 当天同 title 去重，generateAdvice 调 Ark（失败降级模板）
  → 返回新增预警数
前端 AlertToast 展示 + 警示音
```

---

## 10. 安全设计要点

| 维度 | 实现 | 核验 |
|------|------|------|
| 认证 | JWT（HS256，24h），`JwtUtil` | ✅ |
| 授权-粗粒度 | 路由守卫 `meta.requiresAuth`/`adminOnly` | ✅ |
| 授权-细粒度 | Controller 内 `isAdmin`/`isMerchant` 判角色 | ✅ |
| 金额脱敏 | 后端 Controller 层字段级过滤（非仅前端隐藏） | ✅ |
| NL2SQL 防注入 | SELECT 白名单 + 禁关键字 + 禁多语句/注释 + 50 行/10s 限制 | ✅ |
| 密码存储 | MD5 小写 hex（Hutool SecureUtil） | ✅ ⚠️ MD5 已不安全，仅课程设计 |
| 商家注册 | PENDING 待审机制（类群邀请：管理员通过后方可登录） | ✅ |
| 敏感信息 | env 占位 `${DB_PASSWORD}`/`${ARK_API_KEY}` + application-local.yml（gitignored） | ✅ |
| JWT 密钥 | 硬编码于 `JwtUtil.SECRET` | ✅ ⚠️ 仓库公开，生产需改 |

---

## 11. 项目运行方式 ✅

### 11.1 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 8+（推荐 17） | 后端 |
| Node.js | 16+（推荐 18/20） | 前端 |
| MySQL | 5.6+ | 数据存储 |
| Maven | 无需安装 | 自带 Maven Wrapper（mvnw） |

### 11.2 一键启动（Windows，推荐）
```bash
git clone https://github.com/WRY907/studious-octo-happiness.git
cd studious-octo-happiness
# 双击 start.bat
```
`start.bat` 自动完成：检查 Java/Node 环境 → 前端依赖缺失时 `npm install`（国内镜像）→ 检测数据库未初始化时引导导入 `sql/init.sql` → 双窗口启动前后端 → 自动打开浏览器。

macOS/Linux：`chmod +x start.sh && ./start.sh`

### 11.3 手动启动
```bash
# 1. 初始化数据库（创建 huawei_cockpit 库 + 12 表 + 30 天数据）
mysql -uroot -p < sql/init.sql

# 2. 配置密码/Key（见 11.4）

# 3. 启动后端（:8080）
cd holo-cockpit-backend
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # macOS/Linux

# 4. 启动前端（:3000）
cd holo-cockpit-frontend
npm install --registry=https://registry.npmmirror.com
npm run dev
```

### 11.4 配置注入（二选一）

**方式一：本地私密配置文件（推荐）**
```bash
cd holo-cockpit-backend
cp src/main/resources/application-local.example.yml application-local.yml
# 编辑 application-local.yml 填真实 password 与 ark.api-key
```
`application-local.yml` 已被 `.gitignore` 忽略。

**方式二：环境变量**
| 变量 | 说明 | 默认 |
|------|------|------|
| `DB_USERNAME` | MySQL 用户名 | root |
| `DB_PASSWORD` | MySQL 密码 | 123456 |
| `ARK_API_KEY` | 火山方舟 Key | 空（AI 降级） |

火山方舟 Key 获取：https://console.volcengine.com/ark ；不配置时 AI 提示"服务暂时不可用"，其余功能不受影响。

### 11.5 访问入口与默认账号

| 入口 | 地址 |
|------|------|
| 数据大屏 | http://localhost:3000/ |
| 管理后台登录 | http://localhost:3000/#/login |
| 后端 API 基址 | http://localhost:8080/api |

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | admin | admin123 |
| 商家 | merchant | merchant123 |

流程：先登录，登录成功后方可进入全息数据大屏。

### 11.6 重新生成数据与爬虫（可选）
```bash
node tools/generate_sql.js && mysql -uroot -p < sql/init.sql   # 重新生成仿真数据
pip install requests beautifulsoup4 pymysql
export DB_PASSWORD=你的密码
python crawler/vmall_crawler.py                                # 爬取真实机型更新 phone_model
```

---

## 12. FAQ 与开发经验 ✅

**Q：后端启动报数据库连接失败？**
A：检查 MySQL 服务、`application-local.yml` 密码、`huawei_cockpit` 库是否已 `sql/init.sql` 初始化。

**Q：AI 提问显示"AI 服务暂时不可用"？**
A：未配置火山方舟 Key。填 `ARK_API_KEY` 后重启后端。AI 失败时 `AiService` 自动降级本地模板回复，不影响大屏/后台。

**Q：前端依赖安装慢/失败？**
A：用国内镜像 `npm install --registry=https://registry.npmmirror.com`（start.bat 已默认使用）。

**Q：端口被占用？**
A：后端 8080 / 前端 3000。改 `application.yml` 的 `server.port` 与 `vite.config.js` 的 `server.port`（同步改代理 target）。

**Q：登录后白屏？**
A：确认后端已完全启动（日志 `Started HoloCockpitApplication`），前端代理依赖后端 8080。

**Q：MySQL 8.x 能用吗？**
A：可以。驱动为 mysql-connector-j，兼容 5.6~8.x；若密码认证失败，确认用户密码插件为 `mysql_native_password` 或依赖 `allowPublicKeyRetrieval=true`（已默认开启）。

**开发期踩坑（来自会话，供二次开发参考）：**
- ECharts 初始化必须在图表容器 DOM 渲染之后（`v-else-if`/`v-if` 切换时尤其注意），否则 "Initialize failed: invalid dom" 并中断后续逻辑
- `loading` 状态置 false 要在调用 render 之前，否则 `v-if=loading` 分支仍占 DOM，`nextTick` 取不到图表 ref
- Vite 修改 `router/index.js` 后若路由不生效，重启 dev server（HMR 对路由模块不可靠）
- `git push` 遇 github.com 超时多为国内 IP 线路问题，重试或换线路

---

## 13. 声明 ✅

- 本项目为课程设计/学习交流用途，数据为仿真生成，与华为公司真实业务数据无关
- "华为"及相关商标归华为技术有限公司所有，本项目仅作教学演示
- 请勿将真实 API Key、数据库密码提交到公共仓库（本仓库已脱敏，敏感值用环境变量/local 文件注入）

---

## 附录 A：API 接口总览 ✅

所有接口以 `/api` 为前缀，除登录外均需 `Authorization: Bearer <token>`。

**认证**：`POST /auth/login`、`POST /auth/register`
**大屏**：`GET /cockpit/all`、`GET /cockpit/region`、`GET /cockpit/city`
**管理后台**：`GET/POST /admin/phones`、`PUT/DELETE /admin/phones/{id}`、`GET /admin/orders`、`PUT /admin/orders/{id}/status`、`DELETE /admin/orders/{id}`、`GET /admin/users`、`GET /admin/stats`、`GET /admin/trend`、`GET /admin/hotmodels`、`GET /admin/traffic`、`GET /admin/profile`、`/admin/audit/*`（审批）
**Excel 导入（仅 ADMIN）**：`GET /admin/import/meta`、`GET /admin/import/{table}/template`、`POST /admin/import/{table}`
**AI**：`POST /ai/chat`(SSE)、`POST /ai/query`、`GET /ai/alerts`、`POST /ai/alerts/check`、`POST /ai/report`

## 附录 B：提交历史要点 ✅

仓库 10 次提交，主要节点：
- `add6b32` 初始提交（103 文件，华为手机全息数据驾驶舱全栈项目）
- `1fc29d5` 商家注册与管理员审批流程（注册需审核，类群邀请机制）
- `4fc2b54` 销售趋势分析独立页 + 角色差异化（6 文件 +811 行）
- `21d72ca` 背景音乐系统 v3（CC0 三曲连播+播放器 UI+事件音效+偏好记忆）— HEAD

---

## v2 升级记录（2026-09-08 · Motion Plus 更新）

本次升级引入炫酷落地页、大屏动效增强与真实机型数据接入。

### 新增：全息落地页 Landing.vue（路由 `/`）

公开访问的项目形象展示页，融合 motionsites.ai 提取的顶级 motion 设计模式：

| 模块 | 说明 |
|------|------|
| Three.js 星云背景 | 2600 粒子螺旋星系（青→紫渐变顶点色），鼠标视差 + 持续旋转，WebGL 不可用时静默降级 |
| Kinetic Typography | 标题"全息数据驾驶舱"逐字入场（translateY + rotateX + blur 消散） |
| 打字机副标题 | "HOLO DATA COCKPIT · SEE THE FUTURE" 逐字打出 |
| Marquee 双向滚动带 | 描边文字无限滚动（tech 关键词 / 技术栈） |
| 机型矩阵 | 14 款华为真实机型 3D Tilt 卡片（鼠标跟随透视旋转 + 光斑），滚动 stagger 显现 |
| 核心能力卡 | 4 张玻璃拟态卡片（大屏/AI/权限/导入），hover 3D 倾斜 |
| 数据规模 | IntersectionObserver 触发数字滚动动画 |
| 交互细节 | 磁吸主按钮、自定义滚动条、锚点平滑滚动（scrollIntoView 规避 hash 路由冲突） |

技术要点：落地页自成滚动容器（`height:100vh; overflow-y:auto`）以兼容全局
`overflow:hidden` 体系；`prefers-reduced-motion` 降级；组件卸载时完整清理
（Three.js dispose、Observer disconnect、事件监听移除）。

### 路由调整

- `/` → Landing.vue（公开落地页）
- `/cockpit` → Cockpit.vue（数据大屏，需登录）
- 登录成功、已登录访问 /login、"返回大屏"链接 → 均指向 `/cockpit`

### 大屏动效增强（cockpit-motion.css）

无侵入 CSS 叠加层（`.cockpit-container` 前缀提高特异性，`!important` 突破
animation fill 锁定）：

1. 面板 hover 四层辉光 + 上浮
2. HUD 四角括号呼吸闪烁（hover 加速）
3. 面板编号（01-05）辉光脉冲
4. 背景网格 26s 缓慢漂移
5. 径向背景 22s 色相流转（hue-rotate）
6. 统计卡 hover 微升辉光
7. 底部跑马灯边框流光
8. `prefers-reduced-motion` 全量降级

### 真实机型数据接入（华为商城合规爬取）

- 爬取 vmall.com 公开列表页 14 款在售机型（名称/价格/428x428 图片）
- 图片本地化：`holo-cockpit-frontend/public/images/phones/*.png` + `meta.json`
- `hot_product` 表 TOP10 对齐真实在售 lineup（销售额保持仿真规模）
- `phone_model` 表新增 10 款机型（真实价格），Mate X7 价格对齐 ¥11999
- HotProductRank.vue 增加机型缩略图 + 名称模糊匹配器（`PHONE_LIBRARY`）

### Bug 修复

- DualMapChart resize 竞态：容器瞬时 0 尺寸时守卫跳过 + 200ms 恢复重试 +
  try-catch 兜底，修复地图塌陷不自愈问题
- .gitignore 规则修正：`**/application-local.yml`（原路径规则不匹配实际位置）

### 背景音乐

`public/audio/bgm-you-should-know.mp4`（李长庚《You Should Know》，AAC 音频轨，
浏览器 `<audio>` 原生解码 107.7s）。播放列表见 `src/audio/manager.js`。
