<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import NavBar from '@/components/NavBar.vue'

/**
 * 《用户协议》《隐私政策》：通用模板，商城名称与客服电话取 /app/public/config。
 * 正式运营前请由法务按实际业务修订文案。
 */
const route = useRoute()
const app = useAppStore()
onMounted(() => app.loadConfig())

const type = computed(() => (route.params.type === 'privacy' ? 'privacy' : 'user'))
const name = computed(() => app.config.mallName || '本商城')
const phone = computed(() => app.config.servicePhone || '商城客服')
const title = computed(() => (type.value === 'privacy' ? '隐私政策' : '用户协议'))

interface Section {
  h: string
  p: string[]
}

const userSections = computed<Section[]>(() => [
  {
    h: '一、协议的接受',
    p: [
      `欢迎使用${name.value}（以下简称"本平台"）。在注册、登录或使用本平台服务前，请您仔细阅读本协议全部内容。您点击同意、完成注册或实际使用服务，即视为您已阅读、理解并同意受本协议约束。`,
    ],
  },
  {
    h: '二、账号注册与使用',
    p: [
      '1. 您应使用本人实名手机号注册，并对账号下的一切行为负责。',
      '2. 请妥善保管登录密码、支付密码及短信验证码，不得转让、出借账号。因您保管不善造成的损失由您自行承担。',
      '3. 如发现账号被盗用或存在安全漏洞，请立即联系客服。',
    ],
  },
  {
    h: '三、商品与订单',
    p: [
      '1. 商品价格、库存、配送范围以下单时页面展示为准；订单提交后请在支付时限内完成支付，超时订单将自动关闭。',
      '2. 因商品信息标注错误等明显失误，本平台有权取消相应订单并全额退款。',
      '3. 秒杀、优惠券等营销活动以活动规则为准，以不正当手段获取的优惠本平台有权取消。',
    ],
  },
  {
    h: '四、支付与售后',
    p: [
      '1. 您可使用余额、微信支付、支付宝等平台提供的方式付款，退款将原路退回。',
      '2. 售后服务按照《消费者权益保护法》及平台售后规则执行，退货商品应保持完好并附带全部配件。',
    ],
  },
  {
    h: '五、用户行为规范',
    p: [
      '您不得利用本平台从事违法违规活动，不得发布侵权、虚假、违法的评价内容，不得以技术手段干扰平台正常运行。违反上述规定的，本平台有权删除相关内容、限制或终止服务。',
    ],
  },
  {
    h: '六、责任限制',
    p: [
      '因不可抗力、网络故障、第三方服务中断等非本平台原因造成的服务中断或损失，本平台在法律允许的范围内不承担责任，但会尽力协助您减少损失。',
    ],
  },
  {
    h: '七、协议变更与争议解决',
    p: [
      '本平台可根据业务需要修订本协议，修订后的协议将在平台公示。若您继续使用服务，视为接受修订后的协议。',
      `因本协议产生的争议，双方应友好协商；协商不成的，提交${name.value}运营方所在地有管辖权的人民法院处理。如有疑问请联系：${phone.value}。`,
    ],
  },
])

const privacySections = computed<Section[]>(() => [
  {
    h: '一、我们收集的信息',
    p: [
      `${name.value}（以下简称"我们"）仅在提供服务所必需的范围内收集您的信息：`,
      '1. 账号信息：手机号、昵称、头像、登录密码（加密存储）；',
      '2. 交易信息：收货人姓名、电话、地址，订单、支付与售后记录；',
      '3. 使用信息：浏览足迹、收藏、搜索记录（搜索记录仅保存在您的设备上）；',
      '4. 微信登录时，我们仅获取您在本公众号下的 OpenID，用于登录与微信支付，不获取您的微信昵称和头像。',
    ],
  },
  {
    h: '二、信息的使用',
    p: [
      '我们将收集的信息用于：账号注册登录与安全验证、订单履约与配送、支付与退款、售后服务、向您展示个性化的商品推荐，以及依法履行监管义务。',
    ],
  },
  {
    h: '三、信息的共享',
    p: [
      '为完成订单，我们会向物流公司提供收货人信息，向支付机构提供必要的支付信息。除法律法规要求或获得您的明确同意外，我们不会向其他第三方出售或提供您的个人信息。',
    ],
  },
  {
    h: '四、信息的存储与保护',
    p: [
      '您的信息存储于中华人民共和国境内的服务器。我们采用加密传输、密码加密存储、访问权限控制等措施保护您的信息安全，手机号在页面中脱敏展示。',
    ],
  },
  {
    h: '五、您的权利',
    p: [
      '您可以在「我的」中查看和修改个人资料、收货地址，清空浏览足迹，修改登录密码与支付密码。如需注销账号或删除个人信息，请联系客服，我们将在 15 个工作日内处理。',
    ],
  },
  {
    h: '六、未成年人保护',
    p: ['若您是未满 18 周岁的未成年人，请在监护人的陪同和同意下使用本平台服务。'],
  },
  {
    h: '七、政策更新与联系我们',
    p: [`本政策更新后将在平台公示。如您对本政策有任何疑问，请联系：${phone.value}。`],
  },
])

const sections = computed(() => (type.value === 'privacy' ? privacySections.value : userSections.value))
</script>

<template>
  <div class="page agreement">
    <NavBar :title="title" />
    <article class="agreement__body">
      <h1 class="agreement__title">{{ name }}{{ title }}</h1>
      <p class="agreement__meta">更新日期：2026 年 10 月 31 日</p>
      <section v-for="s in sections" :key="s.h">
        <h2 class="agreement__h">{{ s.h }}</h2>
        <p v-for="(t, i) in s.p" :key="i" class="agreement__p">{{ t }}</p>
      </section>
    </article>
  </div>
</template>

<style scoped lang="scss">
.agreement {
  background: $card;
  &__body {
    padding: 16px 18px calc(32px + env(safe-area-inset-bottom));
  }
  &__title {
    margin: 0;
    font-size: 19px;
    font-weight: 700;
    text-align: center;
  }
  &__meta {
    margin: 8px 0 12px;
    text-align: center;
    font-size: 12px;
    color: $text-3;
  }
  &__h {
    margin: 18px 0 8px;
    font-size: 15px;
    font-weight: 600;
  }
  &__p {
    margin: 0 0 6px;
    font-size: 14px;
    line-height: 1.75;
    color: $text-2;
  }
}
</style>
