import { readonly, ref } from 'vue'

const updating = ref(false)

export function markAppUpdating(): void {
  updating.value = true
}

export function useAppUpdate() {
  return { updating: readonly(updating) }
}
