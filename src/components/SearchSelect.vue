<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import { normalizeName } from '../utils/text'

export type SearchOption = {
  id: string
  label: string
  sublabel?: string
  disabled?: boolean
}

const props = withDefaults(
  defineProps<{
    modelValue: string
    options: SearchOption[]
    id?: string
    placeholder?: string
    searchPlaceholder?: string
    emptyText?: string
  }>(),
  {
    placeholder: 'Selecionar...',
    searchPlaceholder: 'Buscar...',
    emptyText: 'Nenhum resultado',
  },
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const open = ref(false)
const query = ref('')
const highlight = ref(-1)
const rootRef = ref<HTMLElement | null>(null)
const searchRef = ref<HTMLInputElement | null>(null)
const listRef = ref<HTMLElement | null>(null)

const selected = computed(
  () => props.options.find((option) => option.id === props.modelValue) ?? null,
)

const filtered = computed(() => {
  const term = normalizeName(query.value).trim()
  if (!term) return props.options
  return props.options.filter((option) => normalizeName(option.label).includes(term))
})

function openList(): void {
  if (open.value) return
  open.value = true
  query.value = ''
  highlight.value = -1
  void nextTick(() => searchRef.value?.focus())
}

function closeList(): void {
  open.value = false
  query.value = ''
  highlight.value = -1
}

function select(option: SearchOption): void {
  if (option.disabled) return
  emit('update:modelValue', option.id)
  closeList()
}

function onTriggerKeydown(event: KeyboardEvent): void {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp' || event.key === 'Enter') {
    event.preventDefault()
    openList()
    if (event.key === 'ArrowDown') highlight.value = 0
  }
  if (event.key === 'Escape') closeList()
}

function onListKeydown(event: KeyboardEvent): void {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    moveHighlight(event.key === 'ArrowDown' ? 1 : -1)
  }
  if (event.key === 'Enter') {
    const option = filtered.value[highlight.value]
    if (option) {
      event.preventDefault()
      select(option)
    }
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    closeList()
  }
}

function setHighlight(index: number, disabled?: boolean): void {
  if (disabled) return
  highlight.value = index
}

function moveHighlight(delta: number): void {
  const options = filtered.value
  if (options.length === 0) return
  let next = (highlight.value + delta) % options.length
  if (next < 0) next = options.length - 1
  highlight.value = next
  void nextTick(() => {
    const element = listRef.value?.querySelector<HTMLElement>('[aria-selected="true"]')
    element?.scrollIntoView({ block: 'nearest' })
  })
}

function onDocumentPointerDown(event: PointerEvent): void {
  if (!rootRef.value || rootRef.value.contains(event.target as Node)) return
  closeList()
}

watch(open, (isOpen) => {
  if (isOpen) document.addEventListener('pointerdown', onDocumentPointerDown)
  else document.removeEventListener('pointerdown', onDocumentPointerDown)
})

onBeforeUnmount(() => document.removeEventListener('pointerdown', onDocumentPointerDown))
</script>

<template>
  <div ref="rootRef" class="relative">
    <button
      :id="id"
      type="button"
      class="input flex items-center justify-between gap-2 text-left"
      :aria-expanded="open"
      aria-haspopup="listbox"
      @click="open ? closeList() : openList()"
      @keydown="onTriggerKeydown"
    >
      <span class="min-w-0 flex-1 truncate" :class="selected ? 'text-slate-900' : 'text-slate-400'">
        {{ selected ? selected.label : placeholder }}
      </span>
      <AppIcon
        name="chevronDown"
        class="h-4 w-4 shrink-0 text-slate-400 transition-transform"
        :class="open ? 'rotate-180' : ''"
      />
    </button>

    <div v-if="open" class="mt-2 rounded-2xl border border-slate-200 bg-white p-2 shadow-sm">
      <div class="relative">
        <AppIcon
          name="search"
          class="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-slate-400"
        />
        <input
          ref="searchRef"
          v-model="query"
          type="text"
          class="input pl-9"
          :placeholder="searchPlaceholder"
          autocomplete="off"
          role="combobox"
          aria-expanded="true"
          aria-controls="searchselect-listbox"
          @keydown="onListKeydown"
        />
      </div>

      <p v-if="filtered.length === 0" class="px-3 py-4 text-center text-sm text-slate-500">
        {{ emptyText }}
      </p>
      <ul
        v-else
        id="searchselect-listbox"
        ref="listRef"
        role="listbox"
        class="mt-1 max-h-56 overflow-y-auto"
      >
        <li v-for="(option, index) in filtered" :key="option.id" role="presentation">
          <button
            type="button"
            role="option"
            class="tap-row flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2 text-left text-sm"
            :class="
              option.disabled
                ? 'cursor-not-allowed opacity-50'
                : highlight === index
                  ? 'bg-indigo-50'
                  : ''
            "
            :aria-selected="highlight === index"
            :aria-disabled="option.disabled"
            @click="select(option)"
            @mousemove="setHighlight(index, option.disabled)"
          >
            <span class="min-w-0 flex-1">
              <span class="block truncate font-medium text-slate-800">{{ option.label }}</span>
              <span v-if="option.sublabel" class="block text-xs text-slate-500">
                {{ option.sublabel }}
              </span>
            </span>
            <AppIcon
              v-if="selected?.id === option.id"
              name="check"
              class="h-4 w-4 shrink-0 text-indigo-600"
            />
          </button>
        </li>
      </ul>
    </div>
  </div>
</template>
