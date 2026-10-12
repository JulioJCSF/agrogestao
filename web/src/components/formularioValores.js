// Valores decimais são mantidos como texto até a serialização do JSON.
export function formatCpf(value) {
  return String(value ?? '')
    .replace(/\D/g, '')
    .slice(0, 11)
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1-$2')
}
export function displayCurrency(value) {
  if (value === '' || value == null) return ''
  const [integer, fraction = ''] = String(value).replace(',', '.').split('.')
  return `R$ ${(integer || '0').replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${fraction.padEnd(2, '0')}`
}
export function readCurrency(value) {
  const digits = value.replace(/\D/g, '')
  if (!digits) return ''
  return `${digits.slice(0, -2) || '0'}.${digits.slice(-2).padStart(2, '0')}`
}
export function displayDate(value) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value || '')
    ? value.split('-').reverse().join('/')
    : value || ''
}
export function readDate(value) {
  if (!/^\d{2}\/\d{2}\/\d{4}$/.test(value)) return null
  const [day, month, year] = value.split('/').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  if (
    year < 1000 ||
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  )
    return null
  return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}
export function localToday() {
  return new Intl.DateTimeFormat('sv-SE', {
    timeZone: 'America/Fortaleza',
  }).format(new Date())
}
