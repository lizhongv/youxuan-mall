"""商城部署后冒烟测试：从浏览、注册、下单、支付、发货、收货、评价到售后退款跑一遍完整链路。

用法：python scripts/smoke_test.py http://localhost:8090 [--admin admin] [--password admin123]
依赖：httpx（pip install httpx）。只读写演示数据和本次新注册的会员，不影响已有数据。
"""
import argparse
import random
import sys
import time
import uuid

import httpx

results: list[tuple[str, bool, str]] = []
# 测试会员的余额支付密码（非连续、非重复数字，满足弱密码规则）
PAY_PASSWORD = "258369"


class Api:
    """对 {code, msg, data} 统一响应的薄封装；code != 0 视为失败并抛出带 msg 的异常。"""

    def __init__(self, base: str):
        # trust_env=False：办公电脑常设系统代理，会把局域网请求转走
        self.http = httpx.Client(base_url=base, timeout=20, trust_env=False)
        self.token: str | None = None

    def call(self, method: str, path: str, *, idem: bool = False, **kw):
        headers = kw.pop("headers", {})
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        if idem:
            headers["X-Idempotent-Key"] = str(uuid.uuid4())
        r = self.http.request(method, "/api" + path, headers=headers, **kw)
        try:
            body = r.json()
        except ValueError:
            raise AssertionError(f"{method} {path} HTTP {r.status_code}: {r.text[:200]}")
        if body.get("code") != 0:
            raise AssertionError(f"{method} {path} code={body.get('code')} msg={body.get('msg')}")
        return body.get("data")


def step(name: str):
    """装饰器：执行一步并记录结果，失败不中断后续独立步骤。"""

    def wrap(fn):
        try:
            detail = fn() or ""
            results.append((name, True, str(detail)))
        except Exception as e:  # noqa: BLE001 冒烟测试需要汇总所有失败，而不是在第一个失败处退出
            results.append((name, False, str(e)[:300]))
        return fn

    return wrap


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("base")
    ap.add_argument("--admin", default="admin")
    ap.add_argument("--password", default="admin123")
    args = ap.parse_args()
    base = args.base.rstrip("/")

    pub = Api(base)
    admin = Api(base)
    buyer = Api(base)
    ctx: dict = {}

    @step("静态页面：商城首页与后台可访问")
    def _():
        raw = httpx.Client(base_url=base, timeout=10, trust_env=False)
        for path in ("/", "/console/", "/doc.html"):
            r = raw.get(path)
            assert r.status_code == 200 and "<html" in r.text.lower(), f"{path} -> {r.status_code}"
        return "/, /console/, /doc.html"

    @step("公开接口：商城配置、首页、类目、商品列表与详情")
    def _():
        cfg = pub.call("GET", "/app/public/config")
        home = pub.call("GET", "/app/public/home")
        assert home["banners"], "首页轮播为空"
        tree = pub.call("GET", "/app/public/category/tree")
        page = pub.call("GET", "/app/public/goods/page", params={"size": 50, "sort": "SALES"})
        assert page["total"] > 0, "没有在售商品"
        detail = None
        for g in page["records"]:
            d = pub.call("GET", f"/app/public/goods/{g['id']}")
            skus = [s for s in d["skus"] if s["stock"] >= 3]
            if skus and float(skus[0]["price"]) < 150:
                detail, ctx["sku"] = d, skus[0]
                break
        assert detail, "找不到有库存且价格适中的 SKU"
        ctx["goods"] = detail
        img = httpx.get(base + detail["pic"], timeout=10, trust_env=False)
        assert img.status_code == 200, f"商品图片 {detail['pic']} -> {img.status_code}"
        return f"{cfg['mallName']}，类目 {len(tree)}，商品 {page['total']}，测试商品《{detail['name']}》"

    @step("后台登录、个人信息与数据概览")
    def _():
        tok = admin.call("POST", "/admin/auth/login", json={"username": args.admin, "password": args.password})
        admin.token = tok["accessToken"]
        me = admin.call("GET", "/admin/auth/me")
        ov = admin.call("GET", "/admin/stats/overview")
        admin.call("GET", "/admin/stats/trend", params={"days": 7})
        return f"{me['username']} 权限 {len(me['permissions'])} 项，今日订单 {ov['todayOrders']}"

    @step("会员注册（验证码）与资料")
    def _():
        mobile = "139" + "".join(random.choices("0123456789", k=8))
        # 发短信前必须过图形验证码；demo 模式下 captcha 接口回显答案（devCode），prod 模式无法自动化
        cap = pub.call("GET", "/app/auth/captcha")
        assert cap.get("devCode"), "图形验证码未回显：冒烟测试需在 MALL_PROFILE=demo 的环境运行"
        code = pub.call("POST", "/app/auth/sms-code", json={
            "mobile": mobile, "scene": "REGISTER",
            "captchaId": cap["captchaId"], "captchaCode": cap["devCode"]})["devCode"]
        assert code, "短信验证码未回显：demo 模式需设置 MALL_SMS_DEV_MODE=true"
        ctx["smsAt"] = time.time()
        tok = buyer.call("POST", "/app/auth/register", json={"mobile": mobile, "code": code, "password": "Test1234"})
        buyer.token = tok["accessToken"]
        prof = buyer.call("GET", "/app/member/profile")
        ctx["mobile"] = mobile
        ctx["memberId"] = prof["id"]
        # 注册赠送余额默认为 0（上线安全默认值），由后台调账给测试会员充值，顺带验证调账接口
        admin.call("POST", f"/admin/member/member/{prof['id']}/adjust",
                   json={"accountType": "BALANCE", "amount": 500, "remark": "冒烟测试充值"})
        prof = buyer.call("GET", "/app/member/profile")
        assert float(prof["balance"]) >= 500, f"调账后余额 {prof['balance']}"
        return f"{mobile} 余额 {prof['balance']}"

    @step("收货地址、收藏、领券")
    def _():
        buyer.call("POST", "/app/member/address", json={
            "name": "张三", "mobile": ctx["mobile"], "province": "浙江省", "city": "杭州市",
            "district": "西湖区", "detail": "文三路 100 号", "isDefault": 1})
        ctx["address"] = buyer.call("GET", "/app/member/address/default")
        buyer.call("POST", f"/app/member/favorite/{ctx['goods']['id']}")
        coupons = buyer.call("GET", "/app/public/coupon/available")
        for c in coupons:
            buyer.call("POST", f"/app/coupon/{c['id']}/receive")
        mine = buyer.call("GET", "/app/coupon/mine", params={"status": "UNUSED"})
        return f"领到 {len(mine)} 张券"

    @step("购物车 → 结算预览 → 下单 → 余额支付")
    def _():
        buyer.call("POST", "/app/cart", json={"skuId": ctx["sku"]["id"], "quantity": 2})
        cart = buyer.call("GET", "/app/cart")
        submit = {"source": "CART", "cartIds": [c["id"] for c in cart], "addressId": ctx["address"]["id"], "remark": "冒烟测试"}
        confirm = buyer.call("POST", "/app/order/confirm", json=submit)
        submit["memberCouponId"] = confirm.get("memberCouponId")
        order = buyer.call("POST", "/app/order", json=submit, idem=True)
        assert abs(float(order["payAmount"]) - float(confirm["payAmount"])) < 0.001, "下单金额与预览不一致"
        channels = buyer.call("GET", "/app/pay/channels", params={"orderId": order["orderId"], "inWechat": False})
        assert any(c["type"] == "BALANCE" and c["enabled"] for c in channels), f"余额支付不可用：{channels}"
        # 余额支付需要支付密码：图形验证码 → 短信验证码（demo 回显）→ 设置。
        # 同一手机号 60 秒内只能发一次验证码（注册时刚发过），这里等过限频窗口，与真实用户的节奏一致
        time.sleep(max(0, 61 - (time.time() - ctx["smsAt"])))
        cap = buyer.call("GET", "/app/auth/captcha")
        sms = buyer.call("POST", "/app/member/pay-password/sms-code",
                         json={"captchaId": cap["captchaId"], "captchaCode": cap["devCode"]})
        buyer.call("PUT", "/app/member/pay-password", json={"code": sms["devCode"], "payPassword": PAY_PASSWORD})
        r = buyer.http.post(f"/api/app/order/{order['orderId']}/pay", json={"payType": "BALANCE", "payPassword": "000001"},
                            headers={"Authorization": f"Bearer {buyer.token}", "X-Idempotent-Key": str(uuid.uuid4())})
        assert r.json().get("code") == 60011, f"错误支付密码应返回 60011，实际 {r.json()}"
        buyer.call("POST", f"/app/order/{order['orderId']}/pay",
                   json={"payType": "BALANCE", "payPassword": PAY_PASSWORD}, idem=True)
        detail = buyer.call("GET", f"/app/order/{order['orderId']}")
        assert detail["status"] == "PENDING_DELIVERY", detail["status"]
        ctx["order1"] = detail
        return (f"商品 {confirm['goodsAmount']} 折扣 {confirm['discountAmount']} 券 {confirm['couponAmount']} "
                f"运费 {confirm['freightAmount']} 实付 {confirm['payAmount']}")

    @step("后台发货 → 买家确认收货 → 评价 → 后台回复")
    def _():
        oid = ctx["order1"]["id"]
        admin.call("PUT", f"/admin/trade/order/{oid}/deliver", json={"deliveryCompany": "顺丰速运", "deliveryNo": "SF1234567890"})
        logistics = buyer.call("GET", f"/app/order/{oid}/logistics")
        assert "traces" in logistics, f"物流接口出参异常：{logistics}"
        buyer.call("PUT", f"/app/order/{oid}/receive")
        item = ctx["order1"]["items"][0]
        buyer.call("POST", "/app/review", json={"orderItemId": item["id"], "rating": 5, "content": "质量很好，物流很快", "images": [], "anonymous": False})
        reviews = admin.call("GET", "/admin/trade/review/page", params={"goodsId": item["goodsId"]})
        admin.call("PUT", f"/admin/trade/review/{reviews['records'][0]['id']}/reply", json={"reply": "感谢支持！"})
        detail = buyer.call("GET", f"/app/order/{oid}")
        assert detail["status"] == "COMPLETED", detail["status"]
        prof = buyer.call("GET", "/app/member/profile")
        return f"订单完成，积分 {prof['points']}"

    @step("退货退款：退货地址 → 买家寄回 → 商家确认收货 → 退回余额并扣回积分")
    def _():
        admin.call("PUT", "/admin/system/config/batch", json={"items": [
            {"configKey": "trade.return_receiver_name", "configValue": "优选商城售后部"},
            {"configKey": "trade.return_receiver_mobile", "configValue": "400-800-8888"},
            {"configKey": "trade.return_address", "configValue": "浙江省杭州市余杭区仓前街道 1 号仓"}]})
        item = ctx["order1"]["items"][0]
        before = buyer.call("GET", "/app/member/profile")
        buyer.call("POST", "/app/aftersale", json={
            "orderItemId": item["id"], "type": "RETURN_REFUND", "reason": "质量问题", "description": "冒烟测试",
            "images": [], "amount": item["payAmount"]})
        page = admin.call("GET", "/admin/trade/aftersale/page", params={"orderNo": ctx["order1"]["orderNo"]})
        aid = page["records"][0]["id"]
        admin.call("PUT", f"/admin/trade/aftersale/{aid}/approve", json={"remark": "请寄回"})
        detail = buyer.call("GET", f"/app/aftersale/{aid}")
        assert detail["status"] == "WAIT_RETURN" and detail["returnAddress"], f"未返回退货地址：{detail}"
        buyer.call("PUT", f"/app/aftersale/{aid}/return", json={"returnCompany": "顺丰速运", "returnNo": "SF9876543210"})
        admin.call("PUT", f"/admin/trade/aftersale/{aid}/receive")
        detail = buyer.call("GET", f"/app/aftersale/{aid}")
        assert detail["status"] == "REFUNDED", detail["status"]
        after = buyer.call("GET", "/app/member/profile")
        assert float(after["balance"]) > float(before["balance"]), "退款未退回余额"
        assert int(after["points"]) < int(before["points"]), "确认收货发放的积分未按比例扣回"
        return f"余额 {before['balance']} → {after['balance']}，积分 {before['points']} → {after['points']}"

    @step("运费模板：新增 → 下拉可见 → 删除")
    def _():
        tid = admin.call("POST", "/admin/trade/freight-template", json={
            "name": "冒烟测试模板", "chargeType": "BY_COUNT", "isDefault": 0, "status": 1,
            "rules": [{"provinces": [], "firstUnit": 1, "firstFee": 8, "nextUnit": 1, "nextFee": 2},
                      {"provinces": ["新疆维吾尔自治区"], "firstUnit": 1, "firstFee": 20, "nextUnit": 1, "nextFee": 10}],
            "frees": [{"provinces": [], "minCount": 3, "minAmount": 0}]})
        options = admin.call("GET", "/admin/trade/freight-template/list")
        assert any(o["id"] == tid for o in options), "新模板不在下拉列表中"
        admin.call("DELETE", f"/admin/trade/freight-template/{tid}")
        return "增删正常"

    @step("立即购买 → 模拟支付 → 申请仅退款 → 后台同意 → 订单取消")
    def _():
        submit = {"source": "BUY_NOW", "items": [{"skuId": ctx["sku"]["id"], "quantity": 1}],
                  "addressId": ctx["address"]["id"], "memberCouponId": "0"}
        order = buyer.call("POST", "/app/order", json=submit, idem=True)
        buyer.call("POST", f"/app/order/{order['orderId']}/pay", json={"payType": "MOCK"}, idem=True)
        detail = buyer.call("GET", f"/app/order/{order['orderId']}")
        item = detail["items"][0]
        buyer.call("POST", "/app/aftersale", json={
            "orderItemId": item["id"], "type": "REFUND", "reason": "不想要了", "description": "冒烟测试",
            "images": [], "amount": item["payAmount"]})
        page = admin.call("GET", "/admin/trade/aftersale/page", params={"orderNo": detail["orderNo"]})
        admin.call("PUT", f"/admin/trade/aftersale/{page['records'][0]['id']}/approve", json={"remark": "同意退款"})
        after = buyer.call("GET", f"/app/order/{order['orderId']}")
        assert after["status"] == "CANCELLED", after["status"]
        return f"订单 {detail['orderNo']} 已退款并关闭"

    @step("下单后取消：库存回补")
    def _():
        sku_id = ctx["sku"]["id"]
        before = pub.call("GET", f"/app/public/goods/{ctx['goods']['id']}")
        stock0 = next(s["stock"] for s in before["skus"] if s["id"] == sku_id)
        order = buyer.call("POST", "/app/order", json={"source": "BUY_NOW", "items": [{"skuId": sku_id, "quantity": 1}],
                                                       "addressId": ctx["address"]["id"], "memberCouponId": "0"}, idem=True)
        buyer.call("PUT", f"/app/order/{order['orderId']}/cancel", json={"reason": "不想买了"})
        after = pub.call("GET", f"/app/public/goods/{ctx['goods']['id']}")
        stock1 = next(s["stock"] for s in after["skus"] if s["id"] == sku_id)
        assert stock0 == stock1, f"库存未回补 {stock0} -> {stock1}"
        return f"库存 {stock1}"

    @step("越权防护：会员令牌不能访问管理端，未登录不能访问会员接口")
    def _():
        r = buyer.http.get("/api/admin/stats/overview", headers={"Authorization": f"Bearer {buyer.token}"})
        assert r.status_code == 403 or r.json().get("code") == 40300, f"会员访问后台返回 {r.status_code}"
        r = pub.http.get("/api/app/member/profile")
        assert r.status_code == 401 or r.json().get("code") == 40100, f"未登录访问返回 {r.status_code}"
        return "403 / 401"

    @step("后台列表接口抽查")
    def _():
        for path in ("/admin/goods/goods/page", "/admin/trade/order/page", "/admin/member/member/page",
                     "/admin/marketing/coupon/page", "/admin/marketing/seckill/page", "/admin/system/admin/page",
                     "/admin/system/oper-log/page", "/admin/trade/order/status-count", "/admin/goods/category/tree",
                     "/admin/system/menu/tree", "/admin/stats/top-goods", "/admin/stats/order-status"):
            admin.call("GET", path)
        return "12 个接口"

    ok = sum(1 for _, passed, _ in results if passed)
    for name, passed, detail in results:
        print(f"[{'PASS' if passed else 'FAIL'}] {name}  {detail}")
    print(f"\n{ok}/{len(results)} 通过")
    sys.exit(0 if ok == len(results) else 1)


if __name__ == "__main__":
    main()
