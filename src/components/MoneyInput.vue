<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { formatBRL } from '../utils/format'
import {
  formatAmountInput,
  normalizeAmountText,
  parseAmountText,
  toCents,
} from '../utils/money'

const props = withDefaults(
  defineProps<{
    modelValue: number | null
    id?: string
    required?: boolean
    allowNegative?: boolean
    showPreview?: boolean
  }>(),
  { allowNegative: true, showPreview: true },
)

const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()

const negative = ref(false)
const text = ref('')

function currentCents(): number | null {
  const parsed = parseAmountText(text.value)
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
  let raw = input.value
  let nextNegative = negative.value
  if (raw.startsWith('-')) {
    nextNegative = true
    raw = raw.slice(1)
  }
  if (!props.allowNegative) nextNegative = false
  const normalized = normalizeAmountText(raw)
  negative.value = nextNegative
  text.value = normalized
  input.value = normalized
  emit('update:modelValue', signedValue.value)
}

function onBlur(): void {
  const parsed = parseAmountText(text.value)
  if (parsed === null) return
  text.value = formatAmountInput(toCents(parsed))
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
        pattern="\d*([.,]\d{0,2})?"
        :required="required"
        placeholder="0,00"
        @input="onInput"
        @blur="onBlur"
      />
      <button
        v-if="allowNegative"
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
    <p
      v-if="showPreview"
      class="mt-1.5 text-xs"
      :class="negative ? 'text-red-600' : 'text-slate-500'"
    >
      Valor: {{ signedValue === null ? '—' : formatBRL(signedValue) }}
    </p>
  </div>
</template>
