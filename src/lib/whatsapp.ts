export function whatsappUrl(number: string, message: string) {
  const digits = number.replace(/\D/g, '')
  if (!digits) return '/contact'
  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`
}
