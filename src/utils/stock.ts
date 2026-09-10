export const LOW_STOCK_THRESHOLD = 5

export function isLowStock(stockQuantity: number): boolean {
  return stockQuantity <= LOW_STOCK_THRESHOLD
}

export function stockBadgeClass(stockQuantity: number): string {
  if (stockQuantity === 0) return 'bg-red-100 text-red-700'
  if (isLowStock(stockQuantity)) return 'bg-amber-100 text-amber-700'
  return 'bg-slate-100 text-slate-600'
}
