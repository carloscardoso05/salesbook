const amountInputFormatter = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
  useGrouping: false,
})

export function toCents(value: number): number {
  return Math.round(value * 100)
}

export function fromCents(cents: number): number {
  return cents / 100
}

export function formatAmountInput(cents: number): string {
  return amountInputFormatter.format(fromCents(Math.abs(cents)))
}

export function parseAmountText(raw: string): number | null {
  const cleaned = raw.trim().replace(/\s/g, '')
  const normalized = cleaned.includes(',')
    ? cleaned.replace(/\./g, '').replace(',', '.')
    : cleaned
  if (normalized === '' || normalized === '.' || normalized === '-') return null
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

export function normalizeAmountText(raw: string): string {
  let cleaned = raw.replace(/[^\d.,]/g, '')
  if (cleaned.includes(',')) cleaned = cleaned.replace(/\./g, '')
  const separatorIndex = cleaned.search(/[.,]/)
  if (separatorIndex === -1) return cleaned
  const separator = cleaned.charAt(separatorIndex)
  const integerPart = cleaned.slice(0, separatorIndex)
  const decimalPart = cleaned
    .slice(separatorIndex + 1)
    .replace(/[.,]/g, '')
    .slice(0, 2)
  return `${integerPart}${separator}${decimalPart}`
}
