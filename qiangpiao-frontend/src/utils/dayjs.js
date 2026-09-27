/**
 * 轻量日期工具（避免额外依赖 dayjs）
 */
function pad(n) {
    return n < 10 ? '0' + n : '' + n
}

function format(date) {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export default {
    format,
    today() {
        return format(new Date())
    },
    plusDays(days) {
        const d = new Date()
        d.setDate(d.getDate() + days)
        return format(d)
    }
}
