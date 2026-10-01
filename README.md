# 火车票秒杀抢票系统

基于 **SSM（Spring + SpringMVC + MyBatis）** 的火车票购票 / 秒杀演示项目，**前后端分离**

| 模块 | 说明 | 打包方式 |
| --- | --- | --- |
| `qiangpiao-backend` | 后端 SSM 服务，纯注解配置，**打 war 包部署到 Tomcat** | `war`（`finalName=/`，上下文 `/`） |
| `qiangpiao-frontend` | Vue3 + Vite 前端，独立运行 / Nginx 部署 | 静态资源 `dist` |

实现功能：车次查询、**区间票（同座位分段复用）**、余票与座位图（按乘车区间计算）、**车次售卖时间窗**、车站字典、**JWT 登录鉴权 + 登出 / 强制下线（Redis token 黑名单）**、**Redis Lua 原子预扣库存秒杀抢票**、异步下单、**SSE 抢票结果推送（轮询兜底）**、**钱包余额支付 / 零钱流水 / 自定义充值**、订单取消、超时关单、退票改签、**L1/L2/L3 三级缓存**、**全链路 traceId 追踪**、IP 黑名单与风控、接口文档（Knife4j）、Druid 监控。

## 功能实现

### **C 端用户功能**

| 模块       | 功能描述                                                     |
| ---------- | ------------------------------------------------------------ |
| 账号       | 注册（手机号号段 + 身份证校验位双重校验）、登录（BCrypt + JWT）、登出（token 进 Redis 黑名单，立即失效）、当前用户信息 |
| 常用乘车人 | 增删改查，最多 9 位、同身份证查重、AES 加密存储 + 脱敏返回   |
| 车次查询   | 分页、OD 站点查询（支持中途站→中途站）、30 天范围、三级缓存  |
| 车次详情   | 余票、按乘车区间计算的余票与区间座位图、经停站时刻表、购票资格预检  **问题** |
| 抢票秒杀   | 限流 → 限购 → Redis Lua 原子预扣 → 异步落库；一次最多买 9 张（多乘客）；排队序号；SSE 毫秒级推送结果（轮询兜底）；抢票流水 |
| 订单       | 列表分页、详情（身份证脱敏）、钱包支付、取消（释放座位 + 归还库存）、超时自动关单 |
| 我的车票   | 待出行 / 历史车票分类、票态（待出行 / 已出行 / 已失效）、距发车天数 |
| 退票       | 已支付且未发车可退，票款退钱包；开车后不可退（4011）、改签过的票开车后不可退（4012）、发车次日起不可退（4016） |
| 改签       | 换到未发车有余票的车次，差额多退少补；一张票只能改签一次；改签历史 + 订单流转时间轴 |
| 钱包       | 余额、累计充值 / 消费、自定义金额充值（0.01~50000）、零钱流水（充值 / 消费 / 退款） |
| 支付       | 钱包余额扣款（同一事务）、支付回调验签 + nonce 防重放 + 时间戳窗口 |
| 人机验证   | 图形验证码、滑块验证码（抢票前置，防脚本）                   |
| 公告       | 免登录查看运营公告                                           |

### 二、管理后台

- 车次：停开班次、时刻表（经停站与到发时刻）、车厢编排、按日期生成每日班次、票价与总票数调整、售卖时间窗、区间库存
- 初始化线路 / 车站：线路 CRUD + 途经站与顺序、车站停用启用
- 订单：多条件筛选、详情、操作日志、改签记录、人工退票（客服）
- 用户：封禁/解封（封禁同步踢下线）、强制下线（拉黑该用户所有 token）
- 公告：发布/编辑/删除
- 监控：实时余票、锁票情况
- 报表：总览统计、日销售额、车次销量、用户增长、CSV 导出
- 风控：风险事件流水、IP 黑名单（拉黑/解封）、秒杀流水查询

### 三、售票业务规则

预售期 14 天 / 查询范围 30 天、开车前 20 分钟停售、已发车不可售、每车次最多 9 张（不分席别）、同一乘车人不可重复购票、行程运行时间冲突不可购票（3007）、区间票同座位分段复用（区间余票取覆盖各段最小值）、区间票暂不支持改签（4017）

### 四、高并发与基础设施

三级缓存（L1 Caffeine / L2 Redis / L3 MySQL，防穿透 __NULL__、防击穿本地锁、防雪崩 TTL 抖动、写后双删）、Redis Lua 原子预扣与回滚、异步线程池落库 + 失败补偿、DB 乐观锁防超卖、用户/IP/全局三级限流、IP 黑名单拦截器、敏感数据 AES 加密 + 脱敏、traceId 全链路追踪（响应体/响应头/日志同值，异步线程 MDC 传递）、慢 SQL 与慢请求（>1s 转 WARN）日志、Druid 监控、Knife4j 文档、定时任务（库存预热 / 超时关单 / 每日班次生成）

### 五、前端体验

移动端适配（≤768px）、骨架屏、幂等 GET 自动重试 2 次（POST 下单支付绝不重试）、SSE 断开自动降级轮询、失败 toast 带 traceId、localStorage 缓存车站与查询结果

---

## 一、技术栈

### 后端

| 类别 | 选型 | 版本 |
| --- | --- | --- |
| 核心框架 | Spring + SpringMVC + MyBatis | Spring `5.3.34`、MyBatis `3.5.16`、mybatis-spring `2.1.2` |
| 安全 | Spring Security + JWT（jjwt）+ BCrypt | Security `5.8.12`、jjwt `0.11.5` |
| 数据源 | Druid 连接池 + MySQL 8 驱动 | Druid `1.2.22`、mysql-connector-j `8.3.0` |
| 缓存 | Caffeine（L1）+ Redis / Lettuce（L2）+ MySQL（L3） | Caffeine `2.9.3`、spring-data-redis `2.7.18`、Lettuce `6.1.10` |
| 参数校验 | JSR-303 / Hibernate Validator | `6.2.5.Final` |
| 文档 | Knife4j + Springfox | `2.0.9` / `2.9.2` |
| 工具 | Lombok、Logback、Jackson、Commons-Lang3 | `1.18.34` / `1.2.13` / `2.15.4` / `3.12.0` |
| 运行环境 | **Java 8**（`maven.compiler.source/target=8`），Servlet 4.0.1 | 外部 **Tomcat 9.x** |

### 前端

Vue 3 + Vite + Vue Router + Pinia + Element Plus + Axios + dayjs；前端侧用 `localStorage` 做**浏览器级缓存**（车站列表、车次查询结果 30 秒）。

移动端 / 弱网体验：

| 能力 | 实现 |
| --- | --- |
| 移动端适配 | `styles/global.css` 的 `@media (max-width: 768px)`：表单竖排、控件撑满、座位图 4 列、表格压缩并横向滚动、分页居中、弹窗 92vw、按钮加大触控区；顶部导航在窄屏横向滚动（管理员 9 个模块也不挤） |
| 骨架屏 | `components/SkeletonCard.vue`，车次列表 / 车次详情 / 我的订单 / 我的车票 / 钱包 / 退票页在 `loading` 时展示，替代空白页 |
| 错误重试 | `components/ErrorRetry.vue` 页面级「加载失败 + 重新加载」按钮；`utils/request.js` 对**幂等 GET** 自动重试 2 次（400ms/800ms 退避），仅在网络中断 / 超时 / 5xx 时触发，**POST 下单与支付绝不重试**避免重复下单 |

### 为什么必须用 Tomcat 9.x

项目使用 `javax.servlet-api 4.0.1`，而 **Tomcat 10+ 已切换为 `jakarta.*` 命名空间**，使用 10.x 会直接 `ClassNotFoundException` / `NoSuchMethodError`。请务必下载 Tomcat **9.0.x**。

---

## 二、目录结构

```
qiangpiao
├─ pom.xml                       # 父工程（pom）：统一依赖版本管理
├─ README.md
├─ 技术.md                        # 技术选型说明
├─ qiangpiao-backend/
│  ├─ pom.xml                    # packaging=war，finalName=qiangpiao
│  └─ src/main/
│     ├─ java/com/qiangpiao/
│     │  ├─ config/              # RootConfig(根容器) / WebMvcConfig(MVC子容器) / DataSourceConfig(Druid)
│     │  │                       # MybatisConfig / RedisConfig(Lua脚本) / CacheConfig / SecurityConfig / AsyncConfig / SwaggerConfig
│     │  ├─ controller/          # 7 个 REST Controller，全部 /api/** 前缀
│     │  ├─ interceptor/         # TraceIdInterceptor(traceId+MDC) / BlackIpInterceptor(IP 黑名单) / SlowSqlInterceptor(慢 SQL)
│     │  ├─ service/ + service/impl/   # 业务层，Service 之间传递 BO
│     │  │                       # SegmentStockService(区间库存) / SeckillSseService(SSE 推送) / TokenService(token 黑名单)
│     │  ├─ mapper/              # MyBatis 接口（SQL 在 resources/mapper/*.xml）
│     │  ├─ dataobject/          # DO 数据库实体（注意包名是 dataobject）
│     │  ├─ dto/                 # 入参 DTO，带 JSR-303 注解
│     │  ├─ bo/                  # Service 层业务对象
│     │  ├─ vo/                  # 出参 VO，屏蔽敏感字段
│     │  ├─ cache/               # LocalCache(L1 Caffeine) / MultiLevelCacheService(三级缓存门面)
│     │  ├─ security/            # JwtAuthenticationFilter / LoginUser
│     │  ├─ task/                # StockPreheatTask(库存预热) / OrderTimeoutTask(超时关单)
│     │  └─ common/              # result(R, ResultCode, PageResult) / exception(BizException, GlobalExceptionHandler)
│     │                          # constant(Constants, RedisKeys) / context(UserContext) / util / validate
│     ├─ resources/
│     │  ├─ config.properties    # 唯一外部化配置源（DB / Redis / 缓存 / JWT / 秒杀）
│     │  ├─ logback.xml          # PATTERN 带 %X{traceId} / %X{userId} / %X{orderNo}
│     │  ├─ mapper/*.xml         # 7 个 MyBatis 映射文件
│     │  └─ sql/schema.sql、sql/data.sql
│     └─ webapp/WEB-INF/web.xml  # ContextLoaderListener + DispatcherServlet + Security/Druid 过滤器
├─ scripts/
│  ├─ import-data.js             # 生成车站 / 线路 / 车次导入 SQL（只生成不执行）
│  └─ sql/                       # 生成的导入 SQL + segment_ticket.sql（区间票建表 / 加列 / 补经停站）
└─ qiangpiao-frontend/
   ├─ vite.config.js             # dev 代理：/api → http://localhost:8081 + 上下文
   └─ src/
      ├─ api/                    # auth / train / station / order / seckill
      ├─ views/                  # Login、TrainList、TrainDetail、OrderList、Profile
      ├─ router/                 # /trains、/orders、/profile(需登录) 等
      ├─ store/                  # Pinia：user / station
      └─ utils/                  # request.js(axios 封装) / cache.js / dayjs.js
```

### 分层规范

```
Controller  ──接收──▶ DTO ──▶ Service ──流转──▶ BO ──▶ Mapper ──▶ DO ──▶ MySQL
Controller ◀──返回── VO  ◀── Service
```

- **DTO**：Controller 入参，JSR-303 校验（`@Valid` + `ValidationGroups` 分组）
- **BO**：Service 之间传递，承载多表组装结果
- **DO**：与数据表一一对应（`BaseDO` 提供公共字段）
- **VO**：Controller 出参，脱敏（如身份证）后返回给前端

---

## 三、运行环境要求

| 组件 | 要求 |
| --- | --- |
| JDK | 8 及以上（本机实测 JDK 21 可正常编译运行） |
| Maven | 3.6+ |
| MySQL | 8.x（需创建 `qiangpiao` 库） |
| Redis | 5.x+（秒杀库存、缓存、限流均依赖 Redis） |
| Tomcat | **9.0.x**（不能用 10+） |

---

## 四、快速开始

### 1. 初始化数据库

```bash
cd qiangpiao-backend/src/main/resources/sql
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS qiangpiao DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p qiangpiao < schema.sql
mysql -uroot -p qiangpiao < data.sql
mysql -uroot -p qiangpiao < schema_admin.sql
# 存量库升级：手机号 / 身份证改为 AES 密文存储后必须扩列（密文 28~48 字符），
# 否则注册 / 下单会报 Data truncation: Data too long for column 'phone'
mysql -uroot -p qiangpiao < <项目根目录>/scripts/sql/encrypt_column_resize.sql
```

### 1.1 导入全量车站与公开高铁线路车次（可选但推荐）

```bash
cd 项目根目录
node scripts/import-data.js          # 生成 SQL：scripts/sql/*.sql
mysql -uroot -p qiangpiao < scripts/sql/stations_import.sql   # 3396 个车站（12306 station_names）
mysql -uroot -p qiangpiao < scripts/sql/trains_import.sql     # 15 条高铁线路 / 72 个车次模板
mysql -uroot -p qiangpiao < scripts/sql/future_dates_import.sql  # 复制出未来 13 天班次
```

脚本说明：

| 环节 | 数据来源 | 产出 |
| --- | --- | --- |
| 车站 | `md/数据.js`（12306 `station_names` 全量） | `t_station` 3396 个车站（站名 / 城市 / 拼音简码，`INSERT IGNORE` 去重） |
| 线路 | 内置 15 条公开高铁（京沪、京广、沪昆、京哈、徐兰、成渝、广深港、杭深、京津、沪宁、京张、合福、宁杭、济青、西成） | `t_line` + `t_line_station` 线路与途经站 |
| 车次 | 每条线路按真实号段生成上下行与区段车次（G1/G2…、D3101、C2001） | `t_train` 模板 + `t_train_stock` 三档席别 + `t_seat` 800 座/车次 + `t_carriage` 8 节车厢 + `t_train_stop` 途经站时刻 |
| 未来班次 | 以模板复制 | 未来 13 天班次（库存、座位同步复制） |

- 脚本只生成 SQL，不连数据库；全部语句幂等（`INSERT IGNORE` / `NOT EXISTS`），可重复执行。
- 只需插入「今天」的模板班次，`TrainScheduleTask` 会以最早一天为模板自动补齐到 30 天（启动 10s 执行一次，之后每小时一次）。
- 修改线路/车次：编辑 `scripts/import-data.js` 中的 `LINES` 配置后重新生成即可。

### 2. 修改后端配置

编辑 `qiangpiao-backend/src/main/resources/config.properties`：

```properties
jdbc.url=jdbc:mysql://127.0.0.1:3306/qiangpiao?...
jdbc.username=你的账号
jdbc.password=你的MySQL密码

redis.host=127.0.0.1
redis.port=6379
redis.password=
```

> **不想把密码写进 war？** 数据源支持外部化覆盖，优先级：**环境变量 > JVM 参数 > config.properties**
>
> - 环境变量：`QP_DB_URL` / `QP_DB_USERNAME` / `QP_DB_PASSWORD`
> - Tomcat 启动参数：在 `bin/setenv.bat` 中写 `set JAVA_OPTS=-Djdbc.password=xxx`
> - IDEA：Run/Debug Configurations → Tomcat → VM options 加 `-Djdbc.password=xxx`

### 3. 打包 war

在项目根目录执行：

```bash
mvn -B -pl qiangpiao-backend -am clean package -DskipTests
```

产物：`qiangpiao-backend/target/qiangpiao.war`（约 45 MB）

### 4. 部署到 Tomcat 9

```bash
# war 名为 qiangpiao.war → 上下文路径为 /
copy qiangpiao-backend\target\qiangpiao.war D:\software\tomcat\apache-tomcat-9.0.x\webapps\
cd D:\software\tomcat\apache-tomcat-9.0.x\bin
startup.bat
```

- 若想以根路径访问（`/api/trains` 而非 `/qiangpiao/api/trains`），把 war 改名为 `ROOT.war`（先删除 `webapps/ROOT` 目录）。
- 建议修改 `conf/server.xml` 的 Connector，避免中文 GET 参数乱码：

  ```xml
  <Connector port="8081" protocol="HTTP/1.1"
             connectionTimeout="20000" redirectPort="8443"
             URIEncoding="UTF-8" />
  ```

- 也可以直接用 Tomcat Manager（`http://localhost:8081/manager/html`）上传 war 热部署。

### 5. 启动前端

```bash
cd qiangpiao-frontend
npm install
npm run dev          # http://localhost:5174
```

生产发布：

```bash
npm run build        # 产物 dist/
```

Nginx 参考配置：

```nginx
server {
    listen 80;
    root D:/daima/Lianshi/qiangpiao/qiangpiao-frontend/dist;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
    location /api/   { proxy_pass http://127.0.0.1:8081/qiangpiao/api/; }
    location /druid/ { proxy_pass http://127.0.0.1:8081/qiangpiao/druid/; }
}
```

> ⚠️ **上下文路径必须与前端代理一致**
> 前端 dev 代理见 `qiangpiao-frontend/vite.config.js`：
> ```js
> const BACKEND_ORIGIN  = process.env.VITE_BACKEND_ORIGIN  || 'http://localhost:8081'
> const BACKEND_CONTEXT = process.env.VITE_BACKEND_CONTEXT || '/qiangpiao'
> ```
> 若用 IDEA 部署（默认上下文可能是 `/qiangpiao_backend_war`），需同步改成相同值，否则接口会 404：
> ```bash
> set VITE_BACKEND_CONTEXT=/qiangpiao_backend_war && npm run dev
> ```

### 6. 验证清单

| 地址 | 期望 |
| --- | --- |
| `http://localhost:8081/qiangpiao/api/health` | `{"code":200,"data":{"status":"UP",...}}` |
| `http://localhost:8081/qiangpiao_backend_war/doc.html` | Knife4j 接口文档 |
| `http://localhost:8081/qiangpiao_backend_war/druid/index.html` | Druid 监控（admin / admin123） |
| `http://localhost:5174/trains` | 车次列表（免登录即可查看） |
| `http://localhost:5174/wallet` | 我的钱包：余额 / 充值 / 零钱流水（需登录） |
| `http://localhost:5174/tickets` | 我的车票：未开车（待出行）/ 历史车票（需登录） |

---

## 五、默认账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| `admin` | `admin123` | ROLE_ADMIN | 重导 `data.sql` 后生效 |
| `zhangsan` / `lisi` | `admin123` | ROLE_USER | 同上 |

> 密码使用 **BCrypt** 存储。BCrypt 每次加密带随机盐，`data.sql` 中保存的是一条与 `admin123` 匹配的合法密文；如果重新生成，`密文不同但同样能校验通过`。
> 若某些历史库是用旧脚本初始化的，`admin` 的口令可能为 `admin123`，重跑一次 `data.sql` 即会重置为 `123456`（`ON DUPLICATE KEY UPDATE` 已包含 `password = VALUES(password)`）。

> 执行 `data.sql` 后会自动为所有用户**开通钱包并赠送 1000.00 元**初始余额（幂等，同时写入一条「开户赠送」流水）；购票支付即从该余额扣款，余额不足请到「我的钱包」自定义金额充值。

---

## 六、核心设计

### 6.1 系统架构与业务流转全景图

> 线上（如 GitHub）与多数 IDE 内置预览可直接渲染 Mermaid；本地 Markdown 编辑器看不到图时，可粘到 <https://mermaid.live> 查看。

```mermaid
flowchart TB
    subgraph C["客户端层"]
        FE["浏览器 SPA<br/>Vue3 + Vite + Pinia + Element Plus"]
        LS["localStorage<br/>车站 / 车次结果 30s"]
    end

    subgraph E["接入层"]
        PX["Nginx / Vite Proxy<br/>/api、/druid → Tomcat 上下文"]
    end

    subgraph W["容器层 Tomcat 9（war）"]
        FLT["Spring Security 过滤链<br/>JwtAuthenticationFilter → UserContext"]
        DSP["DispatcherServlet"]
        GEH["GlobalExceptionHandler<br/>统一封装 R&lt;T&gt;"]
    end

    subgraph CT["Controller 层（/api/**）"]
        AC["AuthController<br/>login / register / info"]
        TC["TrainController<br/>车次列表 / 详情"]
        STC["StationController<br/>车站字典"]
        SEC["SeckillController<br/>抢票 / 结果 / 余票 / 预热"]
        OC["OrderController<br/>订单 / 支付 / 取消"]
        HC["HealthController"]
        SEATC["SeatController<br/>座位图"]
    end

    subgraph SV["Service 层（BO 流转）"]
        AS["AuthServiceImpl<br/>BCrypt 校验 + JWT 签发"]
        TS["TrainServiceImpl<br/>在售校验 / 缓存失效"]
        SKS["SeckillServiceImpl<br/>限流 → 一人一单 → Lua 预扣"]
        STS["StationServiceImpl"]
        SES["SeatServiceImpl<br/>选座锁座 / 释放"]
        ORS["OrderServiceImpl"]
    end

    subgraph AZ["异步与调度"]
        POOL["seckillExecutor<br/>core20 / max100 / queue5000"]
        SPT["StockPreheatTask<br/>启动预热 + ADMIN 手动"]
        OTT["OrderTimeoutTask<br/>fixedDelay 60s"]
    end

    subgraph CH["三级缓存"]
        MLC["MultiLevelCacheService<br/>穿透占位 / 本地锁防击穿 / TTL 抖动"]
        L1["L1 Caffeine<br/>进程内 60s"]
        L2["L2 Redis<br/>300s ± 20% 抖动"]
        LUA["Lua 脚本<br/>原子预扣 / 回滚库存"]
    end

    subgraph D["存储层 MySQL 8（Druid）"]
        MP["Mapper / MyBatis XML"]
        T1[("t_user / t_station")]
        T2[("t_train / t_train_stock<br/>version 乐观锁")]
        T3[("t_seat / t_order / t_seckill_record<br/>唯一索引一人一单")]
    end

    FE --> PX
    FE -.-> LS
    PX --> FLT
    FLT -->|"鉴权失败 401/403"| FE
    FLT -->|"解析 JWT 注入登录态"| DSP
    DSP --> CT
    GEH -.->|"异常转状态码"| FE

    AC --> AS
    TC --> TS
    STC --> STS
    SEATC --> SES
    SEC --> SKS
    OC --> ORS

    TS <--> MLC
    STS <--> MLC
    SES <--> MLC
    MLC --> L1
    L1 -->|miss| L2
    L2 -->|miss| MP
    MP -->|"回源后写回"| L2
    L2 -->|"回填"| L1

    SKS --> LUA
    SKS --> POOL
    SPT --> LUA
    OTT --> ORS
    POOL --> ORS

    LUA --> L2
    AS --> MP
    ORS --> MP
    SES --> MP
    TS --> MP
    MP --> T1
    MP --> T2
    MP --> T3
```

### 6.2 统一返回与全局异常

统一返回体 `R<T>`：

```json
{ "code": 200, "message": "操作成功", "data": {}, "traceId": "3f2a9c8b1e7d4a05", "timestamp": 1790181657341 }
```

**`traceId` 全链路追踪**：`TraceIdInterceptor` 在请求入口生成 16 位 ID 写入 MDC，`R.build()` 统一回填到响应体，
同时写入响应头 `X-Trace-Id`，logback 通过 `%X{traceId}` 打进每一行业务日志 —— **响应体 / 响应头 / 日志三者同值**，
用户报障只需给出这个 ID。成功与失败响应都带（全局异常处理器走同一个 `R.build()`）。
秒杀下单跑在 `seckillExecutor` 线程池，靠 `MdcTaskDecorator` 把 traceId 带进异步线程，请求线程与落库线程可对账。

`GlobalExceptionHandler`（`@RestControllerAdvice`）统一处理并转成对应状态码：

| 异常 | 处理 |
| --- | --- |
| `BizException` | 返回业务 code + message |
| `MethodArgumentNotValidException` / `BindException` / `ConstraintViolationException` | 400 + 字段校验提示拼接 |
| `MissingServletRequestParameterException` / `MethodArgumentTypeMismatchException` / `HttpMessageNotReadableException` | 400 + 参数错误提示 |
| `HttpRequestMethodNotSupportedException` | 405 |
| `AuthenticationException` / `AccessDeniedException` | 401 / 403 |
| `Exception`（兜底） | 500 + 记录堆栈 |

`ResultCode` 分段：通用 `200/400/401/403/404/405/429/500/503`，用户 `1001~1005`，车次座位 `2001~2007`，秒杀 `3001~3005`，订单 `4001~4004`。

### 6.3 三级缓存

```
请求 → L1 Caffeine(JVM 本地, 60s) → L2 Redis(默认 300s + 抖动) → L3 MySQL → 回写 L2、L1
```

- **防穿透**：DB 空结果写入占位符 `__NULL__`（60s），命中占位直接返回 null，不回源
- **防击穿**：per-key `ReentrantLock` + 双重检查，回源后回填
- **防雪崩**：TTL 随机抖动 `ttl + random(0~20%*ttl)`，L1 采用更短的 60s
- **一致性**：写操作后 `evict()` 双删 L1 + L2

主要缓存 key：

| key | 格式 | TTL |
| --- | --- | --- |
| 车次列表 | `qp:cache:train:list:{from}-{to}-{date}:{pageNum}:{pageSize}` | 300s |
| 车次详情 | `qp:cache:train:detail:{trainId}` | 300s |
| 车站全量 | `qp:cache:station:all` | 300s |
| 座位图 | `qp:cache:seat:map:{trainId}:{seatType}` | 120s |
| 秒杀库存 | `qp:seckill:stock:{trainId}:{seatType}` | 预热 12h |
| 限购标记 | `qp:seckill:user:{trainId}:{userId}` | 30min |
| 抢票结果 | `qp:seckill:result:{trainId}:{seatType}:{userId}` | 30min |
| 接口限流 | `qp:limit:user:{userId}` | 60s |

> 说明：回源锁是 **JVM 本地锁**（`ConcurrentHashMap` + `ReentrantLock`），并非分布式锁，多实例部署时不同节点仍可能并发回源；`RedisKeys.lock()` / 解锁 Lua 已预留但当前业务未使用。

读取时序图（对应 6.3 三级缓存）：

```mermaid
sequenceDiagram
    autonumber
    participant S as Service（Train / Station / Seat）
    participant M as MultiLevelCacheService
    participant L1 as L1 Caffeine
    participant L2 as L2 Redis
    participant DB as MySQL

    S->>M: get(key, type, dbLoader, ttl)
    M->>L1: get(key)
    alt L1 命中
        L1-->>M: value
        M-->>S: 返回（微秒级，无网络开销）
    end

    M->>L2: GET key
    alt L2 命中且为占位符 __NULL__
        L2-->>M: __NULL__
        M-->>S: 返回 null（防穿透，不回源）
    else L2 命中有效值
        L2-->>M: json
        M->>L1: put(key, value)（回填 L1）
        M-->>S: 返回（毫秒级）
    end

    Note over M,DB: L1 / L2 均 miss → 回源（本地锁防击穿）
    M->>M: ReentrantLock.lock() + 双重检查 L1/L2
    alt 双重检查命中
        M-->>S: 直接返回
    else 仍未命中
        M->>DB: dbLoader.get()（唯一一个线程回源）
        alt DB 返回空
            DB-->>M: null
            M->>L2: SET key = __NULL__，60s
            M-->>S: 返回 null
        else DB 有数据
            DB-->>M: value
            M->>L2: SET key，TTL = ttl + random(0~20%*ttl)
            M->>L1: put(key, value)
            M-->>S: 返回
        end
    end
    M->>M: unlock() 并移除本地锁

    Note over S,DB: 写操作：MultiLevelCacheService.evict(key) 双删 L1 + L2
```

### 6.4 秒杀抢票流程

1. **限流**：`INCR qp:limit:user:{userId}`，超过 `seckill.user-limit`（默认 20 次/分钟）→ `429`
2. **限购额度**：`INCR qp:seckill:user:{trainId}:{userId}`（计数器，30min，**不分席别**），已购张数 + 本次张数 > 9 → `3006 每车次最多购买 9 张`（失败 / 取消 / 退票时 `DECR` 归还额度）
   - **DB 兜底**：Redis 标记 30min 过期后同步层就失去限购能力，因此紧接着再查一次
     `t_seckill_record(train_id, seat_type, user_id)`，命中抛 `3003`（取消/退票会删除该记录，故取消后可重买）
3. **车次校验**：三级缓存取车次，判断是否在售；不可售则回滚步骤 2 的标记
3.1 **限购规则（以订单表为准，`PurchaseLimitService`）**：
   - **每人每天每车次限购 1 张**：`t_order` 按 `(train_id, user_id)` 统计有效订单（0 待支付 / 1 已支付），已有则 `3006`；
     `train_id` 本身即「某一天的实际班次」（`uk_train_no_date`），故天然满足「每天每车次」
   - **行程运行时间内不可重复购票**：已购有效订单的运行区间 `[发车时刻, 到达时刻]` 与目标车次区间重叠 → `3007`，
     必须等已购车次到达（下车）后才能再买；跨天到达用 `arrive_time <= depart_time` 判定并 +1 天
   - 异步落库（`createSeckillOrder`）开头再校验一次，作为并发下的最后兜底，失败进补偿流程并把原因回写给前端
4. **Lua 原子预扣库存**：`qp:seckill:stock:{trainId}:{seatType}`

   ```lua
   local stock = redis.call('get', KEYS[1])
   if (stock == false) then return -1 end      -- 未初始化
   if (tonumber(stock) <= 0) then return -2 end -- 库存不足
   return redis.call('decr', KEYS[1])           -- 扣减后剩余
   ```

   返回 `-1` 会自动预热一次再重试
5. **写结果 + 投递异步任务**：生成订单号写入 `qp:seckill:result:...`，立即返回 `QUEUEING`，由 `seckillExecutor` 线程池异步落库（core 20 / max 100 / queue 5000）
6. **异步落库**：`INSERT IGNORE t_seckill_record`（唯一索引保证幂等）→ 乐观锁扣 `t_train_stock`（重试 3 次，防超卖最后防线）→ 乐观锁占座 `t_seat` → 建待支付订单（5 分钟过期）→ 失效缓存；**任一环节失败则补偿回滚**（Redis 库存 `INCR` + 删除一人一单标记 + **把失败原因写回结果 key，格式为 `FAIL:<原因>`**）

**结果获取（SSE 优先 + 轮询兜底）**：

- 优先 `GET /api/seckill/stream?batchNo=&token=`（SSE 长连接）：订阅后毫秒级收到 `connected` → `seckill-result`（车厢座位号），不必空转轮询
- 浏览器不支持 EventSource / 连接断开 / 订阅失败时，前端自动降级为 `GET /api/seckill/result` 轮询（1~2 秒一次）
- 结果值：`-1` 失败或售罄、`0` 排队中、`1` 成功（返回车厢座位号）；一次买多张用 `GET /api/seckill/batch/result?batchNo=`
- SSE 是长连接，无法带请求头，`token` 与 `traceId` 走 query 参数（`traceId` 与抢票请求共用，便于串链路）

> ⚠️ **异步线程池里的异常不会走 `GlobalExceptionHandler`**
> `@RestControllerAdvice` 只拦截 DispatcherServlet 线程内的异常。异步落库跑在 `seckillExecutor` 中，
> 异常被 `asyncCreateOrder` 吞掉并执行补偿，因此失败原因必须通过 Redis 结果 key（`FAIL:` 前缀）回传，
> 前端轮询读到 `-1` 时会展示具体原因（如「请勿重复抢票」「余票不足」），而不是一直停在「排队中」。

秒杀抢票完整时序图：

```mermaid
sequenceDiagram
    autonumber
    actor U as 用户
    participant FE as 前端 Vue
    participant PX as Nginx / Vite 代理
    participant FLT as JWT 过滤器
    participant SKC as SeckillController
    participant SKS as SeckillService
    participant RDS as Redis
    participant POOL as seckillExecutor
    participant ORS as OrderService
    participant DB as MySQL

    Note over U,DB: 阶段一：同步抢票（毫秒级返回，削峰入口）
    U->>FE: 选择车次/席别/乘客，点击抢票
    FE->>PX: POST /api/seckill/do（Authorization: Bearer token）
    PX->>FLT: 转发至 Tomcat 上下文
    FLT->>FLT: 校验 JWT → 写入 UserContext
    FLT->>SKC: 放行（其余接口需登录）
    SKC->>SKS: seckill(userId, dto, ip)

    SKS->>RDS: INCR qp:limit:user:{uid}（首次设 60s）
    alt 超过 userLimit 次/分钟
        SKS-->>FE: 429 请求过于频繁
    end

    SKS->>RDS: SETNX qp:seckill:user:{tid}:{type}:{uid}（30min）
    alt 标记已存在
        SKS-->>FE: 3003 请勿重复抢票
    end

    SKS->>SKS: 三级缓存取车次，校验 onSale
    alt 车次不可售
        SKS->>RDS: DEL 一人一单标记
        SKS-->>FE: 2002 该车次暂不可售
    end

    SKS->>RDS: EVAL Lua 预扣 qp:seckill:stock:{tid}:{type}
    alt 返回 -1 库存未初始化
        SKS->>DB: 查询 t_train_stock
        SKS->>RDS: SET 库存（预热 12h）后重试 Lua
    end
    alt 返回 -2 库存不足
        SKS->>RDS: DEL 一人一单标记
        SKS-->>FE: 2006 余票不足
    end

    SKS->>RDS: SET qp:seckill:result:{tid}:{type}:{uid} = orderNo（30min）
    SKS->>POOL: submit(asyncCreateOrder)
    SKS-->>FE: 200 {status: QUEUEING, orderNo}

    Note over POOL,DB: 阶段二：异步落库（事务，失败即补偿）
    POOL->>ORS: createSeckillOrder(taskBO)
    ORS->>DB: INSERT IGNORE t_seckill_record（唯一索引幂等）
    ORS->>DB: 乐观锁扣减 t_train_stock（重试 3 次）
    ORS->>DB: 锁座 t_seat（状态 2 锁定 + 占用订单号）
    ORS->>DB: INSERT t_order（待支付，5 分钟过期）
    ORS->>RDS: evict 车次/座位缓存（L1+L2 双删）
    alt 任一环节失败
        ORS-->>POOL: 抛异常，事务回滚
        POOL->>RDS: INCR 回滚 Redis 库存
        POOL->>RDS: DEL 一人一单标记 + 结果 key
        Note over POOL,RDS: 补偿再失败 → 日志记录，需人工介入
    end

    Note over FE,DB: 阶段三：结果获取（SSE 推送优先，轮询兜底）
    loop 每 1~2 秒轮询（SSE 不可用时）
        FE->>PX: GET /api/seckill/result?trainId&seatType
        PX->>SKC: 转发
        SKC->>SKS: queryResult(...)
        SKS->>RDS: GET qp:seckill:result:...
        alt Redis 无记录
            SKS->>DB: 兜底查 t_seckill_record（按 user+train）
        end
        alt 无记录
            SKS-->>FE: status = -1 失败 / 售罄
        else 订单尚未落库
            SKS-->>FE: status = 0 排队中
        else 订单已生成
            SKS-->>FE: status = 1 抢票成功（车厢号 + 座位号）
        end
    end
```

### 6.5 安全与鉴权

- **无状态**：禁用 Session / CSRF / formLogin / httpBasic，JWT 携带
- **Token 头**：`Authorization: Bearer <token>`（也兼容 `?token=` query 参数），有效期默认 7200 秒
- **免登录路径**：

  ```
  /api/auth/**  /api/trains/**  /api/stations/**  /api/health/**
  /doc.html  /swagger-resources/**  /v2/api-docs  /webjars/**  /druid/**  /error/**
  ```

  其余请求（`orders`、`seats`、`seckill`）必须登录；未登录返回 `{"code":401,"message":"未登录或登录已过期"}`，前端会自动清理 token 并跳转 `/login`
- **管理员**：`@PreAuthorize("hasRole('ADMIN')")` 保护库存预热接口
- **登出 / 强制下线（token 黑名单）**：JWT 签发后无法单独作废，用 Redis 黑名单补上这个能力

  | 项 | 说明 |
  | --- | --- |
  | 登出 | `POST /api/auth/logout` → `qp:token:blacklist:{jti}`，**TTL = token 剩余有效期**（过期自动清理，不留垃圾） |
  | 多端 | `qp:token:user:{userId}`（Set）记录该用户当前有效 token 的 jti，支持多端登录 |
  | 踢下线 | `POST /api/admin/users/{id}/kick` 或后台「强制下线」按钮 → 批量拉黑，对方下一次请求即 401 |
  | 校验 | `JwtAuthenticationFilter` 每次认证后查一次黑名单，命中即不放行（视为未登录） |
  | 封号 | 后台改用户状态为「已封禁」时同步踢下线，不用等 token 自然过期 |
  | 降级 | Redis 故障时黑名单降级为「放行」：可用性优先，不因缓存抖动导致全站掉线 |

  > 改造前签发的老 token 没有 `jti`，无法精准失效，重新登录一次即可获得带 jti 的新 token。
  
  ### 6.6 定时任务
  
- `StockPreheatTask`：启动后预热 / 校准 Redis 秒杀库存
- `OrderTimeoutTask`：扫描超时未支付订单并关闭（归还座位与库存）
- `TrainScheduleTask`：**按日期生成班次**（车次模板 → 每日实际班次），启动 10 秒后 + 每小时执行。
  以「每个车次号最早且已配置库存/座位的记录」为模板，为今天起 `ticket.query-days`（默认 30）天批量复制班次
  （含各席别库存与座位图，余量重置为总量），保证**每天都有车次可查询、可发车、可售卖**
  - 幂等依赖 `uk_train_no_date`：已存在该日期班次则跳过，重复执行无副作用
  - 生成后失效车次列表 / 详情缓存
  - 旧方案 `TrainDateRollTask`（把已发车车次日期整体滚动到明天）已废弃删除：它会让历史订单的车次日期被改写，
    且同一时刻只存在一天的班次，导致「今天查不到票、明天之后也没有票」

### 6.7 售票规则与时间窗

规则集中在 `TrainServiceImpl`，`SeckillServiceImpl` 复用同一套判定（`assertTicketSellable`）：

| 规则 | 配置项 | 位点 | 说明 |
| --- | --- | --- | --- |
| 可查询范围 | `ticket.query-days=30` | `TrainServiceImpl.query` | 日期超出「今天 ~ 今天+29 天」直接抛 `TRAIN_QUERY_DATE_INVALID`；不传日期默认查当天 |
| 只查未发车车次 | — | `TrainMapper` 的 `Not_Departed_Condition` | `depart_date > CURDATE() OR (depart_date = CURDATE() AND depart_time > NOW())`，列表与 count 同条件 |
| 预售期 | `ticket.presale-days=14` | 秒杀校验 | 发车日期超过「今天+14 天」返回 `TICKET_NOT_ON_SALE` |
| 开车前停售 | `ticket.stop-sell-minutes=20` | 秒杀校验 | 距发车不足 20 分钟返回 `TICKET_STOP_SELL` |
| 禁止购买已发车 | — | 秒杀校验 | 发车时刻已过返回 `TRAIN_DEPARTED`；同时释放已占的限购标记，避免用户改期重试被误判为重复抢票 |
| 每车次最多购买 9 张 | — | `PurchaseLimitService` | `t_order` 按 `(train_id, user_id)` 统计有效订单（待支付/已支付），已购 + 本次 > 9 则 `3006`，不分席别、可多乘客 |
| 行程运行时间内不可再买 | — | `PurchaseLimitService` | 已购有效订单区间 `[发车, 到达]` 与目标区间重叠则 `3007`，需等下车（到达）后才能再买 |
| 购票资格预检 | `GET /api/trains/{trainId}/buy-block` | `TrainServiceImpl.buyBlockReason` | 前端进详情页即提示原因并禁用抢票按钮，避免无意义请求 |

列表出参 `TrainVO` 额外返回 `sellable` 与 `sellTip`（「已发车 / 预售期尚未开始 / 开车前 20 分钟停止售票」），前端据此禁用抢票按钮，不必等到下单才报错。

**订单乘车日期快照**：班次按日期生成后，同一车次号会有很多天的班次记录，如果订单直接读 `t_train.depart_date`，容易与订单当时的乘车日期混淆。因此 `t_order` 增加 `depart_date` 列，下单时把当时的乘车日期写入订单；订单详情、订单列表、我的车票统一以该快照为准（老数据已按 `train_id` 回填）。

**可查询 vs 可购买**：查询范围 `ticket.query-days=30` 天（前端日期选择器同步限制），购买范围 `ticket.presale-days=14` 天。超出预售期的班次在列表里照常展示，但 `sellable=false`、`sellTip="预售期尚未开始"`。

### 6.8 区间票（同座位分段复用）与售卖时间窗

**为什么要分段**：原模型是「一个座位从始发独占到终到」，中途上下车的区间无法复用座位，运力白白浪费。
改造后按 **相邻站单段** 管理库存，同一个座位的不同区间可以卖给不同乘客（如 A→B 与 B→C 各卖一张，坐的是同一个座位）。

| 概念 | 说明 |
| --- | --- |
| 段（segment） | 相邻两站之间为一段，N 个经停站 = N−1 段；库存存于 `t_train_segment_stock(train_id, seat_type, seg_index)` |
| 区间占用 | `t_seat_segment(seat_id, from_order, to_order)`：座位只在被占用的区间内不可售 |
| 区间余票 | 某 OD 区间余票 = 该区间覆盖的**所有单段余票的最小值**（木桶效应，任一段没票就买不了） |
| 区间座位图 | 按乘车区间展示，只有「本区间被占」的座位置灰；该座位其它区间被占不影响本次购买 |
| 秒杀扣减 | Redis **多段 Lua 原子扣减**：覆盖的每一段都够票才成交，一段不足则整批失败且不部分扣减 |
| 退票 / 取消 | 该区间覆盖的每一段都归还；座位在最后一个区间被释放后才置回可售 |
| 降级 | 未建表 / 车次无经停站时自动退化为「全程票」，与改造前行为完全一致（无需改代码开关） |

启用步骤：

```bash
mysql -uroot -p qiangpiao < scripts/sql/segment_ticket.sql
```

脚本内容：`t_train_segment_stock` / `t_seat_segment` 两张表 + `t_order` 加 `from_stop_order` / `to_stop_order`
+ `t_train` 加 `sale_start_time` / `sale_end_time` + 为缺经停站的车次补齐首 / 尾两站。

然后后台 → 车次管理 → 该车次「**区间库存**」初始化（生成 N−1 段并预热到 Redis）。
未初始化的车次按全程票售卖，不受影响。

**接口侧用法**：`GET /api/trains/{trainId}?from=北京南&to=德州东` —— 余票与座位图都按该区间计算；
下单时订单会快照实际上下车站序与站名，不再是始发 / 终到。

**售卖时间窗**：`t_train.sale_start_time` / `sale_end_time`，后台车次行「售卖窗口」设置（两栏留空 = 不限时）。
窗口外禁止抢票：未开始 `4018`，已结束 `4019`；列表与详情会给出不可购提示。

> **区间票暂不支持改签**（跨多段的改签会牵动多段库存与差价），提示用户「先退票后重新购票」（`4017`）。

### 6.9 日志（Logback）

配置文件：`qiangpiao-backend/src/main/resources/logback.xml`（可通过 `-Dlogback.configurationFile=...` 覆盖）。

| 输出 | 文件 | 级别 / 说明 |
| --- | --- | --- |
| 控制台 | — | `CONSOLE_LEVEL` 控制，默认 DEBUG；生产加 `-DCONSOLE_LEVEL=INFO` 降级 |
| 全量镜像 | `D:\rizi1\qiangpiao\DEBUG.log` | FileAppender，`append=false` → **每次重启清空重写**，输出 DEBUG 及以上，与控制台同内容 |
| 分类 | `info-yyyy-MM-dd.log` / `warn-yyyy-MM-dd.log` / `error-yyyy-MM-dd.log` | `LevelFilter` 精准隔离，只收对应级别，互不交叉；按天滚动 + 单文件 100MB 再切分（`.0`/`.1`），保留 30 天 |
| 慢 SQL | `D:\rizi1\qiangpiao\SQL\slow-sql-yyyy-MM-dd.log` | MyBatis 拦截器（logger `SLOW_SQL`）+ Druid `StatFilter`，耗时 > `sql.slow-threshold-ms`（默认 100ms） |
| 秒杀 | `seckill-yyyy-MM-dd.log` | 秒杀业务单独一份，便于排查超卖 / 重复下单 |

要点：

- **异步**：所有文件输出经 `AsyncAppender`（队列 4096，`discardingThreshold=0` 不丢 WARN/ERROR），业务线程只写队列；`DelayingShutdownHook` 保证退出前刷盘
- **不重复**：慢 SQL / Druid 慢日志 `additivity=false`；同一个 appender 不会被多个 logger 重复引用
- **全链路 traceId**：`TraceIdInterceptor` 在请求入口生成 ID 写入 MDC，logback PATTERN 输出 `[%X{traceId}] [%X{orderNo}] [%X{userId}]`；
  登录用户在 `JwtAuthenticationFilter` 写入 `userId`，下单后 `TraceContext.putOrder()` 写入订单号 —— 一次请求的日志可直接按 traceId 全量捞出
- **异步线程不丢 traceId**：`AsyncConfig` 的两个线程池都挂了 `MdcTaskDecorator`，秒杀异步落库日志与发起请求同 traceId（否则请求线程与 `seckill-N` 线程对不上账）
- **慢请求**：请求结束日志带 `cost=`，超过 1s 自动升级为 WARN，直接进 `warn-*.log`
- **根 logger**：`DEBUG`（业务包 `com.qiangpiao` 为 DEBUG，Spring / MyBatis / Druid / Lettuce / logback 自身限制为 WARN，避免 DEBUG 风暴）
- **慢 SQL 拦截**：`SlowSqlInterceptor` 挂在 `SqlSessionFactory` 的 plugins 上，记录耗时并把 `?` 替换为真实参数，格式化失败也不影响业务
- **路径外部化**：`-DQP_LOG_HOME=...`、`-DQP_SQL_LOG_HOME=...`（用 `QP_` 前缀，避免被机器上已存在的 `LOG_HOME` 环境变量覆盖）；`-DMAX_HISTORY`、`-DMAX_FILE_SIZE` 同样可覆盖

---

## 七、接口清单

上下文前缀示例：`http://localhost:8081/qiangpiao`

### 认证 `/api/auth`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 登录，返回 JWT | 免登录 |
| POST | `/api/auth/register` | 注册，默认 ROLE_USER | 免登录 |
| GET | `/api/auth/info` | 当前登录用户 | 需登录 |
| POST | `/api/auth/logout` | 登出（token 进 Redis 黑名单，立即失效） | 需登录（token 无效也无副作用） |

### 车次 `/api/trains`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/trains` | 分页查询（三级缓存，支持中途站 OD 查询） | 免登录 |
| GET | `/api/trains/{trainId}` | 车次详情（含座位图）；**带 `from` / `to` 时余票与座位图按该乘车区间计算** | 免登录 |
| GET | `/api/trains/{trainId}/stops` | 车次时刻表（经停站与到发时刻） | 免登录 |
| GET | `/api/trains/{trainId}/buy-block` | 购票资格预检（限购 / 行程冲突原因） | 免登录 |

### 车站 `/api/stations`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/stations` | 全部车站 | 免登录 |
| GET | `/api/stations/search?keyword=` | 模糊搜索 | 免登录 |

### 座位 `/api/seats`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/seats/{trainId}/{seatType}` | 座位图（1 商务 / 2 一等 / 3 二等） | 需登录 |

### 秒杀 `/api/seckill`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/seckill/do` | 抢票（异步下单，返回排队中）；可选 `fromStation` / `toStation` 买区间票 | 需登录 |
| GET | `/api/seckill/stream` | **SSE 订阅抢票结果**（`batchNo` + `token` 走 query，毫秒级推送） | 需登录 |
| GET | `/api/seckill/result` | 轮询抢票结果（SSE 不可用时的兜底） | 需登录 |
| GET | `/api/seckill/batch/result` | 批量抢票结果（一次买多张，逐张给结果） | 需登录 |
| GET | `/api/seckill/flows` | 我的抢票流水（受理时间 / 结果 / 耗时 / 失败原因） | 需登录 |
| GET | `/api/seckill/stock` | 实时余票（未预热返回 -1） | 需登录 |
| POST | `/api/seckill/preheat/{trainId}` | 预热指定车次库存 | ADMIN |
| POST | `/api/seckill/preheat` | 全量预热库存 | ADMIN |

### 订单 `/api/orders`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/orders` | 我的订单（分页） | 需登录 |
| GET | `/api/orders/{orderNo}` | 订单详情（身份证脱敏） | 需登录 |
| POST | `/api/orders/pay` | 支付（模拟） | 需登录 |
| POST | `/api/orders/{orderNo}/cancel` | 取消订单（释放座位 + 归还库存） | 需登录 |

### 我的车票 `/api/tickets`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/tickets` | 我的车票（分页）：`type=upcoming` 未开车、`type=history` 历史，支持 `pageNum` / `pageSize` | 需登录 |

分类规则（以车次 `depart_date + depart_time` 是否早于 now 判定是否发车）：

- **未开车**：`status=1 已支付` 且尚未发车，按发车时间升序（最近要坐的排在最前）
- **历史车票**：已发车的票 + `已取消 / 已退票 / 已超时` 的订单，按发车时间倒序
- 待支付订单不属于车票，需先到「我的订单」完成支付

返回字段额外给出 `ticketStatus`（1 待出行 / 2 已出行 / 3 已失效）、`ticketStatusText`、`departed`、`daysFromNow`（距发车天数，前端据此显示「今天发车 / 明天发车 / 还有 N 天发车」）。

### 管理后台 `/api/admin`（需 ROLE_ADMIN）

| 模块 | 接口 |
| --- | --- |
| 车站 | `GET/POST /admin/stations`、`PUT /admin/stations/{id}/status`（停用/启用） |
| 线路 | `GET/POST /admin/lines`、`DELETE /admin/lines/{id}`、`GET/POST /admin/lines/{lineId}/stations`（途经站与顺序） |
| 车次 | `GET /admin/trains`、`PUT /admin/trains/{id}/status`（停开某天班次）、`GET/POST /admin/trains/{id}/stops`（时刻表）、`GET/POST /admin/trains/{id}/carriages`（车厢）、`POST /admin/trains/{id}/schedule?date=`（按日期生成当日班次）、`POST /admin/trains/{id}/sale-window`（售卖时间窗）、`POST /admin/trains/{id}/segment/init`（初始化区间库存，生成 N−1 段） |
| 票价 | `PUT /admin/stocks/{id}/price`、`PUT /admin/stocks/{id}/total` |
| 订单 | `GET /admin/orders`（订单号/手机号/状态/日期筛选）、`GET /admin/orders/{orderNo}`、`GET /admin/orders/{orderNo}/logs`、`GET /admin/orders/{orderNo}/changes`、`POST /admin/orders/{orderNo}/refund`（人工退票） |
| 用户 | `GET /admin/users`、`PUT /admin/users/{id}/status`（封禁/解封，封禁会同步踢下线）、`POST /admin/users/{id}/kick`（强制下线，拉黑该用户所有 token） |
| 公告 | `GET/POST /admin/announcements`、`DELETE /admin/announcements/{id}`；前台 `GET /api/announcements` 免登录 |
| 监控 | `GET /admin/monitor/stock`（余票）、`GET /admin/monitor/locked-seats`（锁票） |
| 报表 | `GET /admin/stats`、`/daily-sales`、`/train-sales`、`/user-growth`、`GET /admin/stats/export?type=`（导出 CSV，Excel 可直接打开） |

### 退票 / 改签 `/api/after-sale`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/after-sale/refund` | 退票：限已支付且未发车，释放座位、归还库存、票款退回钱包 |
| POST | `/api/after-sale/change` | 改签：换到未发车且有余票的车次，差额多退少补 |
| GET | `/api/after-sale/{orderNo}/timeline` | 订单流转时间轴 |
| GET | `/api/after-sale/{orderNo}/changes` | 改签历史 |

**车次模板与每日班次**：`t_train` 每行是「某一天的实际班次」，`POST /admin/trains/{id}/schedule` 以指定班次为模板复制（车型、时刻、库存、座位）生成新日期班次，实现「模板 + 每日排班」分离；`TrainScheduleTask` 每小时自动为今天起 30 天补齐班次，保证每天都有车次可查可买（购买仍受 14 天预售期限制）。

### 钱包 `/api/wallet`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/wallet` | 钱包余额、累计充值 / 累计消费 | 需登录 |
| GET | `/api/wallet/flows` | 零钱流水分页（`pageNum` / `pageSize`） | 需登录 |
| POST | `/api/wallet/recharge` | 自定义金额充值（0.01 ~ 50000，最多 2 位小数） | 需登录 |

> **支付链路**：`POST /api/orders/pay` 会先扣钱包余额再置单为已支付（同一事务）。
> 余额不足返回 `5002`，前端会引导跳转到「我的钱包」充值。
> 每笔充值 / 消费都会写 `t_wallet_flow`：`type`（1 充值 / 2 消费 / 3 退款）、`title`（如 `购买 G1001 二等座`）、`detail`（如 `北京南 -> 上海虹桥 05车12A · 张三`）、`amount`（消费为负）、`balance`（变动后余额）。

### 健康检查 `/api/health`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/health` | 返回 `status=UP` 与服务器时间 | 免登录 |

---

## 八、数据库表

| #    | 表名                    | 中文名         | 当前行数  | 分类     | 一句话说明                                                   |
| ---- | ----------------------- | -------------- | --------- | -------- | ------------------------------------------------------------ |
| 1    | `t_station`             | 车站表         | 371       | 基础数据 | 全国市级车站，支持拼音简码检索                               |
| 2    | `t_line`                | 线路表         | 15        | 基础数据 | 高铁线路（京沪、京广等），起止站                             |
| 3    | `t_line_station`        | 线路途经站表   | 137       | 基础数据 | 线路与车站的中间关系，带停靠顺序                             |
| 4    | `t_announcement`        | 公告表         | 0         | 基础数据 | 停运 / 节假日 / 其他公告                                     |
| 5    | `t_train`               | 车次表         | 2,700     | 车次运力 | 90 个车次号 × 30 天，按天滚动生成                            |
| 6    | `t_train_stop`          | 车次时刻表     | 2,466     | 车次运力 | 车次途经站的到发时刻、停靠时长                               |
| 7    | `t_train_stock`         | 车次席别库存表 | 8,101     | 车次运力 | 每车次每席别的总座 / 余票 / 票价（秒杀扣减对象）             |
| 8    | `t_carriage`            | 车次车厢表     | 2,160     | 车次运力 | 车次编组：车厢号 + 席别 + 座位数                             |
| 9    | `t_seat`                | 座位表         | 2,151,634 | 车次运力 | **最大表**：每班次 800 个座位明细，选座/锁座/释放都在这      |
| 10   | `t_order`               | 订单表         | 0         | 交易     | 订单主体，自带车次快照与退票手续费字段                       |
| 11   | `t_order_log`           | 订单流转日志表 | 0         | 交易     | 订单时间轴（下单/支付/退票/改签/超时），带 traceId           |
| 12   | `t_order_change`        | 改签记录表     | 0         | 交易     | 改签历史：原/新车次、座位、差额、手续费                      |
| 13   | `t_payment`             | 支付流水表     | 4         | 交易     | 两阶段支付：发起支付建单 → 渠道异步回调                      |
| 14   | `t_seckill_record`      | 秒杀成功记录表 | 0         | 交易     | 抢票成功凭证，唯一键含乘车人证件号（支持一次买多张）         |
| 15   | `t_user`                | 用户表         | 5         | 账户     | 账号密码（BCrypt）、角色、状态                               |
| 16   | `t_wallet`              | 钱包表         | 6         | 账户     | 余额 + 累计充值/消费 + 乐观锁                                |
| 17   | `t_wallet_flow`         | 零钱流水表     | 17        | 账户     | 每笔余额变动留痕，含退款幂等键                               |
| 18   | `t_seq`                 | 号段表         | 20        | 其它     | 号段槽位（20 个），当前代码未引用，预留                      |
| 19   | `t_passenger`           | 常用乘车人表   | 0         | 账户     | 每用户最多 3 位，身份证加密存储，下单时可一键选择            |
| 20   | `t_seckill_flow`        | 抢票流水表     | 0         | 交易     | 一次抢票请求从「受理」到「成功 / 失败」的留痕（排队号 + 耗时 + 失败原因） |
| 21   | `t_seat_segment`        | 座位区间占用表 | 0         | 车次运力 | **区间票核心**：同一座位按 `[上车站, 下车站)` 分段卖给不同乘客 |
| 22   | `t_train_segment_stock` | 区间库存表     | 0         | 车次运力 | 按「相邻站单段」计数，OD 区间余票 = 覆盖各段余票的最小值     |

---

## 九、常见状态码

| code | 含义 | code | 含义 |
| --- | --- | --- | --- |
| 200 | 成功 | 2006 | 余票不足 |
| 401 | 未登录或登录已过期 | 2007 | 秒杀库存未初始化 |
| 403 | 无访问权限 | 3003 | 请勿重复抢票 |
| 429 | 请求过于频繁 | 3005 | 排队中，请稍后查询结果 |
| 1001 | 用户不存在 | 4001 | 订单不存在 |
| 1004 | 用户名或密码错误 | 4003 | 订单已超时 |
| 2002 | 该车次暂不可售 | 500 | 系统繁忙 |
| 4017 | 区间票暂不支持改签，请先退票重买 | 4018 | 该车次尚未开始售票 |
| 4019 | 该车次售票已结束 | 3006 | 每车次最多购买 9 张 |
| 5001 | 钱包不存在，请稍后重试 | 5002 | 余额不足，请到钱包充值 |
| 5003 | 金额不合法 | 5004 | 单笔充值超过 50000 元 |
| 5005 | 钱包扣款失败，请稍后重试 | | |

---

## 十、常见问题

**1. Tomcat 启动报 `一个或多个 listeners 启动失败`**

看 `logs/localhost.<日期>.log` 的最内层 `Caused by`。最常见是数据库连接失败：

```
Caused by: java.sql.SQLException: Access denied for user 'root'@'localhost'
```

因为 Druid 在 `initMethod="init"` 时会建连，失败会沿 `dataSource → sqlSessionFactory → mapper → service` 一路炸到 `ContextLoaderListener`。
修复：核对 `config.properties`（或环境变量 / `-Djdbc.password`）中的用户名密码，并确保 MySQL 已启动、`qiangpiao` 库与表已初始化。

**2. 前端页面空白 / 接口 404**

前端代理上下文与 war 实际上下文不一致。检查 `vite.config.js` 的 `BACKEND_CONTEXT` 是否与 Tomcat 部署路径相同（`/qiangpiao`、`/qiangpiao_backend_war` 或 `''`）。

**3. 调用下单 / 订单接口返回 401**

这些接口需要登录。先 `POST /api/auth/login` 拿 token，前端会自动写入 `Authorization: Bearer <token>`；或直接访问前端 `/login` 页面登录。

**4. 修改了 `config.properties` 要不要重新打包？**

推荐重新打包。临时调试可直接改 `<Tomcat>/webapps/qiangpiao/WEB-INF/classes/config.properties` 后重启 Tomcat；或用 `-Djdbc.password=xxx` / 环境变量注入。

**5. `Access denied for user 'root'@'localhost'` 但命令行 mysql 能连**

检查是否在 Docker / 不同端口，或 MySQL 8 用户使用了 `caching_sha2_password`，此时 URL 需要保留 `allowPublicKeyRetrieval=true`（项目默认已带）。

**6. 端口 8081 被占用**

改 `conf/server.xml` 的 `Connector port`，或 `netstat -ano | findstr 8081` 找到 PID 后结束进程；改端口后同步修改前端 `BACKEND_ORIGIN`。

**7. 区间票没生效：余票、座位图和以前一样**

区间票依赖两张表与经停站数据。依次确认：

1. 是否执行过 `scripts/sql/segment_ticket.sql`（建 `t_train_segment_stock` / `t_seat_segment` + 加列 + 补齐经停站）
2. 后台 → 车次管理 → 该车次是否点过「**区间库存**」初始化（应提示「已初始化 N 个区间库存段」）
3. 该车次是否有经停站（`GET /api/trains/{id}/stops`）；只有始发 / 终到两站时区间=全程，看不出差别

未满足以上任一条都会**自动退化为全程票**，功能不受影响，只是没有分段复用。

**8. 登出后旧 token 仍能调通接口**

token 黑名单依赖 JWT 里的 `jti`。改造前签发的老 token 没有该字段，无法精准失效，只能等它自然过期；
重新登录一次拿到的新 token 即可正常登出 / 被强制下线。

**9.idea老是闪退**

idea所占用内存过多  idea本身内存 tomcat启动  agent内存  这些叠加就容易导致OOM问题
