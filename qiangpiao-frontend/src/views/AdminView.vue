<template>
  <div class="page-container">
    <div class="card-panel">
      <h3 class="page-title">管理后台</h3>
      <el-tabs v-model="tab" class="admin-tabs">
        <!-- 运营概览 -->
        <el-tab-pane label="运营概览" name="stats">
          <el-row :gutter="16">
            <el-col :span="6" v-for="c in statCards" :key="c.label">
              <el-card shadow="never" class="stat-card">
                <div class="stat-label">{{ c.label }}</div>
                <div class="stat-value">{{ c.value }}</div>
              </el-card>
            </el-col>
          </el-row>
          <el-divider content-position="left">每日售票</el-divider>
          <el-table :data="dailySales" border size="small">
            <el-table-column prop="statDate" label="日期" width="140"/>
            <el-table-column prop="orderCount" label="订单数" width="120"/>
            <el-table-column prop="amount" label="销售额" width="140"/>
          </el-table>
          <el-divider content-position="left">车次售票 TOP</el-divider>
          <el-table :data="trainSales" border size="small">
            <el-table-column prop="trainNo" label="车次" width="120"/>
            <el-table-column prop="departDate" label="发车日期" width="140"/>
            <el-table-column prop="soldCount" label="售出" width="100"/>
            <el-table-column prop="amount" label="销售额" width="140"/>
          </el-table>
          <div class="export-bar">
            <el-button type="primary" plain size="small" @click="doExport('daily-sales')">导出每日售票</el-button>
            <el-button type="primary" plain size="small" @click="doExport('train-sales')">导出车次售票</el-button>
            <el-button type="primary" plain size="small" @click="doExport('user-growth')">导出新增用户</el-button>
          </div>
        </el-tab-pane>

        <!-- 车站 -->
        <el-tab-pane label="车站管理" name="stations">
          <el-button type="primary" size="small" @click="openStation()">新增车站</el-button>
          <!-- 车站 3000+，必须分页渲染，否则切到本页会阻塞主线程数秒 -->
          <el-table :data="pagedStations" border size="small" class="mt">
            <el-table-column prop="id" label="ID" width="70"/>
            <el-table-column prop="stationName" label="车站名称" width="160"/>
            <el-table-column prop="city" label="城市" width="120"/>
            <el-table-column prop="pyCode" label="拼音简码" width="140"/>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openStation(row)">编辑</el-button>
                <el-button link :type="row.status === 1 ? 'danger' : 'success'" size="small"
                           @click="toggleStation(row)">
                  {{ row.status === 1 ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="mt" background layout="total, prev, pager, next"
                         :current-page="stationPage" :page-size="stationPageSize" :total="stations.length"
                         @current-change="p => (stationPage = p)"/>
        </el-tab-pane>

        <!-- 线路 -->
        <el-tab-pane label="线路管理" name="lines">
          <el-button type="primary" size="small" @click="openLine()">新增线路</el-button>
          <el-table :data="lines" border size="small" class="mt">
            <el-table-column prop="id" label="ID" width="70"/>
            <el-table-column prop="lineName" label="线路名称" min-width="180"/>
            <el-table-column label="起止" min-width="200">
              <template #default="{ row }">{{ row.fromStationName }} → {{ row.toStationName }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openLine(row)">编辑</el-button>
                <el-button link type="warning" size="small" @click="openLineStations(row)">途经站</el-button>
                <el-button link type="danger" size="small" @click="removeLine(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 车次 -->
        <el-tab-pane label="车次管理" name="trains">
          <el-table :data="trainList" border size="small" v-loading="loading">
            <el-table-column prop="id" label="ID" width="70"/>
            <el-table-column prop="trainNo" label="车次" width="100"/>
            <el-table-column prop="trainType" label="车型" width="90"/>
            <el-table-column label="区间" min-width="180">
              <template #default="{ row }">{{ row.fromStationName }} → {{ row.toStationName }}</template>
            </el-table-column>
            <el-table-column prop="departDate" label="发车日期" width="120"/>
            <el-table-column prop="departTime" label="发车" width="90"/>
            <el-table-column prop="arriveTime" label="到达" width="90"/>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '可售' : '停运' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="420" fixed="right">
              <template #default="{ row }">
                <el-button link :type="row.status === 1 ? 'danger' : 'success'" size="small"
                           @click="toggleTrain(row)">
                  {{ row.status === 1 ? '停开' : '恢复' }}
                </el-button>
                <el-button link type="primary" size="small" @click="openSchedule(row)">生成班次</el-button>
                <el-button link type="warning" size="small" @click="openStops(row)">时刻表</el-button>
                <el-button link type="warning" size="small" @click="openCarriages(row)">车厢</el-button>
                <el-button link type="info" size="small" @click="openSaleWindow(row)">售卖窗口</el-button>
                <el-button link type="info" size="small" @click="initSegments(row)">区间库存</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next"
                         :current-page="trainPage" :page-size="10" :total="trainTotal"
                         @current-change="p => { trainPage = p; loadTrains() }"/>
        </el-tab-pane>

        <!-- 票价 -->
        <el-tab-pane label="票价 / 库存" name="price">
          <el-form :inline="true" size="small" @submit.prevent>
            <el-form-item label="车次号">
              <el-input v-model="stockTrainNo" clearable placeholder="如 G1001" style="width: 160px"
                        @keyup.enter="reloadStocks" @clear="reloadStocks"/>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="reloadStocks">查询</el-button>
            </el-form-item>
          </el-form>
          <!-- 库存 8000+ 行：必须分页渲染，一次性拉全表会阻塞主线程数秒 -->
          <el-table :data="stocks" border size="small">
            <el-table-column prop="trainNo" label="车次" width="100"/>
            <el-table-column prop="departDate" label="发车日期" width="120"/>
            <el-table-column label="席别" width="100">
              <template #default="{ row }">{{ seatTypeName(row.seatType) }}</template>
            </el-table-column>
            <el-table-column prop="totalCount" label="总座位" width="100"/>
            <el-table-column prop="availableCount" label="余票" width="100"/>
            <el-table-column label="票价" width="180">
              <template #default="{ row }">
                <el-input-number v-model="row.price" :min="0" :step="10" size="small" controls-position="right"/>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="savePrice(row)">保存票价</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="mt" background layout="total, prev, pager, next"
                         :current-page="stockPage" :page-size="stockPageSize" :total="stockTotal"
                         @current-change="p => { stockPage = p; loadStocks() }"/>
        </el-tab-pane>

        <!-- 订单 -->
        <el-tab-pane label="订单管理" name="orders">
          <el-form :inline="true" size="small" @submit.prevent>
            <el-form-item label="订单号">
              <el-input v-model="orderQuery.orderNo" clearable style="width: 200px"/>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="orderQuery.phone" clearable style="width: 140px"/>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="orderQuery.status" clearable style="width: 120px">
                <el-option :value="0" label="待支付"/>
                <el-option :value="1" label="已支付"/>
                <el-option :value="2" label="已取消"/>
                <el-option :value="3" label="已退票"/>
                <el-option :value="4" label="已超时"/>
                <el-option :value="5" label="已改签"/>
              </el-select>
            </el-form-item>
            <el-form-item label="下单日期">
              <el-date-picker v-model="orderQuery.startDate" type="date" value-format="YYYY-MM-DD"
                              placeholder="开始" style="width: 150px"/>
              <el-date-picker v-model="orderQuery.endDate" type="date" value-format="YYYY-MM-DD"
                              placeholder="结束" style="width: 150px" class="ml"/>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadOrders">查询</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="orders" border size="small">
            <el-table-column prop="orderNo" label="订单号" width="200"/>
            <el-table-column prop="username" label="用户" width="100"/>
            <el-table-column prop="phone" label="手机号" width="130"/>
            <el-table-column prop="passengerName" label="乘客" width="100"/>
            <el-table-column prop="trainNo" label="车次" width="90"/>
            <el-table-column label="席位" width="130">
              <template #default="{ row }">
                <span v-if="row.carriageNo">{{ row.carriageNo }}车{{ row.seatNo }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="票价" width="90"/>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openOrderDetail(row)">详情</el-button>
                <el-button link type="danger" size="small" :disabled="row.status !== 1"
                           @click="adminRefund(row)">退票
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next"
                         :current-page="orderQuery.pageNum" :page-size="orderQuery.pageSize" :total="orderTotal"
                         @current-change="p => { orderQuery.pageNum = p; loadOrders() }"/>
        </el-tab-pane>

        <!-- 抢票流水 -->
        <el-tab-pane label="抢票流水" name="flows">
          <el-form :inline="true" size="small" @submit.prevent>
            <el-form-item label="用户ID">
              <el-input v-model="flowQuery.userId" clearable style="width: 140px" placeholder="如 1"/>
            </el-form-item>
            <el-form-item label="车次ID">
              <el-input v-model="flowQuery.trainId" clearable style="width: 140px"/>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="flowQuery.status" clearable style="width: 120px" placeholder="全部">
                <el-option label="排队中" :value="0"/>
                <el-option label="成功" :value="1"/>
                <el-option label="失败" :value="2"/>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="reloadFlows">查询</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="flows" border size="small" class="mt">
            <el-table-column prop="orderNo" label="订单号" min-width="180"/>
            <el-table-column prop="userId" label="用户" width="80"/>
            <el-table-column prop="trainId" label="车次ID" width="90"/>
            <el-table-column prop="passengerName" label="乘客" width="100"/>
            <el-table-column prop="ticketIndex" label="第几张" width="80"/>
            <el-table-column label="结果" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="flowStatusType(row.status)">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failReason" label="失败原因" min-width="180"/>
            <el-table-column prop="queueSeq" label="排队号" width="80"/>
            <el-table-column prop="costMs" label="耗时(ms)" width="90"/>
            <el-table-column prop="createTime" label="受理时间" width="180"/>
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next"
                         :current-page="flowQuery.pageNum" :page-size="flowQuery.pageSize" :total="flowTotal"
                         @current-change="p => { flowQuery.pageNum = p; loadFlows() }"/>
        </el-tab-pane>

        <!-- 用户 -->
        <el-tab-pane label="用户管理" name="users">
          <el-form :inline="true" size="small" @submit.prevent>
            <el-form-item label="账号 / 手机号">
              <el-input v-model="userKeyword" clearable style="width: 200px"/>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadUsers">查询</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="users" border size="small">
            <el-table-column prop="id" label="ID" width="70"/>
            <el-table-column prop="username" label="账号" width="140"/>
            <el-table-column prop="realName" label="姓名" width="120"/>
            <el-table-column prop="phone" label="手机号" width="140"/>
            <el-table-column prop="idCard" label="身份证" width="180"/>
            <el-table-column prop="role" label="角色" width="120"/>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '正常' : '已封禁' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button link :type="row.status === 1 ? 'danger' : 'success'" size="small"
                           @click="toggleUser(row)">
                  {{ row.status === 1 ? '封禁' : '解封' }}
                </el-button>
                <el-button link type="warning" size="small" @click="kickOffline(row)">强制下线</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination class="pager" background layout="total, prev, pager, next"
                         :current-page="userPage" :page-size="10" :total="userTotal"
                         @current-change="p => { userPage = p; loadUsers() }"/>
        </el-tab-pane>

        <!-- 公告 -->
        <el-tab-pane label="公告管理" name="notice">
          <el-button type="primary" size="small" @click="openNotice()">发布公告</el-button>
          <el-table :data="notices" border size="small" class="mt">
            <el-table-column prop="title" label="标题" min-width="220"/>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">{{ noticeTypeName(row.type) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '已发布' : '已下架' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发布时间" width="180"/>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openNotice(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeNotice(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 票务监控 -->
        <el-tab-pane label="票务监控" name="monitor">
          <el-divider content-position="left">余票监控</el-divider>
          <el-table :data="stocks" border size="small">
            <el-table-column prop="trainNo" label="车次" width="100"/>
            <el-table-column prop="departDate" label="日期" width="120"/>
            <el-table-column label="席别" width="100">
              <template #default="{ row }">{{ seatTypeName(row.seatType) }}</template>
            </el-table-column>
            <el-table-column prop="totalCount" label="总数" width="90"/>
            <el-table-column prop="availableCount" label="余票" width="90"/>
            <el-table-column label="占用率" min-width="220">
              <template #default="{ row }">
                <el-progress :percentage="soldPercent(row)" :stroke-width="12"/>
              </template>
            </el-table-column>
          </el-table>
          <el-divider content-position="left">锁票（下单后临时锁定，超时自动释放）</el-divider>
          <el-table :data="lockedSeats" border size="small">
            <el-table-column prop="trainId" label="车次ID" width="90"/>
            <el-table-column prop="carriageNo" label="车厢" width="80"/>
            <el-table-column prop="seatNo" label="座位" width="90"/>
            <el-table-column prop="orderNo" label="占用订单" min-width="220"/>
            <el-table-column prop="updateTime" label="锁定时间" width="180"/>
          </el-table>
        </el-tab-pane>

        <!-- 风控 -->
        <el-tab-pane label="风控管理" name="risk">
          <el-divider content-position="left">IP 黑名单</el-divider>
          <el-form :inline="true" size="small" @submit.prevent>
            <el-form-item label="IP">
              <el-input v-model="blockForm.ip" placeholder="如 1.2.3.4" style="width: 180px"/>
            </el-form-item>
            <el-form-item label="时长(秒)">
              <el-input-number v-model="blockForm.seconds" :min="60" :step="60" style="width: 150px"/>
            </el-form-item>
            <el-form-item label="原因">
              <el-input v-model="blockForm.reason" placeholder="选填" style="width: 220px"/>
            </el-form-item>
            <el-form-item>
              <el-button type="danger" size="small" @click="doBlockIp">拉黑</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="blacklist" border size="small">
            <el-table-column prop="ip" label="IP" width="160"/>
            <el-table-column prop="reason" label="原因 / 解封时间" min-width="320"/>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="success" size="small" @click="doUnblockIp(row.ip)">解除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-divider content-position="left">风控事件（同一 IP 多账号 / 极短耗时请求）</el-divider>
          <el-table :data="riskEvents" border size="small" max-height="360">
            <el-table-column prop="text" label="事件" min-width="520"/>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 车站编辑 -->
    <el-dialog v-model="stationVisible" :title="stationForm.id ? '编辑车站' : '新增车站'" width="420px">
      <el-form :model="stationForm" label-width="90px">
        <el-form-item label="车站名称"><el-input v-model="stationForm.stationName"/></el-form-item>
        <el-form-item label="城市"><el-input v-model="stationForm.city"/></el-form-item>
        <el-form-item label="拼音简码"><el-input v-model="stationForm.pyCode"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stationVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStation">保存</el-button>
      </template>
    </el-dialog>

    <!-- 线路编辑 -->
    <el-dialog v-model="lineVisible" :title="lineForm.id ? '编辑线路' : '新增线路'" width="460px">
      <el-form :model="lineForm" label-width="90px">
        <el-form-item label="线路名称"><el-input v-model="lineForm.lineName"/></el-form-item>
        <el-form-item label="起点站">
          <el-select-v2 v-model="lineForm.fromStationId" :options="stationOptions" filterable
                        placeholder="输入或选择" style="width: 100%"
                        @change="onLineStationChange('from')"/>
        </el-form-item>
        <el-form-item label="终点站">
          <el-select-v2 v-model="lineForm.toStationId" :options="stationOptions" filterable
                        placeholder="输入或选择" style="width: 100%"
                        @change="onLineStationChange('to')"/>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="lineForm.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lineVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLine">保存</el-button>
      </template>
    </el-dialog>

    <!-- 线路途经站 -->
    <el-dialog v-model="lineStationVisible" title="线路途经站（按数组顺序）" width="600px">
      <el-button size="small" @click="addLineStationRow">添加站点</el-button>
      <el-table :data="lineStationRows" border size="small" class="mt">
        <el-table-column label="顺序" width="90">
          <template #default="{ row, $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="车站">
          <template #default="{ row }">
            <el-select-v2 v-model="row.stationId" :options="stationOptions" filterable
                          placeholder="输入或选择" style="width: 100%"
                          @change="onRowStationChange(row)"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="lineStationRows.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="lineStationVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLineStations">保存</el-button>
      </template>
    </el-dialog>

    <!-- 售卖时间窗口 -->
    <el-dialog v-model="saleWindowVisible" title="设置售卖时间窗口" width="440px">
      <p class="muted">窗口外不允许抢票 / 下单；两栏都留空表示不限时（默认一直卖）。</p>
      <el-form label-width="96px" size="small">
        <el-form-item label="开始时间">
          <el-input v-model="saleWindowForm.start" placeholder="yyyy-MM-dd HH:mm:ss，留空=不限"/>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-input v-model="saleWindowForm.end" placeholder="yyyy-MM-dd HH:mm:ss，留空=不限"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saleWindowVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSaleWindow">保存</el-button>
      </template>
    </el-dialog>

    <!-- 生成班次 -->
    <el-dialog v-model="scheduleVisible" title="按日期生成当日班次" width="420px">
      <p class="muted">将复制该车次的车型、时刻、库存与座位安排，生成指定日期的新班次。</p>
      <el-form label-width="90px">
        <el-form-item label="发车日期">
          <el-date-picker v-model="scheduleDate" type="date" value-format="YYYY-MM-DD" style="width: 100%"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">生成</el-button>
      </template>
    </el-dialog>

    <!-- 时刻表 -->
    <el-dialog v-model="stopVisible" title="运行时刻表（按数组顺序）" width="720px">
      <el-button size="small" @click="addStopRow">添加停靠站</el-button>
      <el-table :data="stopRows" border size="small" class="mt">
        <el-table-column label="顺序" width="70">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="车站" min-width="150">
          <template #default="{ row }">
            <el-select-v2 v-model="row.stationId" :options="stationOptions" filterable
                          placeholder="输入或选择" style="width: 100%"
                          @change="onStopStationChange(row)"/>
          </template>
        </el-table-column>
        <el-table-column label="到站" width="130">
          <template #default="{ row }"><el-time-picker v-model="row.arriveTime" value-format="HH:mm:ss" style="width: 100%"/></template>
        </el-table-column>
        <el-table-column label="发车" width="130">
          <template #default="{ row }"><el-time-picker v-model="row.departTime" value-format="HH:mm:ss" style="width: 100%"/></template>
        </el-table-column>
        <el-table-column label="停靠(分)" width="100">
          <template #default="{ row }"><el-input-number v-model="row.stopMinutes" :min="0" size="small" controls-position="right"/></template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="stopRows.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="stopVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStops">保存</el-button>
      </template>
    </el-dialog>

    <!-- 车厢 -->
    <el-dialog v-model="carriageVisible" title="车厢编排" width="640px">
      <el-button size="small" @click="addCarriageRow">添加车厢</el-button>
      <el-table :data="carriageRows" border size="small" class="mt">
        <el-table-column label="车厢号" width="110">
          <template #default="{ row }"><el-input-number v-model="row.carriageNo" :min="1" size="small" controls-position="right"/></template>
        </el-table-column>
        <el-table-column label="车厢类型" min-width="160">
          <template #default="{ row }">
            <el-select v-model="row.seatType" style="width: 100%">
              <el-option :value="1" label="商务座"/>
              <el-option :value="2" label="一等座"/>
              <el-option :value="3" label="二等座"/>
              <el-option :value="4" label="软卧"/>
              <el-option :value="5" label="硬卧"/>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="座位总数" width="130">
          <template #default="{ row }"><el-input-number v-model="row.seatCount" :min="0" size="small" controls-position="right"/></template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="carriageRows.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="carriageVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCarriages">保存</el-button>
      </template>
    </el-dialog>

    <!-- 公告编辑 -->
    <el-dialog v-model="noticeVisible" :title="noticeForm.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form :model="noticeForm" label-width="90px">
        <el-form-item label="标题"><el-input v-model="noticeForm.title"/></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="noticeForm.type" style="width: 100%">
            <el-option :value="1" label="停运通知"/>
            <el-option :value="2" label="节假日通知"/>
            <el-option :value="3" label="其他"/>
          </el-select>
        </el-form-item>
        <el-form-item label="正文"><el-input v-model="noticeForm.content" type="textarea" :rows="4"/></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="noticeForm.status">
            <el-radio-button :value="1">发布</el-radio-button>
            <el-radio-button :value="0">下架</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNotice">保存</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情（含时间轴） -->
    <el-drawer v-model="orderVisible" title="订单详情" size="480px">
      <template v-if="currentOrder">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentOrder.username }}（{{ currentOrder.phone }}）</el-descriptions-item>
          <el-descriptions-item label="乘客">{{ currentOrder.passengerName }}</el-descriptions-item>
          <el-descriptions-item label="身份证">{{ currentOrder.idCard || '-' }}</el-descriptions-item>
          <el-descriptions-item label="车次">{{ currentOrder.trainNo }} {{ currentOrder.fromStationName }} → {{ currentOrder.toStationName }}</el-descriptions-item>
          <el-descriptions-item label="席位">{{ currentOrder.carriageNo }}车{{ currentOrder.seatNo }}座</el-descriptions-item>
          <el-descriptions-item label="票价">¥{{ currentOrder.price }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ currentOrder.statusText }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">流转时间轴</el-divider>
        <el-timeline>
          <el-timeline-item v-for="log in orderLogs" :key="log.id"
                            :timestamp="log.createTime" :type="timelineType(log.action)" placement="top">
            <div class="log-title">{{ log.actionText }}</div>
            <div class="muted">{{ log.detail }}</div>
          </el-timeline-item>
        </el-timeline>
        <div v-if="orderChanges.length">
          <el-divider content-position="left">改签历史</el-divider>
          <div v-for="c in orderChanges" :key="c.id" class="muted">
            {{ c.createTime }}：车次 {{ c.oldTrainId }} → {{ c.newTrainId }}，差额 {{ c.diffAmount }}
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/admin'

const route = useRoute()
const router = useRouter()

// tab 与顶部导航栏同步（/admin?tab=xxx），管理员登录后导航栏直接平铺各管理模块
const tab = ref(route.query.tab || 'stats')
watch(tab, v => {
  if (route.query.tab !== v) {
    router.replace({ query: { ...route.query, tab: v } })
  }
})
watch(() => route.query.tab, v => {
  if (v && v !== tab.value) {
    tab.value = v
  }
})

// 概览
const stats = ref({})
const dailySales = ref([])
const trainSales = ref([])
const statCards = computed(() => {
  const r = stats.value.revenue || {}
  const u = stats.value.users || {}
  return [
    { label: '售票收入', value: '¥' + (r.income ?? 0) },
    { label: '退票金额', value: '¥' + (r.refundAmount ?? 0) },
    { label: '用户充值', value: '¥' + (r.rechargeAmount ?? 0) },
    { label: '注册用户 / 封禁', value: `${u.total ?? 0} / ${u.banned ?? 0}` }
  ]
})

// 车站
const stations = ref([])
/** 供 el-select-v2 使用：3000+ 站点用虚拟滚动，避免每个下拉都渲染上千 el-option */
const stationOptions = computed(() =>
  stations.value.map(s => ({ value: s.id, label: s.stationName }))
)
const stationVisible = ref(false)
const stationForm = ref({})
const stationPage = ref(1)
const stationPageSize = ref(20)
const pagedStations = computed(() =>
  stations.value.slice((stationPage.value - 1) * stationPageSize.value, stationPage.value * stationPageSize.value)
)

// 线路
const lines = ref([])
const lineVisible = ref(false)
const lineForm = ref({})
const lineStationVisible = ref(false)
const lineStationRows = ref([])
const currentLineId = ref(null)

// 车次
const trainList = ref([])
const trainTotal = ref(0)
const trainPage = ref(1)
const loading = ref(false)
const scheduleVisible = ref(false)
const scheduleDate = ref('')
const currentTrain = ref(null)
const stopVisible = ref(false)
const stopRows = ref([])
const carriageVisible = ref(false)
const carriageRows = ref([])

// 票价 / 库存
const stocks = ref([])
const stockPage = ref(1)
const stockPageSize = ref(20)
const stockTotal = ref(0)
const stockTrainNo = ref('')

// 订单
const orders = ref([])
const orderTotal = ref(0)
const orderQuery = ref({ orderNo: '', phone: '', status: null, startDate: '', endDate: '', pageNum: 1, pageSize: 10 })
const orderVisible = ref(false)
const currentOrder = ref(null)
const orderLogs = ref([])
const orderChanges = ref([])

// 抢票流水（排查超卖 / 抢票失败用）
const flows = ref([])
const flowTotal = ref(0)
const flowQuery = ref({ userId: null, trainId: null, status: null, pageNum: 1, pageSize: 10 })

// 用户
const users = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userKeyword = ref('')

// 公告
const notices = ref([])
const noticeVisible = ref(false)
const noticeForm = ref({})

// 监控
const lockedSeats = ref([])

// 风控（IP 黑名单 + 风控事件）
const blacklist = ref([])
const riskEvents = ref([])
const blockForm = ref({ ip: '', seconds: 1800, reason: '' })

// 后台数据量大（车次 2700 / 库存 8000 / 车站 3000），首屏只加载当前页签，切页签时再加载
const loadedTabs = ref(new Set())
const TAB_LOADERS = {
  stats: loadStats,
  stations: loadStations,
  lines: loadLines,
  trains: loadTrains,
  price: loadStocks,
  orders: loadOrders,
  users: loadUsers,
  notice: loadNotices,
  monitor: loadLocked,
  risk: loadRisk
}

async function ensureTab(name) {
  const loader = TAB_LOADERS[name]
  if (!loader || loadedTabs.value.has(name)) return
  loadedTabs.value.add(name)
  await loader()
}

onMounted(async () => {
  await ensureTab(tab.value)
})
watch(tab, v => {
  ensureTab(v)
})

// ============ 数据加载 ============
async function loadRisk() {
  try {
    const map = await api.adminBlacklist()
    blacklist.value = Object.keys(map || {}).map(ip => ({ ip, reason: map[ip] }))
    const list = await api.adminRiskEvents(50)
    riskEvents.value = (list || []).map(text => ({ text }))
  } catch (e) {
    ElMessage.error(e.message || '风控数据加载失败')
  }
}

async function doBlockIp() {
  if (!blockForm.value.ip) {
    return ElMessage.warning('请输入要拉黑的 IP')
  }
  try {
    await api.adminBlockIp(blockForm.value.ip, blockForm.value.seconds, blockForm.value.reason)
    ElMessage.success('已拉黑该 IP')
    blockForm.value.ip = ''
    blockForm.value.reason = ''
    await loadRisk()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function doUnblockIp(ip) {
  try {
    await api.adminUnblockIp(ip)
    ElMessage.success('已解除拉黑')
    await loadRisk()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadStats() {
  try {
    stats.value = await api.adminStats()
    dailySales.value = await api.adminDailySales()
    trainSales.value = await api.adminTrainSales()
  } catch (e) { ElMessage.error(e.message) }
}
async function loadStations() {
  try { stations.value = await api.adminStations() } catch (e) { ElMessage.error(e.message) }
}
async function loadLines() {
  try { lines.value = await api.adminLines() } catch (e) { ElMessage.error(e.message) }
}
async function loadTrains() {
  loading.value = true
  try {
    const data = await api.adminTrains({ pageNum: trainPage.value, pageSize: 10 })
    trainList.value = data.list || []
    trainTotal.value = data.total || 0
  } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}
async function loadStocks() {
  try {
    const data = await api.adminStockMonitor({
      pageNum: stockPage.value,
      pageSize: stockPageSize.value,
      trainNo: stockTrainNo.value
    })
    stocks.value = data.list || []
    stockTotal.value = data.total || 0
  } catch (e) { ElMessage.error(e.message) }
}
function reloadStocks() {
  stockPage.value = 1
  loadStocks()
}
async function loadOrders() {
  try {
    const data = await api.adminOrders(orderQuery.value)
    orders.value = data.list || []
    orderTotal.value = data.total || 0
  } catch (e) { ElMessage.error(e.message) }
}
async function loadFlows() {
  try {
    const data = await api.adminSeckillFlows(flowQuery.value)
    flows.value = data.list || []
    flowTotal.value = data.total || 0
  } catch (e) { ElMessage.error(e.message) }
}
function reloadFlows() {
  flowQuery.value.pageNum = 1
  loadFlows()
}
function flowStatusType(status) {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

async function loadUsers() {
  try {
    const data = await api.adminUsers({ keyword: userKeyword.value, pageNum: userPage.value, pageSize: 10 })
    users.value = data.list || []
    userTotal.value = data.total || 0
  } catch (e) { ElMessage.error(e.message) }
}
async function loadNotices() {
  try {
    const data = await api.adminAnnouncements({ pageNum: 1, pageSize: 20 })
    notices.value = data.list || []
  } catch (e) { ElMessage.error(e.message) }
}
async function loadLocked() {
  try { lockedSeats.value = await api.adminLockedSeats(50) } catch (e) { ElMessage.error(e.message) }
}

// ============ 车站 ============
function openStation(row) {
  stationForm.value = row ? { ...row } : { stationName: '', city: '', pyCode: '' }
  stationVisible.value = true
}
async function submitStation() {
  await api.saveAdminStation(stationForm.value)
  ElMessage.success('已保存')
  stationVisible.value = false
  loadStations()
}
async function toggleStation(row) {
  await api.updateStationStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('已更新')
  loadStations()
}

// ============ 线路 ============
function openLine(row) {
  lineForm.value = row ? { ...row } : { lineName: '', fromStationId: null, toStationId: null, status: 1 }
  lineVisible.value = true
}
function onLineStationChange(side) {
  const s = stations.value.find(x => x.id === (side === 'from' ? lineForm.value.fromStationId : lineForm.value.toStationId))
  if (!s) return
  if (side === 'from') { lineForm.value.fromStationName = s.stationName } else { lineForm.value.toStationName = s.stationName }
}
async function submitLine() {
  await api.saveAdminLine(lineForm.value)
  ElMessage.success('已保存')
  lineVisible.value = false
  loadLines()
}
async function removeLine(row) {
  await ElMessageBox.confirm(`删除线路「${row.lineName}」？`, '提示', { type: 'warning' })
  await api.deleteAdminLine(row.id)
  ElMessage.success('已删除')
  loadLines()
}
async function openLineStations(row) {
  currentLineId.value = row.id
  lineStationRows.value = await api.adminLineStations(row.id)
  lineStationVisible.value = true
}
function addLineStationRow() {
  lineStationRows.value.push({ stationId: null, stationName: '', stopOrder: lineStationRows.value.length + 1 })
}
function onRowStationChange(row) {
  const s = stations.value.find(x => x.id === row.stationId)
  if (s) row.stationName = s.stationName
}
async function submitLineStations() {
  await api.saveAdminLineStations(currentLineId.value, lineStationRows.value)
  ElMessage.success('已保存')
  lineStationVisible.value = false
}

// ============ 车次 ============
// 售卖时间窗口（区间票 / 限时售卖）
const saleWindowVisible = ref(false)
const saleWindowForm = ref({ start: '', end: '' })

function openSaleWindow(row) {
  currentTrain.value = row
  saleWindowForm.value = { start: '', end: '' }
  saleWindowVisible.value = true
}

async function submitSaleWindow() {
  await api.updateSaleWindow(currentTrain.value.id,
      saleWindowForm.value.start, saleWindowForm.value.end)
  ElMessage.success('售卖时间窗口已更新')
  saleWindowVisible.value = false
  loadTrains()
}

/** 初始化该车次的区间库存（相邻站单段）并预热到 Redis */
async function initSegments(row) {
  try {
    const n = await api.initSegmentStock(row.id)
    ElMessage.success(`已初始化 ${n} 个区间库存段`)
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function toggleTrain(row) {
  await api.updateTrainStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已停开该班次' : '已恢复售票')
  loadTrains()
}
function openSchedule(row) {
  currentTrain.value = row
  scheduleDate.value = ''
  scheduleVisible.value = true
}
async function submitSchedule() {
  if (!scheduleDate.value) return ElMessage.warning('请选择日期')
  const id = await api.generateDailyTrain(currentTrain.value.id, scheduleDate.value)
  ElMessage.success('已生成班次，ID：' + id)
  scheduleVisible.value = false
  loadTrains()
}
async function openStops(row) {
  currentTrain.value = row
  stopRows.value = await api.adminStops(row.id)
  stopVisible.value = true
}
function addStopRow() {
  stopRows.value.push({ stationId: null, stationName: '', arriveTime: '', departTime: '', stopMinutes: 0 })
}
function onStopStationChange(row) {
  const s = stations.value.find(x => x.id === row.stationId)
  if (s) row.stationName = s.stationName
}
async function submitStops() {
  await api.saveAdminStops(currentTrain.value.id, stopRows.value)
  ElMessage.success('时刻表已保存')
  stopVisible.value = false
}
async function openCarriages(row) {
  currentTrain.value = row
  carriageRows.value = await api.adminCarriages(row.id)
  carriageVisible.value = true
}
function addCarriageRow() {
  carriageRows.value.push({ carriageNo: carriageRows.value.length + 1, seatType: 3, seatCount: 100 })
}
async function submitCarriages() {
  await api.saveAdminCarriages(currentTrain.value.id, carriageRows.value)
  ElMessage.success('车厢编排已保存')
  carriageVisible.value = false
}

// ============ 票价 ============
async function savePrice(row) {
  await api.updateStockPrice(row.id, row.price)
  ElMessage.success('票价已更新')
}

// ============ 订单 ============
async function openOrderDetail(row) {
  currentOrder.value = row
  orderLogs.value = await api.adminOrderLogs(row.orderNo)
  orderChanges.value = await api.adminOrderChanges(row.orderNo)
  orderVisible.value = true
}
async function adminRefund(row) {
  await ElMessageBox.confirm(`确认对订单 ${row.orderNo} 执行退票？票款将退回用户钱包。`, '提示', { type: 'warning' })
  await api.adminRefundOrder(row.orderNo, '客服人工退票')
  ElMessage.success('退票成功')
  loadOrders()
}

// ============ 用户 ============
async function toggleUser(row) {
  await api.updateUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已封禁（已同步踢下线）' : '已解封')
  loadUsers()
}

/** 强制下线：拉黑该用户所有 token，对方下一次请求即 401 */
async function kickOffline(row) {
  try {
    await ElMessageBox.confirm(`确定将 ${row.username} 强制下线吗？其所有登录端会立即失效。`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    const n = await api.kickUser(row.id)
    ElMessage.success(n > 0 ? `已强制下线，失效 ${n} 个登录态` : '该用户当前没有在线登录态')
  } catch (e) {
    ElMessage.error(e.message)
  }
}

// ============ 公告 ============
function openNotice(row) {
  noticeForm.value = row ? { ...row } : { title: '', content: '', type: 3, status: 1 }
  noticeVisible.value = true
}
async function submitNotice() {
  await api.saveAdminAnnouncement(noticeForm.value)
  ElMessage.success('已保存')
  noticeVisible.value = false
  loadNotices()
}
async function removeNotice(row) {
  await api.deleteAdminAnnouncement(row.id)
  ElMessage.success('已删除')
  loadNotices()
}

// ============ 工具 ============
function seatTypeName(t) {
  return { 1: '商务座', 2: '一等座', 3: '二等座', 4: '软卧', 5: '硬卧' }[t] || '未知'
}
function noticeTypeName(t) {
  return { 1: '停运通知', 2: '节假日通知', 3: '其他' }[t] || '其他'
}
function soldPercent(row) {
  if (!row.totalCount) return 0
  return Math.round((row.totalCount - row.availableCount) * 100 / row.totalCount)
}
function timelineType(action) {
  if (action === 'PAY') return 'success'
  if (action === 'REFUND' || action === 'CANCEL' || action === 'EXPIRE') return 'danger'
  if (action === 'CHANGE') return 'warning'
  return 'primary'
}
async function doExport(type) {
  try {
    const blob = await api.exportReport(type)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${type}.csv`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e.message)
  }
}
</script>

<style scoped>
/* 后台管理模块入口已平铺到顶部导航栏，隐藏页内 tab 头部，只保留内容区 */
.admin-tabs :deep(.el-tabs__header) {
  display: none;
}
.page-title { margin: 0 0 12px; font-size: 18px; }
.stat-card { text-align: center; margin-bottom: 8px; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { font-size: 22px; font-weight: 700; margin-top: 6px; }
.mt { margin-top: 10px; }
.ml { margin-left: 8px; }
.export-bar { margin-top: 12px; display: flex; gap: 8px; }
.pager { margin-top: 12px; justify-content: flex-end; }
.log-title { font-weight: 600; }
</style>
