import { readonly, ref } from 'vue'

export type ToastKind = 'success' | 'error' | 'info'

export type Toast = {
  id: number
  kind: ToastKind
  message: string
}

const toasts = ref<Toast[]>([])
let nextId = 1

function push(kind: ToastKind, message: string, timeout: number): void {
  const id = nextId++
  toasts.value = [...toasts.value, { id, kind, message }]
  window.setTimeout(() => dismiss(id), timeout)
}

export function dismiss(id: number): void {
  toasts.value = toasts.value.filter((toast) => toast.id !== id)
}

export const toast = {
  success: (message: string): void => push('success', message, 3500),
  error: (message: string): void => push('error', message, 6000),
  info: (message: string): void => push('info', message, 4000),
}

export function useToasts() {
  return readonly(toasts)
}

export function errorMessage(error: unknown): string {
  if (error instanceof Error && error.message) return error.message
  return 'Ocorreu um erro inesperado.'
}
