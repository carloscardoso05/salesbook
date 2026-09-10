import { fromCents } from './money'

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

export function formatBRL(cents: number): string {
  return brlFormatter.format(fromCents(cents))
}

export function formatSignedBRL(cents: number): string {
  if (cents === 0) return brlFormatter.format(0)
  const sign = cents > 0 ? '+' : '-'
  return `${sign} ${brlFormatter.format(fromCents(Math.abs(cents)))}`
}

export function formatDateTime(iso: string): string {
  return dateTimeFormatter.format(new Date(iso))
}

export function formatDate(iso: string): string {
  return dateFormatter.format(new Date(iso))
}

export function pluralize(count: number, singular: string, plural: string): string {
  return `${count} ${count === 1 ? singular : plural}`
}
