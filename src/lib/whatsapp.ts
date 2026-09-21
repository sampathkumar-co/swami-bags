export function hasWhatsAppNumber(number: string) {
  const digits = number.replace(/\D/g, '')
  return digits.length >= 10 && digits.length <= 15
}

export function whatsappUrl(number: string, message: string) {
  const digits = number.replace(/\D/g, '')
  if (!hasWhatsAppNumber(number)) return '/contact'
  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`
}
