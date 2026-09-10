<script setup lang="ts">
import { useAppUpdate } from '../composables/useAppUpdate'

const { updating } = useAppUpdate()
</script>

<template>
  <Teleport to="body">
    <div
      v-if="updating"
      class="fixed inset-0 z-[80] flex touch-none flex-col items-center justify-center gap-4 bg-slate-900/70 px-6 text-center backdrop-blur-sm"
      role="alertdialog"
      aria-live="assertive"
      aria-label="Atualizando o aplicativo"
    >
      <span
        class="h-10 w-10 animate-spin rounded-full border-4 border-white/30 border-t-white"
        aria-hidden="true"
      />
      <p class="text-base font-semibold text-white">Atualizando o aplicativo...</p>
      <p class="text-sm text-white/80">Aguarde um instante e não feche esta tela.</p>
    </div>
  </Teleport>
</template>
