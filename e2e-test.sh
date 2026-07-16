#!/bin/sh
# WeBox 后端端到端验证脚本
BASE=http://localhost:8080/api
PASS=0; FAIL=0
check() { # $1=名称 $2=实际 $3=期望片段
  if echo "$2" | grep -q "$3"; then PASS=$((PASS+1)); echo "PASS: $1";
  else FAIL=$((FAIL+1)); echo "FAIL: $1 -- got: $(echo "$2" | head -c 200)"; fi
}

EMAIL="alice$(date +%s)@corp.com"

# 1. 注册
R=$(curl -s -X POST $BASE/auth/register -H "Content-Type: application/json" -d "{\"email\":\"$EMAIL\",\"password\":\"pass123\",\"name\":\"Alice\"}")
check "register" "$R" '"code":0'

# 2. 重复注册 → 明确报错
R=$(curl -s -X POST $BASE/auth/register -H "Content-Type: application/json" -d "{\"email\":\"$EMAIL\",\"password\":\"pass123\",\"name\":\"Alice\"}")
check "register duplicate rejected" "$R" '"code":4001'

# 3. 短密码 → 校验报错
R=$(curl -s -X POST $BASE/auth/register -H "Content-Type: application/json" -d '{"email":"x@y.com","password":"123","name":"X"}')
check "short password rejected" "$R" '"code":4000'

# 4. 错误密码登录
R=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" -d "{\"email\":\"$EMAIL\",\"password\":\"wrongpw\"}")
check "wrong password rejected" "$R" '"code":4003'

# 5. 登录拿 token
R=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" -d "{\"email\":\"$EMAIL\",\"password\":\"pass123\"}")
check "login" "$R" '"token"'
TOKEN=$(echo "$R" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
AUTH="Authorization: Bearer $TOKEN"

# 6. 菜单列表 + 分类筛选
R=$(curl -s "$BASE/menu")
check "menu list has 8 items" "$R" 'item_008'
R=$(curl -s "$BASE/menu?category=chinese")
check "menu filter chinese" "$R" 'item_007'
if echo "$R" | grep -q 'item_002'; then FAIL=$((FAIL+1)); echo "FAIL: category filter leaked salad"; else PASS=$((PASS+1)); echo "PASS: category filter excludes others"; fi

# 7. 菜品详情 + 404
R=$(curl -s "$BASE/menu/item_001")
check "menu detail" "$R" '"peanut"'
R=$(curl -s "$BASE/menu/item_999")
check "menu detail 404" "$R" '"code":404'

# 8. 无 token 访问购物车 → 401
R=$(curl -s "$BASE/cart")
check "cart without token 401" "$R" '"code":401'

# 9. 加购物车（宫保鸡丁 x1，重复加 → 数量累加）
R=$(curl -s -X POST $BASE/cart/items -H "$AUTH" -H "Content-Type: application/json" -d '{"menuItemId":"item_001","quantity":1}')
check "add to cart" "$R" '"totalQuantity":1'
R=$(curl -s -X POST $BASE/cart/items -H "$AUTH" -H "Content-Type: application/json" -d '{"menuItemId":"item_001","quantity":2}')
check "repeat add accumulates" "$R" '"totalQuantity":3'

# 10. 再加一个菜，改数量，删除
R=$(curl -s -X POST $BASE/cart/items -H "$AUTH" -H "Content-Type: application/json" -d '{"menuItemId":"item_006","quantity":1}')
check "add second item" "$R" 'item_006'
CART_ID=$(echo "$R" | sed -n 's/.*"items":\[{"id":\([0-9]*\).*/\1/p')
R=$(curl -s -X PUT $BASE/cart/items/$CART_ID -H "$AUTH" -H "Content-Type: application/json" -d '{"quantity":5}')
check "update quantity" "$R" '"quantity":5'
# 总价 = 22*5 + 35*1 = 145
check "cart total price" "$R" '"totalAmount":145.00'

# 10b. 订单确认页预览（checkout）：只预览不落库
R=$(printf '{"mealPeriod":"lunch","deliveryAddress":"3F-301 工位"}' | curl -s -X POST $BASE/checkout -H "$AUTH" -H "Content-Type: application/json" --data-binary @-)
check "checkout preview items" "$R" 'item_006'
check "checkout total amount" "$R" '"totalAmount":145.00'
check "checkout total quantity" "$R" '"totalQuantity":6'
check "checkout delivery date" "$R" '"deliveryDate"'
check "checkout meal period" "$R" '"mealPeriod":"lunch"'
check "checkout address" "$R" '"deliveryAddress":"3F-301 工位"'
# 确认页不应该清空购物车 — 后续下单照常执行应成功
R=$(curl -s "$BASE/cart" -H "$AUTH")
check "cart still intact after checkout" "$R" '"totalAmount":145.00'

# 11. 下单
R=$(printf '{"mealPeriod":"lunch","deliveryAddress":"3F-301 工位"}' | curl -s -X POST $BASE/orders/success -H "$AUTH" -H "Content-Type: application/json" --data-binary @-)
check "create order" "$R" '"status":"pending"'
check "order total snapshot" "$R" '"totalAmount":145.00'
ORDER_ID=$(echo "$R" | sed -n 's/.*"data":{"id":\([0-9]*\).*/\1/p')

# 12. 下单后购物车已清空 → 再下单报错
R=$(curl -s -X POST $BASE/orders/success -H "$AUTH" -H "Content-Type: application/json" -d '{"mealPeriod":"dinner","deliveryAddress":"X"}')
check "empty cart order rejected" "$R" '"code":4004'

# 13. 订单列表 + 详情
R=$(curl -s "$BASE/orders" -H "$AUTH")
check "order list" "$R" "\"id\":$ORDER_ID"
R=$(curl -s "$BASE/orders/$ORDER_ID" -H "$AUTH")
check "order detail has items" "$R" 'item_001'

# 14. 偏好设置：过敏原 peanut，偏好 salad，预算 20-40
R=$(curl -s -X PUT $BASE/preferences -H "$AUTH" -H "Content-Type: application/json" -d '{"allergens":["peanut"],"preferredCategories":["salad"],"spicyLevel":"none","taste":"light","budgetMin":20,"budgetMax":40}')
check "save preferences" "$R" '"spicyLevel":"none"'
R=$(curl -s "$BASE/preferences" -H "$AUTH")
check "get preferences" "$R" '"peanut"'

# 15. 菜单偏好过滤：recommend=true 时排除含 peanut 的宫保鸡丁、预算外的菜
R=$(curl -s "$BASE/menu?recommend=true" -H "$AUTH")
if echo "$R" | grep -q 'item_001'; then FAIL=$((FAIL+1)); echo "FAIL: allergen item not filtered"; else PASS=$((PASS+1)); echo "PASS: allergen filtered from recommended menu"; fi
if echo "$R" | grep -q 'item_003'; then FAIL=$((FAIL+1)); echo "FAIL: over-budget item not filtered (45 > 40)"; else PASS=$((PASS+1)); echo "PASS: budget filter works"; fi
# 偏好菜系 salad 排最前
FIRST=$(echo "$R" | sed -n 's/.*"data":\[{"id":"\([^"]*\)".*/\1/p')
if [ "$FIRST" = "item_002" ] || [ "$FIRST" = "item_006" ]; then PASS=$((PASS+1)); echo "PASS: preferred category sorted first ($FIRST)"; else FAIL=$((FAIL+1)); echo "FAIL: preferred category not first, got $FIRST"; fi

# 16. AI 推荐（规则降级路径），确认排除过敏原
R=$(printf '{"query":"想吃清淡的"}' | curl -s -X POST $BASE/recommend -H "$AUTH" -H "Content-Type: application/json" --data-binary @-)
check "recommend returns results" "$R" '"recommendations":\[{'
check "recommend source" "$R" '"source"'
if echo "$R" | grep -q 'item_001'; then FAIL=$((FAIL+1)); echo "FAIL: recommend leaked allergen item"; else PASS=$((PASS+1)); echo "PASS: recommend excludes allergens"; fi
R=$(printf '{"query":"今天想吃日料"}' | curl -s -X POST $BASE/recommend -H "$AUTH" -H "Content-Type: application/json" --data-binary @-)
check "recommend japanese" "$R" 'item_003'

echo
echo "======== RESULT: $PASS passed, $FAIL failed ========"
