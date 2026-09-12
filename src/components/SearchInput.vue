<script setup lang="ts">
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
  }>(),
  { placeholder: 'Buscar...' },
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <div class="relative">
    <AppIcon
      name="search"
      class="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-slate-400"
    />
    <input
      :value="modelValue"
      type="text"
      class="input pl-9 pr-9"
      :placeholder="placeholder"
      autocomplete="off"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <button
      v-if="modelValue"
      type="button"
      class="absolute top-1/2 right-1.5 -translate-y-1/2 cursor-pointer rounded-full p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
      aria-label="Limpar busca"
      @click="emit('update:modelValue', '')"
    >
      <AppIcon name="x" class="h-4 w-4" />
    </button>
  </div>
</template>
