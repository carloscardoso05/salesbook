<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { formatBRL } from '../utils/format'
import { formatAmountInput, toCents } from '../utils/money'

const props = defineProps<{
  modelValue: number | null
  id?: string
  required?: boolean
}>()

const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()

const negative = ref(false)
const text = ref('')

function parseReais(raw: string): number | null {
  const normalized = raw.trim().replace(/\s/g, '').replace(',', '.')
  if (normalized === '' || normalized === '.' || normalized === '-') return null
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

function currentCents(): number | null {
  const parsed = parseReais(text.value)
  if (parsed === null) return null
  const cents = toCents(parsed)
  return negative.value ? -cents : cents
}

watch(
  () => props.modelValue,
  (value) => {
    if (value === currentCents()) return
    if (value === null || value === undefined || !Number.isFinite(value)) {
      text.value = ''
      negative.value = false
      return
    }
    negative.value = value < 0
    text.value = formatAmountInput(value)
  },
  { immediate: true },
)

const signedValue = computed(() => currentCents())

function onInput(event: Event): void {
  const input = event.target as HTMLInputElement
  const raw = input.value
  if (raw.startsWith('-')) {
    negative.value = true
    text.value = raw.slice(1)
    input.value = text.value
  } else {
    text.value = raw
  }
  emit('update:modelValue', signedValue.value)
}

function toggleSign(): void {
  negative.value = !negative.value
  emit('update:modelValue', signedValue.value)
}
</script>

<template>
  <div>
    <div class="flex gap-2">
      <input
        :id="id"
        :value="text"
        class="input"
        type="text"
        inputmode="decimal"
        autocomplete="off"
        pattern="\d*([.,]\d*)?"
        :required="required"
        placeholder="0,00"
        @input="onInput"
      />
      <button
        type="button"
        class="btn shrink-0 border px-3"
        :class="
          negative
            ? 'border-red-300 bg-red-50 text-red-700 hover:bg-red-100'
            : 'border-slate-300 bg-white text-slate-600 hover:bg-slate-50'
        "
        :aria-pressed="negative"
        aria-label="Alternar sinal negativo"
        title="Alternar sinal"
        @click="toggleSign"
      >
        ±
      </button>
    </div>
    <p class="mt-1.5 text-xs" :class="negative ? 'text-red-600' : 'text-slate-500'">
      Valor: {{ signedValue === null ? '—' : formatBRL(signedValue) }}
    </p>
  </div>
</template>
