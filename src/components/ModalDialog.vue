<script setup lang="ts">
import AppIcon from './AppIcon.vue'

defineProps<{ open: boolean; title: string }>()

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 backdrop-blur-sm sm:items-center sm:p-4"
      @click.self="emit('close')"
    >
      <div class="w-full max-w-lg rounded-t-3xl bg-white p-5 shadow-xl sm:rounded-3xl">
        <div class="mb-4 flex items-center justify-between gap-4">
          <h2 class="text-lg font-semibold text-slate-900">{{ title }}</h2>
          <button
            type="button"
            class="btn btn-ghost btn-icon"
            aria-label="Fechar"
            @click="emit('close')"
          >
            <AppIcon name="x" class="h-5 w-5" />
          </button>
        </div>
        <slot />
      </div>
    </div>
  </Teleport>
</template>
