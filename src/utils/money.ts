const amountInputFormatter = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
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
