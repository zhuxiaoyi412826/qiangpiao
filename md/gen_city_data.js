/**
 * 站点数据重建：只保留「市」级城市（数据源 md/市.txt）
 *
 * 生成内容：
 *   1. t_station   —— 市.txt 中的市级行政区（约 400 个）
 *   2. t_line / t_line_station —— 15 条高铁线路，途经站全部为市级城市
 *   3. t_train     —— 每条线路 6 个车次号（G/D），只生成「今天」的模板班次，
 *                     未来 30 天由后端 TrainScheduleTask 自动复制补齐
 *   4. t_train_stop / t_carriage / t_seat / t_train_stock —— 时刻表、车厢、座位、余票
 *
 * 输出：md/city_data.sql
 */
const fs = require('fs')
const path = require('path')

// pinyin-pro 装在前端工程下，这里按绝对路径引用（仅生成数据用，不写入项目依赖）
const { pinyin } = require('d:/daima/Lianshi/qiangpiao/qiangpiao-frontend/node_modules/pinyin-pro')

const BASE = 'd:/daima/Lianshi/qiangpiao/md'

/** 起始日期：生成今天起的 DAYS 天班次，其余未来日期由后端 TrainScheduleTask 补齐 */
const DAYS = 3
const START_DATE = new Date()

function dateOffset(n) {
  const d = new Date(START_DATE.getTime() + n * 86400000)
  return d.toISOString().slice(0, 10)
}

// ---------------------------------------------------------------- 站点

/** 民族称谓（长词在前，保证优先匹配），用于简化自治州 / 自治县名称 */
const ETHNIC_RE = /(?:乌孜别克|柯尔克孜|维吾尔|哈萨克|达斡尔|鄂温克|鄂伦春|塔吉克|塔塔尔|土家|布依|朝鲜|傈僳|景颇|哈尼|蒙古|撒拉|珞巴|门巴|赫哲|裕固|东乡|保安|毛南|仫佬|锡伯|纳西|独龙|普米|德昂|阿昌|布朗|基诺|拉祜|俄罗斯|苗|藏|羌|彝|壮|傣|回|黎|白|怒|侗|佤|瑶|畲|满|土|水|京)族$/

/** 去掉行政后缀得到城市名（自治州再去掉民族称谓，如 延边朝鲜族自治州 -> 延边） */
function cityOf(name) {
  let city = name.replace(/(特别行政区|自治州|地区|盟|林区|自治县|县|市)$/, '')
  while (ETHNIC_RE.test(city)) {
    city = city.replace(ETHNIC_RE, '')
  }
  return city || name
}

/** 拼音首字母大写，如 北京市 -> BJS */
function pyOf(name) {
  return pinyin(name, { toneType: 'none', pattern: 'first' })
    .replace(/[^a-zA-Z]/g, '')
    .toUpperCase()
}

function parseStations() {
  const raw = fs.readFileSync(path.join(BASE, '市.txt'), 'utf8')
  const names = raw
    .split(/[，,\s]+/)
    .map(s => s.trim())
    .filter(Boolean)
  const list = []
  const seen = new Set()
  for (let rawName of names) {
    // 重庆在源数据里拆成了「城区 / 郊县」，统一成一个「重庆市」
    let name = rawName
    if (name === '重庆城区') name = '重庆市'
    if (name === '重庆郊县') continue
    if (name === '香港特别行政区') name = '香港'
    if (name === '澳门特别行政区') name = '澳门'
    if (seen.has(name)) continue
    seen.add(name)
    list.push({ name, city: cityOf(name), py: pyOf(cityOf(name)) })
  }
  return list
}

const stations = parseStations()
const stationIndex = new Map(stations.map((s, i) => [s.name, i + 1]))

function sid(name) {
  const id = stationIndex.get(name)
  if (!id) {
    throw new Error(`线路途经站不在站点表中：${name}`)
  }
  return id
}

// ---------------------------------------------------------------- 线路（市级途经站）

/**
 * mileage 为该线路实际运营里程（公里），用于推算运行时间与票价
 */
const LINES = [
  {
    name: '京沪高铁', mileage: 1318,
    stops: ['北京市', '廊坊市', '天津市', '沧州市', '德州市', '济南市', '泰安市', '济宁市', '徐州市',
      '宿州市', '蚌埠市', '滁州市', '南京市', '镇江市', '常州市', '无锡市', '苏州市', '上海市']
  },
  {
    name: '京广高铁', mileage: 2298,
    stops: ['北京市', '保定市', '石家庄市', '邢台市', '邯郸市', '安阳市', '鹤壁市', '新乡市', '郑州市',
      '许昌市', '漯河市', '驻马店市', '信阳市', '武汉市', '咸宁市', '岳阳市', '长沙市', '株洲市',
      '衡阳市', '郴州市', '韶关市', '清远市', '广州市']
  },
  {
    name: '沪昆高铁', mileage: 2252,
    stops: ['上海市', '嘉兴市', '杭州市', '金华市', '衢州市', '上饶市', '鹰潭市', '南昌市', '新余市',
      '宜春市', '萍乡市', '长沙市', '娄底市', '怀化市', '贵阳市', '安顺市', '曲靖市', '昆明市']
  },
  {
    name: '京哈高铁', mileage: 1200,
    stops: ['北京市', '承德市', '朝阳市', '阜新市', '沈阳市', '铁岭市', '四平市', '长春市', '哈尔滨市']
  },
  {
    name: '徐兰高铁', mileage: 1400,
    stops: ['郑州市', '洛阳市', '三门峡市', '渭南市', '西安市', '咸阳市', '宝鸡市', '天水市', '定西市', '兰州市']
  },
  {
    name: '成渝高铁', mileage: 308,
    stops: ['成都市', '资阳市', '内江市', '重庆市']
  },
  {
    name: '广深港高铁', mileage: 140,
    stops: ['广州市', '东莞市', '深圳市', '香港']
  },
  {
    name: '杭深线', mileage: 1500,
    stops: ['杭州市', '绍兴市', '宁波市', '台州市', '温州市', '宁德市', '福州市', '莆田市', '泉州市',
      '厦门市', '漳州市', '潮州市', '汕头市', '揭阳市', '汕尾市', '惠州市', '深圳市']
  },
  {
    name: '京津城际', mileage: 120,
    stops: ['北京市', '廊坊市', '天津市']
  },
  {
    name: '沪宁城际', mileage: 301,
    stops: ['上海市', '苏州市', '无锡市', '常州市', '镇江市', '南京市']
  },
  {
    name: '京张高铁', mileage: 174,
    stops: ['北京市', '张家口市']
  },
  {
    name: '合福高铁', mileage: 850,
    stops: ['合肥市', '铜陵市', '黄山市', '上饶市', '南平市', '宁德市', '福州市']
  },
  {
    name: '宁杭高铁', mileage: 250,
    stops: ['南京市', '常州市', '无锡市', '湖州市', '杭州市']
  },
  {
    name: '济青高铁', mileage: 307,
    stops: ['济南市', '滨州市', '淄博市', '潍坊市', '青岛市']
  },
  {
    name: '西成高铁', mileage: 658,
    stops: ['西安市', '汉中市', '广元市', '绵阳市', '德阳市', '成都市']
  }
]

// ---------------------------------------------------------------- 车次

/** 每车次 8 节车厢：1 商务 / 2-3 一等 / 4-8 二等，每节 100 座 */
const CARRIAGES = [
  { no: 1, type: 1 }, { no: 2, type: 2 }, { no: 3, type: 2 },
  { no: 4, type: 3 }, { no: 5, type: 3 }, { no: 6, type: 3 }, { no: 7, type: 3 }, { no: 8, type: 3 }
]
const SEAT_LETTERS = ['A', 'B', 'C', 'D', 'F'] // 跳过 E，与既有数据一致
const ROWS_PER_CARRIAGE = 20 // 20 排 × 5 座 = 100 座

/** 票价：二等座按里程，一等 / 商务按比例上浮 */
function prices(km) {
  const second = Math.max(20, Math.round((km * 0.46) / 10) * 10)
  const first = Math.round((second * 1.7) / 10) * 10
  const business = Math.round((second * 2.6) / 10) * 10
  return { 1: business, 2: first, 3: second }
}

function pad(n) {
  return String(n).padStart(2, '0')
}

/** 分钟 -> HH:mm:ss */
function toTime(minutes) {
  const total = Math.floor(minutes)
  const h = Math.floor(total / 60) % 24
  const m = total % 60
  return `${pad(h)}:${pad(m)}:00`
}

/**
 * 生成一个班次：时刻表 + 车厢 + 座位 + 库存
 * @returns {{stops:Array, duration:number, distance:number}}
 */
function buildTrip(line, stops, departMinutes, kmpMin, stopMinutes) {
  const segCount = stops.length - 1
  const segKm = line.mileage / segCount
  const result = { stops: [], duration: 0, distance: 0 }
  let clock = departMinutes
  let distance = 0
  stops.forEach((name, idx) => {
    const stationId = sid(name)
    if (idx > 0) {
      const run = Math.max(3, Math.round(segKm / kmpMin))
      clock += run
      distance += Math.round(segKm)
    }
    const arrive = idx === 0 ? null : toTime(clock)
    // 首站发车即走；中间站到站后停留；终点站只记录到达
    if (idx > 0 && idx < stops.length - 1) {
      clock += stopMinutes
    }
    const depart = toTime(clock)
    result.stops.push({
      stationId,
      stationName: name,
      stopOrder: idx + 1,
      arriveTime: arrive,
      departTime: depart,
      stopMinutes: idx === 0 || idx === stops.length - 1 ? 0 : stopMinutes,
      distanceKm: Math.round(distance)
    })
  })
  result.duration = clock - departMinutes
  result.distance = Math.round(distance)
  return result
}

// ---------------------------------------------------------------- 组装 SQL

const out = []
out.push('-- 由 md/gen_city_data.js 自动生成：站点替换为市级城市后的完整业务数据')
out.push(`-- 生成时间：${new Date().toLocaleString('zh-CN')}    班次日期：${dateOffset(0)} ~ ${dateOffset(DAYS - 1)}`)
out.push('SET NAMES utf8mb4;')
out.push('SET FOREIGN_KEY_CHECKS = 0;')
out.push('')
out.push('-- 清理旧数据（已备份到 md/backup_20260928.sql）')
;['t_order_log', 't_order_change', 't_seckill_record', 't_order', 't_seat', 't_train_stock',
  't_train_stop', 't_carriage', 't_train', 't_line_station', 't_line', 't_station']
  .forEach(t => {
    out.push(`DELETE FROM ${t};`)
    out.push(`ALTER TABLE ${t} AUTO_INCREMENT = 1;`)
  })
out.push('')

function esc(s) {
  return String(s).replace(/'/g, "''")
}

function insertBatch(table, columns, rows) {
  if (!rows.length) return
  const size = 500
  for (let i = 0; i < rows.length; i += size) {
    const chunk = rows.slice(i, i + size)
    const values = chunk.map(r => `(${r.map(v => (v === null ? 'NULL' : `'${esc(v)}'`)).join(',')})`).join(',\n')
    out.push(`INSERT INTO ${table} (${columns.join(',')}) VALUES\n${values};`)
  }
  out.push('')
}

// 1. 站点
insertBatch('t_station', ['id', 'station_name', 'city', 'py_code', 'status'],
  stations.map((s, i) => [i + 1, s.name, s.city, s.py, 1]))

// 2. 线路 + 途经站
const lineRows = []
const lineStationRows = []
LINES.forEach((line, i) => {
  const lineId = i + 1
  lineRows.push([lineId, line.name, sid(line.stops[0]), line.stops[0],
    sid(line.stops[line.stops.length - 1]), line.stops[line.stops.length - 1], 1])
  line.stops.forEach((name, idx) => {
    lineStationRows.push([lineId, sid(name), name, idx + 1])
  })
})
insertBatch('t_line', ['id', 'line_name', 'from_station_id', 'from_station_name', 'to_station_id', 'to_station_name', 'status'], lineRows)
insertBatch('t_line_station', ['line_id', 'station_id', 'station_name', 'stop_order'], lineStationRows)

// 3. 车次 / 时刻表 / 车厢 / 座位 / 库存
const trainRows = []
const stopRows = []
const carriageRows = []
const seatRows = []
const stockRows = []

let trainId = 0
for (let day = 0; day < DAYS; day++) {
  const departDate = dateOffset(day)
LINES.forEach((line, li) => {
  const forward = line.stops
  const backward = [...line.stops].reverse()
  // 每条线路 6 个车次号：4 个高铁（双向各 2）+ 2 个动车（双向各 1）
  const plans = [
    { no: `G${li + 1}01`, type: '高铁', dir: 'down', start: 6 * 60 + 30 },
    { no: `G${li + 1}02`, type: '高铁', dir: 'down', start: 13 * 60 + 0 },
    { no: `G${li + 1}03`, type: '高铁', dir: 'up', start: 8 * 60 + 20 },
    { no: `G${li + 1}04`, type: '高铁', dir: 'up', start: 16 * 60 + 40 },
    { no: `D${li + 1}01`, type: '动车', dir: 'down', start: 9 * 60 + 10 },
    { no: `D${li + 1}02`, type: '动车', dir: 'up', start: 18 * 60 + 20 }
  ]
  for (const plan of plans) {
    trainId++
    const stops = plan.dir === 'down' ? forward : backward
    // 高铁均速 250km/h，动车 200km/h
    const kmpMin = plan.type === '高铁' ? 250 / 60 : 200 / 60
    const trip = buildTrip(line, stops, plan.start, kmpMin, 2)
    const from = stops[0]
    const to = stops[stops.length - 1]
    const last = trip.stops[trip.stops.length - 1]

    trainRows.push([trainId, plan.no, plan.type, sid(from), from, sid(to), to, departDate,
      toTime(plan.start), last.arriveTime, trip.duration, 1])

    trip.stops.forEach(s => {
      stopRows.push([trainId, s.stationId, s.stationName, s.stopOrder,
        s.arriveTime, s.departTime, s.stopMinutes, s.distanceKm])
    })

    // 车厢 + 座位
    const p = prices(line.mileage)
    const typeCount = { 1: 0, 2: 0, 3: 0 }
    for (const c of CARRIAGES) {
      carriageRows.push([trainId, c.no, c.type, ROWS_PER_CARRIAGE * SEAT_LETTERS.length])
      for (let row = 1; row <= ROWS_PER_CARRIAGE; row++) {
        for (const letter of SEAT_LETTERS) {
          seatRows.push([trainId, c.type, c.no, `${pad(row)}${letter}`, 0, null, 0])
          typeCount[c.type]++
        }
      }
    }
    stockRows.push([trainId, 1, typeCount[1], typeCount[1], p[1], 0])
    stockRows.push([trainId, 2, typeCount[2], typeCount[2], p[2], 0])
    stockRows.push([trainId, 3, typeCount[3], typeCount[3], p[3], 0])
  }
})
}

insertBatch('t_train',
  ['id', 'train_no', 'train_type', 'from_station_id', 'from_station_name', 'to_station_id', 'to_station_name',
    'depart_date', 'depart_time', 'arrive_time', 'duration_minutes', 'status'], trainRows)
insertBatch('t_train_stop',
  ['train_id', 'station_id', 'station_name', 'stop_order', 'arrive_time', 'depart_time', 'stop_minutes', 'distance_km'], stopRows)
insertBatch('t_carriage', ['train_id', 'carriage_no', 'seat_type', 'seat_count'], carriageRows)
insertBatch('t_seat', ['train_id', 'seat_type', 'carriage_no', 'seat_no', 'status', 'order_no', 'version'], seatRows)
insertBatch('t_train_stock', ['train_id', 'seat_type', 'total_count', 'available_count', 'price', 'version'], stockRows)

out.push('SET FOREIGN_KEY_CHECKS = 1;')

const sql = out.join('\n')
const target = path.join(BASE, 'city_data.sql')
fs.writeFileSync(target, sql, 'utf8')

console.log(`站点：${stations.length}`)
console.log(`线路：${LINES.length}`)
console.log(`车次（${dateOffset(0)} ~ ${dateOffset(DAYS - 1)}）：${trainRows.length}`)
console.log(`时刻表：${stopRows.length}   车厢：${carriageRows.length}   座位：${seatRows.length}   库存：${stockRows.length}`)
console.log(`已生成：${target}`)
