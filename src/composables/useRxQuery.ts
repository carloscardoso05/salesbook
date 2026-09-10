import { onScopeDispose, ref, watch, type Ref, type WatchSource } from 'vue'
import type { Observable } from 'rxjs'

type QueryLike = { $: Observable<any[]> }

type JsonDocument = { toMutableJSON?: () => unknown }

export function useRxQuery<Doc>(
  queryFactory: () => QueryLike,
  sources: WatchSource[] = [],
): Ref<Doc[]> {
  const result = ref<Doc[]>([]) as Ref<Doc[]>
  let subscription: { unsubscribe: () => void } | null = null

  const run = (): void => {
    subscription?.unsubscribe()
    subscription = queryFactory().$.subscribe((docs) => {
      result.value = docs.map((doc) => {
        const jsonDocument = doc as JsonDocument
        return (
          typeof jsonDocument.toMutableJSON === 'function' ? jsonDocument.toMutableJSON() : doc
        ) as Doc
      })
    })
  }

  if (sources.length > 0) {
    watch(sources, run, { immediate: true })
  } else {
    run()
  }

  onScopeDispose(() => subscription?.unsubscribe())
  return result
}
