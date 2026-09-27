# 火车票秒杀抢票系统

基于 **SSM（Spring + SpringMVC + MyBatis）** 的火车票购票 / 秒杀演示项目，**前后端分离**，分为两个模块：

| 模块 | 说明 | 打包方式 |
| --- | --- | --- |
| `qiangpiao-backend` | 后端 SSM 服务，纯注解配置，**打 war 包部署到 Tomcat** | `war`（`finalName=qiangpiao`，上下文 `/qiangpiao`） |
| `qiangpiao-frontend` | Vue3 + Vite 前端，独立运行 / Nginx 部署 | 静态资源 `dist` |

实现功能：车次查询、余票与座位图、车站字典、**JWT 登录鉴权**、**Redis Lua 原子预扣库存秒杀抢票**、异步下单、**钱包余额支付 / 零钱流水 / 自定义充值**、订单取消、超时关单、**L1/L2/L3 三级缓存**、接口文档（Knife4j）、Druid 监控。

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
│     │  ├─ service/ + service/impl/   # 业务层，Service 之间传递 BO
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
│     │  ├─ logback.xml
│     │  ├─ mapper/*.xml         # 7 个 MyBatis 映射文件
│     │  └─ sql/schema.sql、sql/data.sql
│     └─ webapp/WEB-INF/web.xml  # ContextLoaderListener + DispatcherServlet + Security/Druid 过滤器
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
```

`data.sql` 会写入：10 个车站、3 个用户、6 个车次、三档席别库存，并按真实座位数校准库存。

### 2. 修改后端配置

编辑 `qiangpiao-backend/src/main/resources/config.properties`：

```properties
jdbc.url=jdbc:mysql://127.0.0.1:3306/qiangpiao?...
jdbc.username=root
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
# war 名为 qiangpiao.war → 上下文路径为 /qiangpiao
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
| `http://localhost:8081/qiangpiao/doc.html` | Knife4j 接口文档 |
| `http://localhost:8081/qiangpiao/druid/index.html` | Druid 监控（admin / admin123） |
| `http://localhost:5174/trains` | 车次列表（免登录即可查看） |
| `http://localhost:5174/wallet` | 我的钱包：余额 / 充值 / 零钱流水（需登录） |
| `http://localhost:5174/tickets` | 我的车票：未开车（待出行）/ 历史车票（需登录） |

---

## 五、默认账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| `admin` | `123456` | ROLE_ADMIN | 重导 `data.sql` 后生效 |
| `zhangsan` / `lisi` | `123456` | ROLE_USER | 同上 |

> 密码使用 **BCrypt** 存储。BCrypt 每次加密带随机盐，`data.sql` 中保存的是一条与 `123456` 匹配的合法密文；如果重新生成，`密文不同但同样能校验通过`。
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
{ "code": 200, "message": "操作成功", "data": {}, "traceId": null, "timestamp": 1790181657341 }
```

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
| 一人一单 | `qp:seckill:user:{trainId}:{seatType}:{userId}` | 30min |
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
2. **一人一单**：`SETNX qp:seckill:user:{trainId}:{seatType}:{userId}`（30min），失败 → `3003 请勿重复抢票`
   - **DB 兜底**：Redis 标记 30min 过期后同步层就失去限购能力，因此紧接着再查一次
     `t_seckill_record(train_id, seat_type, user_id)`，命中同样抛 `3003`（取消/退票会删除该记录，故取消后可重买）
3. **车次校验**：三级缓存取车次，判断是否在售；不可售则回滚步骤 2 的标记
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

前端通过 `GET /api/seckill/result` 轮询结果：`-1` 失败/售罄、`0` 排队中、`1` 成功（返回车厢座位号）。

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

    Note over FE,DB: 阶段三：前端轮询结果
    loop 每 1~2 秒轮询
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

### 6.6 定时任务

- `StockPreheatTask`：启动后预热 / 校准 Redis 秒杀库存
- `OrderTimeoutTask`：扫描超时未支付订单并关闭（归还座位与库存）
- `TrainDateRollTask`：**车次日期滚动**，启动 5 秒后 + 每小时执行，把已发车的车次挪到它的下一班次
  （今天该时刻还没过就保持在今天，否则顺延到明天），保证任何时刻都能查到未发车车次

### 6.7 售票规则与时间窗

规则集中在 `TrainServiceImpl`，`SeckillServiceImpl` 复用同一套判定（`assertTicketSellable`）：

| 规则 | 配置项 | 位点 | 说明 |
| --- | --- | --- | --- |
| 可查询范围 | `ticket.query-days=30` | `TrainServiceImpl.query` | 日期超出「今天 ~ 今天+29 天」直接抛 `TRAIN_QUERY_DATE_INVALID`；不传日期默认查当天 |
| 只查未发车车次 | — | `TrainMapper` 的 `Not_Departed_Condition` | `depart_date > CURDATE() OR (depart_date = CURDATE() AND depart_time > NOW())`，列表与 count 同条件 |
| 预售期 | `ticket.presale-days=14` | 秒杀校验 | 发车日期超过「今天+14 天」返回 `TICKET_NOT_ON_SALE` |
| 开车前停售 | `ticket.stop-sell-minutes=20` | 秒杀校验 | 距发车不足 20 分钟返回 `TICKET_STOP_SELL` |
| 禁止购买已发车 | — | 秒杀校验 | 发车时刻已过返回 `TRAIN_DEPARTED`；同时释放已占的一人一单标记，避免用户改期重试被误判为重复抢票 |

列表出参 `TrainVO` 额外返回 `sellable` 与 `sellTip`（「已发车 / 预售期尚未开始 / 开车前 20 分钟停止售票」），前端据此禁用抢票按钮，不必等到下单才报错。

**订单乘车日期快照**：车次日期会被 `TrainDateRollTask` 滚动，如果订单直接读 `t_train.depart_date`，历史订单的日期会被改写。因此 `t_order` 增加 `depart_date` 列，下单时把当时的乘车日期写入订单；订单详情、订单列表、我的车票统一以该快照为准（老数据已按 `train_id` 回填）。

---

## 七、接口清单

上下文前缀示例：`http://localhost:8081/qiangpiao`

### 认证 `/api/auth`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 登录，返回 JWT | 免登录 |
| POST | `/api/auth/register` | 注册，默认 ROLE_USER | 免登录 |
| GET | `/api/auth/info` | 当前登录用户 | 需登录 |

### 车次 `/api/trains`

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| GET | `/api/trains` | 分页查询（三级缓存） | 免登录 |
| GET | `/api/trains/{trainId}` | 车次详情（含座位图） | 免登录 |

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
| POST | `/api/seckill/do` | 抢票（异步下单，返回排队中） | 需登录 |
| GET | `/api/seckill/result` | 轮询抢票结果 | 需登录 |
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

| 表 | 说明 |
| --- | --- |
| `t_user` | 用户：账号、BCrypt 密码、真实姓名、手机号、身份证、角色、状态 |
| `t_station` | 车站：站名、城市、拼音简码 |
| `t_train` | 车次：车次号、车型、起终点、发车日期/时间、到达、历时、在售状态 |
| `t_train_stock` | 车次席别库存：总座位、剩余可售、票价、`version` 乐观锁 |
| `t_seat` | 座位：车厢号、座位号、状态（0 可售 / 1 已售 / 2 锁定）、占用订单号、乐观锁 |
| `t_order` | 订单：订单号、用户、车次、座位、乘客、票价、状态、支付/取消/超时时间 |
| `t_seckill_record` | 秒杀记录：唯一索引 `(train_id, seat_type, user_id)`，保证一人一单 |
| `t_wallet` | 钱包：用户余额、累计充值、累计消费、`version` 乐观锁，唯一索引 `(user_id)` |
| `t_wallet_flow` | 零钱流水：流水号、业务单号、类型（1 充值 / 2 消费 / 3 退款）、摘要、明细、变动金额、变动后余额 |

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
