<script setup lang="ts">
import { dismiss, useToasts } from '../composables/useToast'
import AppIcon from './AppIcon.vue'

const toasts = useToasts()
</script>

<template>
  <Teleport to="body">
    <div
      class="pointer-events-none fixed inset-x-0 top-0 z-[60] flex flex-col items-center gap-2 px-4 pt-4"
    >
      <div
        v-for="item in toasts"
        :key="item.id"
        class="pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-2xl border p-3.5 shadow-lg"
        :class="{
          'border-red-200 bg-red-50 text-red-800': item.kind === 'error',
          'border-emerald-200 bg-emerald-50 text-emerald-800': item.kind === 'success',
          'border-slate-200 bg-white text-slate-700': item.kind === 'info',
        }"
      >
        <AppIcon
          :name="item.kind === 'success' ? 'check' : 'alert'"
          class="mt-0.5 h-5 w-5 shrink-0"
        />
        <p class="flex-1 text-sm font-medium">{{ item.message }}</p>
        <button
          type="button"
          class="shrink-0 opacity-60 hover:opacity-100"
          aria-label="Fechar aviso"
          @click="dismiss(item.id)"
        >
          <AppIcon name="x" class="h-4 w-4" />
        </button>
      </div>
    </div>
  </Teleport>
</template>
