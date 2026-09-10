const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const dateTimeFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short',
})

const dateFormatter = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
})

export function formatBRL(value: number): string {
  return brlFormatter.format(value)
}

export function formatSignedBRL(value: number): string {
  if (value === 0) return brlFormatter.format(0)
  const sign = value > 0 ? '+' : '-'
  return `${sign} ${brlFormatter.format(Math.abs(value))}`
}

export function formatDateTime(iso: string): string {
  return dateTimeFormatter.format(new Date(iso))
}

export function formatDate(iso: string): string {
  return dateFormatter.format(new Date(iso))
}
