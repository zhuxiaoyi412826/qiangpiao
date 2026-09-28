/**
 * 数据导入脚本（生成 SQL，不直接连库）
 *
 * 1) 站点：解析 md/数据.js（12306 station_names 全量车站）→ scripts/sql/stations_import.sql
 * 2) 车次：按内置「公开高铁线路」配置 → scripts/sql/trains_import.sql
 *    （车次模板 + 席别库存 + 座位图 + 车厢 + 线路/途经站 + 时刻表）
 *
 * 说明：只需要插入「今天」的模板班次，后端 TrainScheduleTask 会以它为模板
 *       自动生成未来 30 天班次（库存、座位一并复制）。
 *
 * 用法：node scripts/import-data.js
 */
const fs = require('fs')
const path = require('path')

const ROOT = path.resolve(__dirname, '..')
const OUT_DIR = path.join(ROOT, 'scripts', 'sql')
const STATION_FILE = path.join(ROOT, 'md', '数据.js')

const esc = s => String(s == null ? '' : s).replace(/'/g, "''")

// =====================================================================
// 一、站点：解析 station_names
// =====================================================================
function parseStations() {
  const raw = fs.readFileSync(STATION_FILE, 'utf8')
  const matched = raw.match(/station_names\s*=\s*'([^']*)'/)
  if (!matched) {
    throw new Error('未在 md/数据.js 中找到 station_names 变量')
  }
  const map = new Map()
  matched[1].split('@').filter(Boolean).forEach(seg => {
    const f = seg.split('|')
    // code | 站名 | 电报码 | 拼音全拼 | 拼音简码 | 序号 | 城市码 | 城市
    const name = (f[1] || '').trim()
    if (!name) {
      return
    }
    const city = (f[7] || '').trim() || name
    const py = (f[4] || f[3] || '').trim().toUpperCase()
    if (!map.has(name)) {
      map.set(name, { name, city, py })
    }
  })
  return map
}

function buildStationSql(stations) {
  const lines = []
  lines.push('-- =============================================================')
  lines.push('--  站点导入：来源 md/数据.js（12306 station_names 全量车站）')
  lines.push('--  幂等：INSERT IGNORE + ON DUPLICATE KEY UPDATE')
  lines.push('--  生成时间：' + new Date().toISOString())
  lines.push('-- =============================================================')
  lines.push('USE qiangpiao;')
  lines.push('')

  const rows = [...stations.values()]
  const BATCH = 500
  for (let i = 0; i < rows.length; i += BATCH) {
    const part = rows.slice(i, i + BATCH)
    lines.push('INSERT IGNORE INTO t_station (station_name, city, py_code) VALUES')
    lines.push(part.map((s, idx) =>
      `('${esc(s.name)}', '${esc(s.city)}', '${esc(s.py)}')${idx === part.length - 1 ? '' : ','}`
    ).join('\n'))
    lines.push('ON DUPLICATE KEY UPDATE city = VALUES(city), py_code = VALUES(py_code);')
    lines.push('')
  }
  return lines.join('\n')
}

// =====================================================================
// 二、车次：公开高铁线路配置
// =====================================================================
// stations：线路途经站（按顺序，含首末站）
// services：车次，from/to 必须是 stations 中的站，minutes=全程历时，price2=二等座票价
const LINES = [
  {
    name: '京沪高铁',
    stations: ['北京南', '廊坊', '天津南', '德州东', '济南西', '泰安', '曲阜东', '徐州东', '蚌埠南', '南京南', '镇江南', '无锡东', '苏州北', '上海虹桥'],
    services: [
      { no: 'G1', from: '北京南', to: '上海虹桥', depart: '06:20', minutes: 268, price2: 553 },
      { no: 'G3', from: '北京南', to: '上海虹桥', depart: '07:00', minutes: 268, price2: 553 },
      { no: 'G5', from: '北京南', to: '上海虹桥', depart: '07:30', minutes: 275, price2: 553 },
      { no: 'G7', from: '北京南', to: '上海虹桥', depart: '08:00', minutes: 268, price2: 553 },
      { no: 'G9', from: '北京南', to: '上海虹桥', depart: '09:00', minutes: 272, price2: 553 },
      { no: 'G11', from: '北京南', to: '上海虹桥', depart: '10:00', minutes: 275, price2: 553 },
      { no: 'G2', from: '上海虹桥', to: '北京南', depart: '06:30', minutes: 268, price2: 553 },
      { no: 'G4', from: '上海虹桥', to: '北京南', depart: '07:10', minutes: 272, price2: 553 },
      { no: 'G6', from: '上海虹桥', to: '北京南', depart: '08:00', minutes: 268, price2: 553 },
      { no: 'G8', from: '上海虹桥', to: '北京南', depart: '09:00', minutes: 275, price2: 553 },
      { no: 'G10', from: '上海虹桥', to: '北京南', depart: '10:00', minutes: 268, price2: 553 },
      { no: 'G12', from: '上海虹桥', to: '北京南', depart: '11:00', minutes: 272, price2: 553 },
      { no: 'G101', from: '北京南', to: '济南西', depart: '07:00', minutes: 82, price2: 184 },
      { no: 'G102', from: '济南西', to: '北京南', depart: '08:30', minutes: 82, price2: 184 },
      { no: 'G105', from: '南京南', to: '上海虹桥', depart: '08:00', minutes: 70, price2: 139.5 },
      { no: 'G106', from: '上海虹桥', to: '南京南', depart: '09:30', minutes: 70, price2: 139.5 },
      { no: 'G107', from: '济南西', to: '上海虹桥', depart: '12:00', minutes: 185, price2: 397 },
      { no: 'G108', from: '上海虹桥', to: '济南西', depart: '13:00', minutes: 185, price2: 397 }
    ]
  },
  {
    name: '京广高铁',
    stations: ['北京西', '保定东', '石家庄', '郑州东', '武汉', '长沙南', '广州南'],
    services: [
      { no: 'G71', from: '北京西', to: '广州南', depart: '07:00', minutes: 480, price2: 862 },
      { no: 'G79', from: '北京西', to: '广州南', depart: '10:00', minutes: 480, price2: 862 },
      { no: 'G72', from: '广州南', to: '北京西', depart: '08:00', minutes: 480, price2: 862 },
      { no: 'G80', from: '广州南', to: '北京西', depart: '10:30', minutes: 480, price2: 862 },
      { no: 'G501', from: '北京西', to: '武汉', depart: '08:00', minutes: 270, price2: 520.5 },
      { no: 'G502', from: '武汉', to: '北京西', depart: '09:00', minutes: 270, price2: 520.5 },
      { no: 'G1003', from: '武汉', to: '广州南', depart: '12:00', minutes: 240, price2: 463.5 },
      { no: 'G1004', from: '广州南', to: '武汉', depart: '13:00', minutes: 240, price2: 463.5 },
      { no: 'G1101', from: '长沙南', to: '广州南', depart: '14:00', minutes: 140, price2: 314 },
      { no: 'G1102', from: '广州南', to: '长沙南', depart: '15:00', minutes: 140, price2: 314 }
    ]
  },
  {
    name: '沪昆高铁',
    stations: ['上海虹桥', '杭州东', '南昌西', '长沙南', '贵阳北', '昆明南'],
    services: [
      { no: 'G1371', from: '上海虹桥', to: '昆明南', depart: '06:40', minutes: 636, price2: 879 },
      { no: 'G1372', from: '昆明南', to: '上海虹桥', depart: '07:20', minutes: 636, price2: 879 },
      { no: 'G1301', from: '上海虹桥', to: '长沙南', depart: '08:15', minutes: 330, price2: 597 },
      { no: 'G1302', from: '长沙南', to: '上海虹桥', depart: '09:00', minutes: 330, price2: 597 },
      { no: 'G1401', from: '杭州东', to: '南昌西', depart: '10:00', minutes: 140, price2: 263.5 },
      { no: 'G1402', from: '南昌西', to: '杭州东', depart: '11:00', minutes: 140, price2: 263.5 }
    ]
  },
  {
    name: '京哈高铁',
    stations: ['北京朝阳', '承德南', '沈阳北', '长春西', '哈尔滨西'],
    services: [
      { no: 'G901', from: '北京朝阳', to: '哈尔滨西', depart: '06:30', minutes: 270, price2: 541 },
      { no: 'G902', from: '哈尔滨西', to: '北京朝阳', depart: '07:00', minutes: 270, price2: 541 },
      { no: 'G3601', from: '北京朝阳', to: '沈阳北', depart: '08:00', minutes: 150, price2: 336 },
      { no: 'G3602', from: '沈阳北', to: '北京朝阳', depart: '09:00', minutes: 150, price2: 336 }
    ]
  },
  {
    name: '徐兰高铁',
    stations: ['郑州东', '洛阳龙门', '西安北', '宝鸡南', '兰州西'],
    services: [
      { no: 'G2005', from: '郑州东', to: '西安北', depart: '07:00', minutes: 120, price2: 239 },
      { no: 'G2006', from: '西安北', to: '郑州东', depart: '08:00', minutes: 120, price2: 239 },
      { no: 'G2007', from: '西安北', to: '兰州西', depart: '09:00', minutes: 180, price2: 262.5 },
      { no: 'G2008', from: '兰州西', to: '西安北', depart: '10:00', minutes: 180, price2: 262.5 }
    ]
  },
  {
    name: '成渝高铁',
    stations: ['成都东', '资阳北', '内江北', '重庆北'],
    services: [
      { no: 'G8501', from: '成都东', to: '重庆北', depart: '07:00', minutes: 90, price2: 154 },
      { no: 'G8502', from: '重庆北', to: '成都东', depart: '08:00', minutes: 90, price2: 154 },
      { no: 'G8503', from: '成都东', to: '重庆北', depart: '12:00', minutes: 90, price2: 154 },
      { no: 'G8504', from: '重庆北', to: '成都东', depart: '13:00', minutes: 90, price2: 154 }
    ]
  },
  {
    name: '广深港高铁',
    stations: ['广州南', '虎门', '光明城', '深圳北'],
    services: [
      { no: 'G6501', from: '广州南', to: '深圳北', depart: '07:00', minutes: 30, price2: 74.5 },
      { no: 'G6502', from: '深圳北', to: '广州南', depart: '08:00', minutes: 30, price2: 74.5 },
      { no: 'G6503', from: '广州南', to: '深圳北', depart: '12:00', minutes: 30, price2: 74.5 },
      { no: 'G6504', from: '深圳北', to: '广州南', depart: '13:00', minutes: 30, price2: 74.5 }
    ]
  },
  {
    name: '杭深线',
    stations: ['杭州东', '宁波', '台州西', '温州南', '福州南', '厦门北', '深圳北'],
    services: [
      { no: 'D3101', from: '杭州东', to: '深圳北', depart: '07:30', minutes: 480, price2: 560, type: '动车' },
      { no: 'D3102', from: '深圳北', to: '杭州东', depart: '08:00', minutes: 480, price2: 560, type: '动车' },
      { no: 'D3201', from: '杭州东', to: '福州南', depart: '09:00', minutes: 210, price2: 261, type: '动车' },
      { no: 'D3202', from: '福州南', to: '杭州东', depart: '10:00', minutes: 210, price2: 261, type: '动车' }
    ]
  },
  {
    name: '京津城际',
    stations: ['北京南', '武清', '天津'],
    services: [
      { no: 'C2001', from: '北京南', to: '天津', depart: '07:00', minutes: 30, price2: 54.5, type: '城际' },
      { no: 'C2002', from: '天津', to: '北京南', depart: '07:30', minutes: 30, price2: 54.5, type: '城际' },
      { no: 'C2003', from: '北京南', to: '天津', depart: '12:00', minutes: 30, price2: 54.5, type: '城际' },
      { no: 'C2004', from: '天津', to: '北京南', depart: '12:30', minutes: 30, price2: 54.5, type: '城际' }
    ]
  },
  {
    name: '沪宁城际',
    stations: ['上海虹桥', '苏州', '无锡', '常州', '南京南'],
    services: [
      { no: 'G7001', from: '上海虹桥', to: '南京南', depart: '07:00', minutes: 75, price2: 139.5 },
      { no: 'G7002', from: '南京南', to: '上海虹桥', depart: '08:00', minutes: 75, price2: 139.5 },
      { no: 'G7003', from: '上海虹桥', to: '南京南', depart: '12:00', minutes: 75, price2: 139.5 },
      { no: 'G7004', from: '南京南', to: '上海虹桥', depart: '13:00', minutes: 75, price2: 139.5 }
    ]
  },
  {
    name: '京张高铁',
    stations: ['北京北', '清河', '张家口'],
    services: [
      { no: 'G8801', from: '北京北', to: '张家口', depart: '08:00', minutes: 60, price2: 87 },
      { no: 'G8802', from: '张家口', to: '北京北', depart: '09:00', minutes: 60, price2: 87 }
    ]
  },
  {
    name: '合福高铁',
    stations: ['合肥南', '黄山北', '南平市', '福州'],
    services: [
      { no: 'G1601', from: '合肥南', to: '福州', depart: '08:00', minutes: 180, price2: 357 },
      { no: 'G1602', from: '福州', to: '合肥南', depart: '09:00', minutes: 180, price2: 357 }
    ]
  },
  {
    name: '宁杭高铁',
    stations: ['南京南', '溧阳', '宜兴', '湖州', '杭州东'],
    services: [
      { no: 'G7601', from: '南京南', to: '杭州东', depart: '08:00', minutes: 70, price2: 118.5 },
      { no: 'G7602', from: '杭州东', to: '南京南', depart: '09:00', minutes: 70, price2: 118.5 }
    ]
  },
  {
    name: '济青高铁',
    stations: ['济南西', '淄博北', '潍坊北', '青岛北'],
    services: [
      { no: 'G6901', from: '济南西', to: '青岛北', depart: '08:00', minutes: 150, price2: 178 },
      { no: 'G6902', from: '青岛北', to: '济南西', depart: '09:00', minutes: 150, price2: 178 }
    ]
  },
  {
    name: '西成高铁',
    stations: ['西安北', '汉中', '广元', '成都东'],
    services: [
      { no: 'G2201', from: '西安北', to: '成都东', depart: '08:00', minutes: 210, price2: 263 },
      { no: 'G2202', from: '成都东', to: '西安北', depart: '09:00', minutes: 210, price2: 263 }
    ]
  }
]

/** 座位数：商务座 1 车厢 100，一等座 2 车厢 200，二等座 5 车厢 500（共 800，与 data.sql 一致） */
const SEAT_LAYOUT = [
  { seatType: 1, total: 100, rate: 3.1 },
  { seatType: 2, total: 200, rate: 1.6 },
  { seatType: 3, total: 500, rate: 1 }
]

function toTime(baseMinutes, plus) {
  const m = baseMinutes + plus
  const day = Math.floor(m / 1440)
  const rest = ((m % 1440) + 1440) % 1440
  const hh = String(Math.floor(rest / 60)).padStart(2, '0')
  const mm = String(rest % 60).padStart(2, '0')
  return { time: `${hh}:${mm}:00`, nextDay: day > 0 }
}

function hm2min(t) {
  const [h, m] = t.split(':').map(Number)
  return h * 60 + m
}

function buildTrainSql(stationSet) {
  const warnings = []
  const services = []
  LINES.forEach(line => {
    const missing = line.stations.filter(s => !stationSet.has(s))
    if (missing.length) {
      warnings.push(`线路【${line.name}】缺少站点：${missing.join('、')}`)
    }
    line.services.forEach(svc => {
      if (!stationSet.has(svc.from) || !stationSet.has(svc.to)) {
        warnings.push(`车次 ${svc.no} 站点不在站点库：${svc.from} -> ${svc.to}`)
        return
      }
      services.push({ ...svc, lineName: line.name, lineStations: line.stations, type: svc.type || '高铁' })
    })
  })

  const lines = []
  lines.push('-- =============================================================')
  lines.push('--  车次导入：公开高铁线路（车次模板 + 库存 + 座位 + 车厢 + 线路/时刻表）')
  lines.push('--  只插入「今天」的模板班次，TrainScheduleTask 会自动复制出未来 30 天班次')
  lines.push('--  幂等：全部使用 INSERT IGNORE / NOT EXISTS，可重复执行')
  lines.push('--  生成时间：' + new Date().toISOString())
  lines.push('-- =============================================================')
  lines.push('USE qiangpiao;')
  lines.push('')

  // ---------- 1. 车次模板（今天） ----------
  lines.push('-- ---------- 1. 车次模板（depart_date = CURDATE()） ----------')
  lines.push('INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,')
  lines.push('                           to_station_id, to_station_name, depart_date, depart_time,')
  lines.push('                           arrive_time, duration_minutes, status)')
  lines.push('VALUES')
  const trainRows = services.map((s, idx) => {
    const dep = hm2min(s.depart)
    const arr = toTime(dep, s.minutes)
    const values = [
      `'${esc(s.no)}'`,
      `'${esc(s.type)}'`,
      `(SELECT id FROM t_station WHERE station_name = '${esc(s.from)}')`,
      `'${esc(s.from)}'`,
      `(SELECT id FROM t_station WHERE station_name = '${esc(s.to)}')`,
      `'${esc(s.to)}'`,
      'CURDATE()',
      `'${s.depart}:00'`,
      `'${arr.time}'`,
      String(s.minutes),
      '1'
    ]
    return `(${values.join(', ')})${idx === services.length - 1 ? '' : ','}`
  })
  lines.push(trainRows.join('\n'))
  lines.push(';')
  lines.push('')

  const noList = services.map(s => `'${esc(s.no)}'`).join(', ')

  // ---------- 2. 席别库存 ----------
  lines.push('-- ---------- 2. 席别库存（商务座 100 / 一等座 200 / 二等座 500） ----------')
  services.forEach(s => {
    SEAT_LAYOUT.forEach(l => {
      const price = Math.round(s.price2 * l.rate)
      lines.push(
        `INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)\n` +
        `SELECT t.id, ${l.seatType}, ${l.total}, ${l.total}, ${price}.00, 0\n` +
        `FROM t_train t WHERE t.train_no = '${esc(s.no)}' AND t.depart_date = CURDATE();`
      )
    })
  })
  lines.push('')

  // ---------- 3. 座位图 ----------
  lines.push('-- ---------- 3. 座位图（1 车厢商务 / 2-3 车厢一等 / 4-8 车厢二等，每车厢 20 排 × 5 座） ----------')
  lines.push('CREATE TABLE IF NOT EXISTS t_seq (n INT PRIMARY KEY);')
  lines.push('INSERT IGNORE INTO t_seq (n) VALUES ' +
    Array.from({ length: 20 }, (_, i) => `(${i + 1})`).join(',') + ';')
  lines.push('')
  lines.push('INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)')
  lines.push('SELECT t.id, ty.seat_type, c.n, CONCAT(LPAD(r.n, 2, \'0\'), sl.letter), 0, 0')
  lines.push('FROM t_train t')
  lines.push('         CROSS JOIN (SELECT 1 AS seat_type UNION ALL SELECT 2 UNION ALL SELECT 3) ty')
  lines.push('         CROSS JOIN (SELECT n FROM t_seq WHERE n <= 8) c')
  lines.push('         CROSS JOIN (SELECT n FROM t_seq WHERE n <= 20) r')
  lines.push('         CROSS JOIN (SELECT \'A\' letter UNION ALL SELECT \'B\' UNION ALL SELECT \'C\'')
  lines.push('                     UNION ALL SELECT \'D\' UNION ALL SELECT \'F\') sl')
  lines.push(`WHERE t.train_no IN (${noList}) AND t.depart_date = CURDATE()`)
  lines.push('  AND (   (ty.seat_type = 1 AND c.n = 1)')
  lines.push('       OR (ty.seat_type = 2 AND c.n IN (2, 3))')
  lines.push('       OR (ty.seat_type = 3 AND c.n BETWEEN 4 AND 8));')
  lines.push('')

  // ---------- 4. 车厢 ----------
  lines.push('-- ---------- 4. 车厢表 ----------')
  lines.push('INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)')
  lines.push('SELECT t.id, c.n, CASE WHEN c.n = 1 THEN 1 WHEN c.n <= 3 THEN 2 ELSE 3 END, 100')
  lines.push('FROM t_train t')
  lines.push('         CROSS JOIN (SELECT n FROM t_seq WHERE n <= 8) c')
  lines.push(`WHERE t.train_no IN (${noList}) AND t.depart_date = CURDATE();`)
  lines.push('')

  // ---------- 5. 线路与途经站 ----------
  lines.push('-- ---------- 5. 线路（t_line + t_line_station） ----------')
  LINES.forEach(line => {
    const from = line.stations[0]
    const to = line.stations[line.stations.length - 1]
    lines.push(`INSERT INTO t_line (line_name, from_station_id, from_station_name, to_station_id, to_station_name, status)`)
    lines.push(`SELECT '${esc(line.name)}',`)
    lines.push(`       (SELECT id FROM t_station WHERE station_name = '${esc(from)}'), '${esc(from)}',`)
    lines.push(`       (SELECT id FROM t_station WHERE station_name = '${esc(to)}'), '${esc(to)}', 1`)
    lines.push(`WHERE NOT EXISTS (SELECT 1 FROM t_line l WHERE l.line_name = '${esc(line.name)}');`)
    lines.push('INSERT IGNORE INTO t_line_station (line_id, station_id, station_name, stop_order)')
    lines.push(`SELECT l.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name), tmp.station_name, tmp.stop_order`)
    lines.push('FROM t_line l')
    lines.push('         JOIN (')
    lines.push(line.stations.map((s, i) => `             SELECT '${esc(s)}' AS station_name, ${i + 1} AS stop_order`).join(' UNION ALL '))
    lines.push('             ) tmp')
    lines.push(`WHERE l.line_name = '${esc(line.name)}';`)
  })
  lines.push('')

  // ---------- 6. 车次时刻表 ----------
  lines.push('-- ---------- 6. 车次时刻表（t_train_stop） ----------')
  services.forEach(s => {
    const idxFrom = s.lineStations.indexOf(s.from)
    const idxTo = s.lineStations.indexOf(s.to)
    if (idxFrom < 0 || idxTo < 0 || idxTo <= idxFrom) {
      return
    }
    const stops = s.lineStations.slice(idxFrom, idxTo + 1)
    const segCount = stops.length - 1
    const dep = hm2min(s.depart)
    const rows = []
    let cursor = dep
    stops.forEach((station, i) => {
      const order = i + 1
      if (i === 0) {
        rows.push({ station, order, arrive: null, depart: `${s.depart}:00`, stop: 0 })
        return
      }
      const seg = Math.max(5, Math.round(s.minutes / segCount))
      cursor += seg
      let arrive = toTime(dep, cursor - dep).time
      if (i === stops.length - 1) {
        arrive = toTime(dep, s.minutes).time
      }
      const depart = i === stops.length - 1 ? null : toTime(dep, cursor - dep + 2).time
      rows.push({ station, order, arrive, depart, stop: 2 })
      cursor += 2
    })
    lines.push(`INSERT IGNORE INTO t_train_stop (train_id, station_id, station_name, stop_order, arrive_time, depart_time, stop_minutes)`)
    lines.push(`SELECT t.id, (SELECT id FROM t_station WHERE station_name = tmp.station_name),`)
    lines.push(`       tmp.station_name, tmp.stop_order, tmp.arrive_time, tmp.depart_time, tmp.stop_minutes`)
    lines.push('FROM t_train t')
    lines.push('         JOIN (')
    rows.forEach((r, i) => {
      const arrive = r.arrive ? `'${r.arrive}'` : 'NULL'
      const depart = r.depart ? `'${r.depart}'` : 'NULL'
      const cols = `'${esc(r.station)}', ${r.order}, ${arrive}, ${depart}, ${r.stop}`
      const sel = i === 0
        ? `SELECT '${esc(r.station)}' AS station_name, ${r.order} AS stop_order, ${arrive} AS arrive_time, ${depart} AS depart_time, ${r.stop} AS stop_minutes`
        : `SELECT ${cols}`
      lines.push('             ' + sel + (i === rows.length - 1 ? '' : ' UNION ALL'))
    })
    lines.push('             ) tmp')
    lines.push(`WHERE t.train_no = '${esc(s.no)}' AND t.depart_date = CURDATE();`)
  })
  lines.push('')

  // ---------- 7. 库存校准 ----------
  lines.push('-- ---------- 7. 校准库存与真实可售座位数一致 ----------')
  lines.push('UPDATE t_train_stock st')
  lines.push('    JOIN (SELECT train_id, seat_type, COUNT(*) AS cnt FROM t_seat WHERE status = 0')
  lines.push('          GROUP BY train_id, seat_type) s')
  lines.push('    ON s.train_id = st.train_id AND s.seat_type = st.seat_type')
  lines.push('SET st.total_count = s.cnt, st.available_count = s.cnt;')
  lines.push('')

  return { sql: lines.join('\n'), warnings, count: services.length }
}

/**
 * 未来日期班次：把「今天」的模板复制出后续 N 天（与 TrainScheduleTask 逻辑一致，
 * 这里让导入后可立即查询未来日期，定时任务再补齐到 30 天）。
 */
function buildFutureSql(days) {
  const lines = []
  lines.push('-- =============================================================')
  lines.push('--  未来日期班次复制（模板 → 后续 ' + days + ' 天，含列车/库存/座位/车厢）')
  lines.push('--  生成时间：' + new Date().toISOString())
  lines.push('-- =============================================================')
  lines.push('USE qiangpiao;')
  lines.push('')
  for (let offset = 1; offset <= days; offset++) {
    const date = `CURDATE() + INTERVAL ${offset} DAY`
    lines.push(`-- ---------- D+${offset} ----------`)
    lines.push(`INSERT IGNORE INTO t_train (train_no, train_type, from_station_id, from_station_name,`)
    lines.push(`                           to_station_id, to_station_name, depart_date, depart_time,`)
    lines.push(`                           arrive_time, duration_minutes, status)`)
    lines.push(`SELECT t.train_no, t.train_type, t.from_station_id, t.from_station_name,`)
    lines.push(`       t.to_station_id, t.to_station_name, ${date}, t.depart_time, t.arrive_time,`)
    lines.push(`       t.duration_minutes, 1`)
    lines.push(`FROM t_train t`)
    lines.push(`WHERE t.depart_date = CURDATE() AND t.status = 1`)
    lines.push(`  AND NOT EXISTS (SELECT 1 FROM t_train x WHERE x.train_no = t.train_no AND x.depart_date = ${date});`)
    lines.push('')
    lines.push(`INSERT IGNORE INTO t_train_stock (train_id, seat_type, total_count, available_count, price, version)`)
    lines.push(`SELECT nt.id, s.seat_type, s.total_count, s.available_count, s.price, 0`)
    lines.push(`FROM t_train nt`)
    lines.push(`         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()`)
    lines.push(`         JOIN t_train_stock s ON s.train_id = tp.id`)
    lines.push(`WHERE nt.depart_date = ${date};`)
    lines.push('')
    lines.push(`INSERT IGNORE INTO t_seat (train_id, seat_type, carriage_no, seat_no, status, version)`)
    lines.push(`SELECT nt.id, s.seat_type, s.carriage_no, s.seat_no, 0, 0`)
    lines.push(`FROM t_train nt`)
    lines.push(`         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()`)
    lines.push(`         JOIN t_seat s ON s.train_id = tp.id`)
    lines.push(`WHERE nt.depart_date = ${date};`)
    lines.push('')
    lines.push(`INSERT IGNORE INTO t_carriage (train_id, carriage_no, seat_type, seat_count)`)
    lines.push(`SELECT nt.id, c.carriage_no, c.seat_type, c.seat_count`)
    lines.push(`FROM t_train nt`)
    lines.push(`         JOIN t_train tp ON tp.train_no = nt.train_no AND tp.depart_date = CURDATE()`)
    lines.push(`         JOIN t_carriage c ON c.train_id = tp.id`)
    lines.push(`WHERE nt.depart_date = ${date};`)
    lines.push('')
  }
  return lines.join('\n')
}

// =====================================================================
// 主流程
// =====================================================================
function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true })

  const stations = parseStations()
  const stationSql = buildStationSql(stations)
  const stationOut = path.join(OUT_DIR, 'stations_import.sql')
  fs.writeFileSync(stationOut, stationSql, 'utf8')

  const { sql: trainSql, warnings, count } = buildTrainSql(stations)
  const trainOut = path.join(OUT_DIR, 'trains_import.sql')
  fs.writeFileSync(trainOut, trainSql, 'utf8')

  const futureSql = buildFutureSql(13)
  const futureOut = path.join(OUT_DIR, 'future_dates_import.sql')
  fs.writeFileSync(futureOut, futureSql, 'utf8')

  console.log(`站点解析：${stations.size} 个 → ${stationOut}`)
  console.log(`车次生成：${count} 个（${LINES.length} 条线路） → ${trainOut}`)
  console.log(`未来班次：13 天 → ${futureOut}`)
  if (warnings.length) {
    console.log('警告：')
    warnings.forEach(w => console.log('  - ' + w))
  }
}

main()
