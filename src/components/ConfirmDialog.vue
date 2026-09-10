<script setup lang="ts">
import ModalDialog from './ModalDialog.vue'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    message: string
    confirmLabel?: string
    danger?: boolean
    busy?: boolean
  }>(),
  {
    confirmLabel: 'Confirmar',
    danger: false,
    busy: false,
  },
)

const emit = defineEmits<{ confirm: []; cancel: [] }>()
</script>

<template>
  <ModalDialog :open="open" :title="title" @close="emit('cancel')">
    <p class="text-sm text-slate-600">{{ message }}</p>
    <div class="mt-5 flex justify-end gap-2">
      <button type="button" class="btn btn-secondary" :disabled="busy" @click="emit('cancel')">
        Cancelar
      </button>
      <button
        type="button"
        class="btn"
        :class="danger ? 'btn-danger' : 'btn-primary'"
        :disabled="busy"
        @click="emit('confirm')"
      >
        {{ busy ? 'Aguarde...' : confirmLabel }}
      </button>
    </div>
  </ModalDialog>
</template>
