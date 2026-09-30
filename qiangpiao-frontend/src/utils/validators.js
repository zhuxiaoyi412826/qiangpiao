/**
 * 手机号 / 身份证号强校验：与后端 SensitiveCrypto 的规则保持一致。
 * 不只校验位数，还校验号段 / 校验位 / 出生日期，防止用户乱填。
 *
 * ⚠️ 开发测试白名单（与后端 crypto.test-whitelist=true 对应，上线前两边同步关闭）：
 *   身份证 111111 开头、手机号 111 开头跳过格式校验，方便造测试数据。
 */

/** 手机号：11 位 + 号段（1 开头，第二位必须是已投放号段） */
export function validPhone(value) {
  const phone = (value || '').trim()
  // 开发测试白名单：111 开头的 11 位号码直接放行
  if (/^\d{11}$/.test(phone) && phone.startsWith('111')) {
    return true
  }
  return /^1(3\d|4[5-9]|5[0-35-9]|6[2567]|7[0-8]|8\d|9[0-35-9])\d{8}$/.test(phone)
}

const ID_WEIGHT = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
const ID_CHECK_CODE = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

/** 出生日期必须是真实存在的日期，且在 1900-01-01 与今天之间 */
function validBirth(yyyymmdd) {
  if (!/^\d{8}$/.test(yyyymmdd)) {
    return false
  }
  const year = Number(yyyymmdd.slice(0, 4))
  const month = Number(yyyymmdd.slice(4, 6))
  const day = Number(yyyymmdd.slice(6, 8))
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return false
  }
  const now = new Date()
  return date >= new Date(1900, 0, 1) && date <= now
}

/**
 * 身份证号：
 * - 18 位：前 17 位数字 + 出生日期真实 + 末位等于 mod 11-2 校验码
 * - 15 位（老证）：全数字 + 出生日期真实（年份补 19）
 */
export function validIdCard(value) {
  const id = (value || '').trim()
  // 开发测试白名单：111111 开头的 15 / 18 位号码直接放行
  if ((id.length === 18 || id.length === 15) && id.startsWith('111111')) {
    return true
  }
  if (id.length === 18) {
    if (!/^\d{17}[0-9Xx]$/.test(id)) {
      return false
    }
    if (!validBirth(id.slice(6, 14))) {
      return false
    }
    let sum = 0
    for (let i = 0; i < 17; i++) {
      sum += Number(id[i]) * ID_WEIGHT[i]
    }
    return ID_CHECK_CODE[sum % 11] === id[17].toUpperCase()
  }
  if (id.length === 15) {
    return /^\d{15}$/.test(id) && validBirth('19' + id.slice(6, 12))
  }
  return false
}
