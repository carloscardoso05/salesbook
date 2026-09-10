<script setup lang="ts">
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

withDefaults(
  defineProps<{
    icon?: IconName
    title: string
    description?: string
  }>(),
  { icon: 'cube' },
)
</script>

<template>
  <div
    class="flex flex-col items-center justify-center gap-2 rounded-2xl border border-dashed border-slate-300 bg-white/60 px-6 py-10 text-center"
  >
    <AppIcon :name="icon" class="h-8 w-8 text-slate-400" />
    <p class="text-sm font-semibold text-slate-700">{{ title }}</p>
    <p v-if="description" class="max-w-sm text-sm text-slate-500">{{ description }}</p>
    <div class="mt-2">
      <slot />
    </div>
  </div>
</template>
