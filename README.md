# EnterpriseMealOrderPlatform (WeBox)

企业员工餐食订购平台 — 后端 REST API

Spring Boot 2.7 (Java 8) + MyBatis + MySQL 8 (Docker) + JWT

## 功能范围

- 🟢 核心功能：注册/登录（JWT）、菜单浏览与分类筛选、购物车、下单与订单记录
- 🟡 加分项 A：用户偏好设置（过敏原/菜系/辣度/口味/价格区间）+ 菜单按偏好过滤与排序
- 🔵 加分项 B：AI 自然语言推荐 — 配置 `ANTHROPIC_API_KEY` 时调用 Claude API，未配置时自动降级为关键词规则引擎，两条路径都自动排除用户过敏原

## 本地启动

前置：JDK 8+、Docker Desktop（无需安装 Maven 和 MySQL）

```bash
# 1. 启动 MySQL（首次自动建表 + 灌入 8 个示例菜品）
docker compose up -d

# 2. 启动应用（首次会自动下载 Maven 与依赖）
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

# 3.（可选）启用真实 LLM 推荐
export ANTHROPIC_API_KEY=sk-ant-...

# 4. 端到端验证（30 个断言）
sh e2e-test.sh
```

服务地址：`http://localhost:8080`。数据库连接：`localhost:3306/webox`，root / `webox123`（见 docker-compose.yml）。

## API 一览

统一返回 `{"code":0,"message":"success","data":...}`；业务错误 code 非 0，未登录返回 401。
需登录的接口带请求头 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | /api/auth/register | 注册（email + 密码≥6位 + 姓名） | 公开 |
| POST | /api/auth/login | 登录，返回 `{token, user}` | 公开 |
| GET | /api/menu?category=&recommend= | 菜单列表；`category` 分类筛选；`recommend=true` 且带 token 时按偏好过滤排序 | 公开 |
| GET | /api/menu/{id} | 菜品详情（描述/价格/过敏原） | 公开 |
| GET | /api/cart | 购物车列表 + 实时总价 | JWT |
| POST | /api/cart/items | 加入购物车 `{menuItemId, quantity}`，重复加数量累加 | JWT |
| PUT | /api/cart/items/{id} | 修改数量（最小 1） | JWT |
| DELETE | /api/cart/items/{id} | 删除购物车项 | JWT |
| POST | /api/orders | 下单 `{deliveryDate?, mealPeriod, deliveryAddress}`（事务：建单+快照+清空购物车） | JWT |
| GET | /api/orders | 我的订单列表 | JWT |
| GET | /api/orders/{id} | 订单详情 | JWT |
| GET | /api/preferences | 查询偏好 | JWT |
| PUT | /api/preferences | 保存偏好（过敏原/菜系/辣度/口味/预算） | JWT |
| POST | /api/recommend | AI 推荐 `{query}`，返回 3-5 个 `{menuItem, reason}` | JWT |

## curl 快速体验

```bash
BASE=http://localhost:8080/api

# 注册 + 登录
curl -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"email":"alice@corp.com","password":"pass123","name":"Alice"}'
TOKEN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"alice@corp.com","password":"pass123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
AUTH="Authorization: Bearer $TOKEN"

# 浏览菜单 / 分类筛选
curl "$BASE/menu"
curl "$BASE/menu?category=chinese"

# 购物车 → 下单
curl -X POST $BASE/cart/items -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"menuItemId":"item_006","quantity":2}'
curl -X POST $BASE/orders -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"mealPeriod":"lunch","deliveryAddress":"3F-301"}'
curl "$BASE/orders" -H "$AUTH"

# 偏好设置（过敏原 peanut）→ 菜单推荐视图会过滤宫保鸡丁
curl -X PUT $BASE/preferences -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"allergens":["peanut"],"preferredCategories":["salad"],"budgetMin":20,"budgetMax":40}'
curl "$BASE/menu?recommend=true" -H "$AUTH"

# AI 推荐（无 API Key 时走规则引擎）
curl -X POST $BASE/recommend -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"query":"想吃清淡的"}'
```

> Windows Git Bash 提示：请求体含中文时用 `printf '...' | curl --data-binary @-` 传递，
> 直接 `-d '中文'` 会被终端按 GBK 重编码导致 400/500（e2e-test.sh 中已按此处理）。

## 项目结构

```
├── docker-compose.yml          # MySQL 8 容器
├── db/init/schema.sql          # 建表 + 菜品种子数据（容器首次启动自动执行）
├── e2e-test.sh                 # 端到端验证脚本（30 个断言）
└── src/main/
    ├── resources/
    │   ├── application.yml     # 数据源 / JWT / AI 配置
    │   └── mapper/*.xml        # MyBatis XML
    └── java/com/webox/
        ├── config/             # WebMvc(JWT拦截器/CORS)、BCrypt、RestTemplate
        ├── common/             # ApiResponse、BizException、全局异常处理
        ├── auth/               # JwtUtil、JwtInterceptor
        ├── controller/         # Auth / Menu / Cart / Order / Preference / Recommend
        ├── service/            # 业务逻辑（下单事务、偏好过滤、推荐引擎）
        ├── mapper/             # MyBatis Mapper 接口
        ├── entity/             # User / MenuItem / CartItem / Order / OrderItem / UserPreference
        └── dto/                # 请求/响应对象
```

## 设计说明

- **密码**：BCrypt 加密（仅引入 spring-security-crypto，不引全套 Security）
- **登录态**：无状态 JWT（HS256，72h 过期），刷新页面不丢失；菜单接口公开但可选解析 token 以启用偏好过滤
- **下单**：`@Transactional` 事务内完成建单、订单项价格/名称快照、清空购物车；订单表名用 `orders` 避开 SQL 关键字
- **偏好过滤**：过敏原硬过滤 + 预算区间过滤 + 偏好菜系置顶排序，`recommend=true` 开关切换"全部/推荐"
- **AI 推荐降级链**：Claude API（读 `ANTHROPIC_API_KEY`）失败或未配置 → 关键词规则引擎（清淡/高蛋白/辣/日料/实惠等），响应中 `source` 字段标识 `llm` / `rule`
