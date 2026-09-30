<template>
  <div class="page-container">
    <!-- ============ Hero ============ -->
    <div class="hero">
      <div class="hero-main">
        <div class="hero-badge">12306 业务规则 · 全流程闭环</div>
        <h1 class="hero-title">抢票系统</h1>
        <p class="hero-sub">
          一套对齐 12306 售票规则的火车票在线购票 / 秒杀示例系统。
          查票、抢票、支付、出票、退票、改签、退款全链路打通，支持高并发秒杀与库存强一致。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="go('/trains')">开始查票</el-button>
          <el-button size="large" @click="openGithub">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/>
            </svg>
            GitHub
          </el-button>
        </div>
      </div>
      <div class="hero-stats">
        <div v-for="s in stats" :key="s.label" class="stat-item">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 网站作用 ============ -->
    <div class="card-panel">
      <h3 class="section-title">这个网站是做什么的</h3>
      <p class="section-desc">
        模拟真实铁路售票场景：车次按天滚动开售、区间票共用座位库存、开车前 20 分钟停售、14 天预售期；
        下单走秒杀链路保证不超卖，支付支持钱包余额扣款，售后完整实现退票阶梯手续费与改签手续费规则。
      </p>
      <div class="feature-grid">
        <div v-for="f in features" :key="f.title" class="feature-card">
          <div class="feature-title">{{ f.title }}</div>
          <div class="feature-desc">{{ f.desc }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 如何使用 ============ -->
    <div class="card-panel">
      <h3 class="section-title">如何使用</h3>
      <el-steps :active="steps.length" finish-status="success" align-center class="use-steps">
        <el-step v-for="s in steps" :key="s.title" :title="s.title" :description="s.desc"/>
      </el-steps>
      <el-divider content-position="left">购票之后</el-divider>
      <ul class="tips">
        <li><b>我的车票</b>：查看未开车车票与历史行程记录。</li>
        <li><b>退票 / 改签</b>：按距开车时间阶梯计费，改签费按新旧票较低票价计算，一张车票只能改签一次。</li>
        <li><b>我的钱包</b>：查看余额与零钱流水，余额不足可自助充值。</li>
        <li><b>管理后台</b>（管理员）：车站 / 线路 / 车次 / 票价库存 / 订单 / 用户 / 公告 / 票务监控。</li>
      </ul>
    </div>

    <!-- ============ 下载安装包 ============ -->
    <div class="card-panel">
      <h3 class="section-title">下载安装包</h3>
      <p class="section-desc">
        移动端与 PC 端使用同一套账号与数据，订单、钱包、车票实时同步。
      </p>
      <div class="download-grid">
        <div v-for="d in downloads" :key="d.key" class="download-card">
          <div class="dl-head">
            <div class="dl-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <template v-if="d.key === 'android'">
                  <rect x="7" y="2" width="10" height="20" rx="2"/>
                  <path d="M11 18.5h2"/>
                </template>
                <template v-else>
                  <rect x="2" y="4" width="20" height="12" rx="2"/>
                  <path d="M8 20h8M12 16v4"/>
                </template>
              </svg>
            </div>
            <div>
              <div class="dl-platform">{{ d.platform }}</div>
              <div class="dl-name">{{ d.name }}</div>
            </div>
          </div>
          <div class="dl-desc">{{ d.desc }}</div>
          <div class="dl-meta">
            <span>版本 {{ d.version }}</span>
            <span>{{ d.size }}</span>
            <span>{{ d.require }}</span>
          </div>
          <div class="dl-actions">
            <el-button type="primary" @click="open(d.url)">立即下载</el-button>
            <el-button text @click="open(GITHUB_RELEASES)">历史版本</el-button>
          </div>
        </div>
      </div>
      <div class="dl-more">
        <span class="muted">其他资源：</span>
        <el-button text type="primary" @click="open(GITHUB_RELEASES)">服务端 war 部署包</el-button>
        <el-button text type="primary" @click="open(GITHUB_ZIP)">源码 ZIP</el-button>
        <span class="muted">下载链接来自 GitHub Releases，未发布版本时会跳转失败，请到 Releases 页获取。</span>
      </div>
    </div>

    <!-- ============ GitHub ============ -->
    <div class="card-panel github-card">
      <div>
        <h3 class="section-title">开源仓库</h3>
        <p class="section-desc">
          后端 SSM + Redis 三级缓存，前端 Vue3 + Vite + Element Plus，完整源码与建表脚本都在仓库里。
        </p>
        <div class="repo-url" @click="copyRepo">{{ GITHUB_REPO }}</div>
      </div>
      <div class="github-actions">
        <el-button type="primary" @click="openGithub">打开仓库</el-button>
        <el-button @click="copyRepo">复制链接</el-button>
        <el-button @click="open(GITHUB_RELEASES)">Releases</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

const GITHUB_REPO = 'https://github.com/zhuxiaoyi412826/qiangpiao'
const GITHUB_RELEASES = GITHUB_REPO + '/releases/latest'
const GITHUB_ZIP = GITHUB_REPO + '/archive/refs/heads/main.zip'

const stats = [
    { value: 'L1/L2/L3', label: '三级缓存架构' },
    { value: '0 超卖', label: 'Lua 原子扣库存' },
    { value: '6 档', label: '退票阶梯费率' },
    { value: '6 档', label: '改签场景费率' }
]

const features = [
    { title: '车次查询', desc: '按站点与日期查车次，支持席别筛选、区间余票与票价实时展示。' },
    { title: '秒杀抢票', desc: 'Redis 预扣库存 + Lua 原子扣减 + 线程池异步下单，限购一人一单，不超卖。' },
    { title: '在线支付', desc: '钱包余额扣款，支付单超时自动关闭，回调幂等，异常订单不会重复入账。' },
    { title: '订单与车票', desc: '未支付订单超时自动关单释放座位；已支付订单生成车票，支持查详情与时间轴。' },
    { title: '退票 / 改签', desc: '退票阶梯手续费、改签手续费、改签一次限制，规则与 12306 对齐，全程有流水。' },
    { title: '管理后台', desc: '车站、线路、车次、票价库存、订单、用户、公告与抢票流水监控一体化管理。' }
]

const steps = [
    { title: '注册 / 登录', desc: '手机号注册后登录，未登录也可先查车次。' },
    { title: '充值钱包', desc: '在「我的钱包」自助充值，购票时从余额扣款。' },
    { title: '查询车次', desc: '选择出发站、到达站与日期，查看余票和票价。' },
    { title: '下单抢票', desc: '选席别提交，秒杀链路异步处理，返回排队位置与结果。' },
    { title: '完成支付', desc: '在支付截止时间前完成支付，超时订单自动关闭并释放座位。' }
]

const downloads = [
    {
        key: 'android',
        platform: '移动端',
        name: 'Android 安装包',
        version: 'v1.0.0',
        size: '约 18 MB',
        require: 'Android 8.0+',
        desc: '手机 App，随时随地查票、抢票、看订单和办理退改签，支持消息提醒。',
        url: GITHUB_RELEASES + '/download/qiangpiao.apk'
    },
    {
        key: 'windows',
        platform: 'PC 端',
        name: 'Windows 安装包',
        version: 'v1.0.0',
        size: '约 65 MB',
        require: 'Windows 10 / 11',
        desc: '桌面客户端，大屏批量查票更顺手，支持多窗口比价与开售提醒。',
        url: GITHUB_RELEASES + '/download/qiangpiao-setup.exe'
    }
]

function go(path) {
    router.push(path)
}

function open(url) {
    window.open(url, '_blank')
}

function openGithub() {
    open(GITHUB_REPO)
}

async function copyRepo() {
    try {
        await navigator.clipboard.writeText(GITHUB_REPO)
        ElMessage.success('仓库地址已复制')
    } catch (e) {
        ElMessage.info(GITHUB_REPO)
    }
}
</script>

<style scoped>
.hero {
    background: linear-gradient(135deg, #1a73e8 0%, #4f9bf5 100%);
    border-radius: 10px;
    padding: 32px;
    margin-bottom: 16px;
    color: #fff;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 24px;
}

.hero-badge {
    display: inline-block;
    font-size: 12px;
    padding: 4px 10px;
    border-radius: 20px;
    background: rgba(255, 255, 255, .18);
    margin-bottom: 12px;
}

.hero-title {
    margin: 0 0 10px;
    font-size: 34px;
    font-weight: 700;
}

.hero-sub {
    margin: 0 0 20px;
    max-width: 620px;
    line-height: 1.7;
    font-size: 14px;
    color: rgba(255, 255, 255, .92);
}

.hero-actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
}

.icon {
    width: 16px;
    height: 16px;
    margin-right: 4px;
    vertical-align: -3px;
}

.hero-stats {
    display: grid;
    grid-template-columns: repeat(2, minmax(110px, 1fr));
    gap: 12px;
    flex: 0 0 auto;
}

.stat-item {
    background: rgba(255, 255, 255, .14);
    border-radius: 8px;
    padding: 12px 14px;
    text-align: center;
}

.stat-value {
    font-size: 18px;
    font-weight: 700;
}

.stat-label {
    font-size: 12px;
    color: rgba(255, 255, 255, .85);
    margin-top: 4px;
}

.section-title {
    margin: 0 0 8px;
    font-size: 18px;
    font-weight: 600;
}

.section-desc {
    margin: 0 0 16px;
    color: #606266;
    font-size: 14px;
    line-height: 1.7;
}

.feature-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
}

.feature-card {
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 14px;
    background: #fafcff;
}

.feature-title {
    font-weight: 600;
    margin-bottom: 6px;
}

.feature-desc {
    font-size: 13px;
    color: #909399;
    line-height: 1.6;
}

.use-steps {
    margin: 8px 0 4px;
}

.tips {
    margin: 0;
    padding-left: 20px;
    color: #606266;
    font-size: 13px;
    line-height: 2;
}

.download-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
}

.download-card {
    border: 1px solid #ebeef5;
    border-radius: 10px;
    padding: 18px;
    background: #fff;
}

.dl-head {
    display: flex;
    align-items: center;
    gap: 12px;
}

.dl-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
    background: #ecf5ff;
    color: #1a73e8;
    display: flex;
    align-items: center;
    justify-content: center;
}

.dl-icon svg {
    width: 24px;
    height: 24px;
}

.dl-platform {
    font-size: 12px;
    color: #909399;
}

.dl-name {
    font-size: 16px;
    font-weight: 600;
}

.dl-desc {
    margin: 12px 0;
    font-size: 13px;
    color: #606266;
    line-height: 1.7;
    min-height: 44px;
}

.dl-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    font-size: 12px;
    color: #909399;
    margin-bottom: 14px;
}

.dl-actions {
    display: flex;
    align-items: center;
    gap: 6px;
}

.dl-more {
    margin-top: 14px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
    font-size: 13px;
}

.github-card {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 20px;
}

.repo-url {
    font-family: Consolas, monospace;
    font-size: 13px;
    color: #1a73e8;
    background: #f5f7fa;
    border-radius: 6px;
    padding: 8px 12px;
    cursor: pointer;
    word-break: break-all;
}

.github-actions {
    display: flex;
    gap: 8px;
    flex: 0 0 auto;
    flex-wrap: wrap;
}

@media (max-width: 768px) {
    .hero {
        flex-direction: column;
        align-items: stretch;
        padding: 20px;
    }

    .hero-title {
        font-size: 26px;
    }

    .hero-stats {
        grid-template-columns: repeat(2, 1fr);
    }

    .feature-grid,
    .download-grid {
        grid-template-columns: 1fr;
    }

    .github-card {
        flex-direction: column;
    }
}
</style>
