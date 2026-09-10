<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/EmptyState.vue'
import ModalDialog from '../components/ModalDialog.vue'
import MoneyInput from '../components/MoneyInput.vue'
import PageHeader from '../components/PageHeader.vue'
import { useDatabase, useSalesbook } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import { errorMessage, toast } from '../composables/useToast'
import type { CustomerDocType } from '../db/types'
import { formatBRL, pluralize } from '../utils/format'

const db = useDatabase()
const service = useSalesbook()

const customers = useRxQuery<CustomerDocType>(() =>
  db.customers.find({ sort: [{ nameNormalized: 'asc' }] }),
)

const isFormOpen = ref(false)
const name = ref('')
const initialBalance = ref<number | null>(0)
const saving = ref(false)
const customerToRemove = ref<CustomerDocType | null>(null)
const removing = ref(false)

function openForm(): void {
  name.value = ''
  initialBalance.value = 0
  isFormOpen.value = true
}

async function submit(): Promise<void> {
  saving.value = true
  try {
    await service.createCustomer(name.value, initialBalance.value ?? 0)
    toast.success('Cliente criado.')
    isFormOpen.value = false
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function confirmRemove(): Promise<void> {
  if (!customerToRemove.value) return
  removing.value = true
  try {
    await service.removeCustomer(customerToRemove.value.id)
    toast.success('Cliente excluído.')
    customerToRemove.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removing.value = false
  }
}
</script>

<template>
  <PageHeader title="Clientes" :subtitle="pluralize(customers.length, 'cadastrado', 'cadastrados')">
    <template #actions>
      <button type="button" class="btn btn-primary" @click="openForm">
        <AppIcon name="plus" class="h-4 w-4" />
        Novo cliente
      </button>
    </template>
  </PageHeader>

  <EmptyState
    v-if="customers.length === 0"
    icon="users"
    title="Nenhum cliente cadastrado"
    description="Cadastre seu primeiro cliente para começar a vender."
  >
    <button type="button" class="btn btn-primary" @click="openForm">Novo cliente</button>
  </EmptyState>

  <ul v-else class="space-y-2.5">
    <li v-for="customer in customers" :key="customer.id" class="card card-pad flex items-center gap-1">
      <RouterLink
        :to="`/customers/${customer.id}`"
        class="tap-row flex min-w-0 flex-1 items-center gap-3 rounded-xl py-1"
      >
        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-semibold text-slate-900">{{ customer.name }}</p>
          <p class="mt-0.5 text-xs text-slate-500">Ver detalhes</p>
        </div>
        <span
          class="badge"
          :class="
            customer.balanceCents < 0
              ? 'bg-red-100 text-red-700'
              : customer.balanceCents > 0
                ? 'bg-emerald-100 text-emerald-700'
                : 'bg-slate-100 text-slate-600'
          "
        >
          {{ formatBRL(customer.balanceCents) }}
        </span>
        <AppIcon name="chevronRight" class="h-4 w-4 shrink-0 text-slate-300" />
      </RouterLink>
      <button
        type="button"
        class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
        aria-label="Excluir cliente"
        @click="customerToRemove = customer"
      >
        <AppIcon name="trash" class="h-5 w-5" />
      </button>
    </li>
  </ul>

  <ModalDialog :open="isFormOpen" title="Novo cliente" @close="isFormOpen = false">
    <form class="space-y-4" @submit.prevent="submit">
      <div>
        <label class="label" for="customer-name">Nome</label>
        <input
          id="customer-name"
          v-model="name"
          class="input"
          type="text"
          required
          maxlength="200"
          placeholder="Ex.: Maria Souza"
        />
        <p class="mt-1.5 text-xs text-slate-500">O nome precisa ser único.</p>
      </div>
      <div>
        <label class="label" for="customer-initial-balance">Saldo inicial (R$)</label>
        <MoneyInput id="customer-initial-balance" v-model="initialBalance" />
        <p class="mt-1.5 text-xs text-slate-500">
          O valor será registrado como um ajuste no histórico.
        </p>
      </div>
      <div class="flex justify-end gap-2">
        <button type="button" class="btn btn-secondary" @click="isFormOpen = false">
          Cancelar
        </button>
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? 'Salvando...' : 'Salvar' }}
        </button>
      </div>
    </form>
  </ModalDialog>

  <ConfirmDialog
    :open="customerToRemove !== null"
    title="Excluir cliente"
    :message="`Tem certeza que deseja excluir ${customerToRemove?.name}? Esta ação não pode ser desfeita.`"
    confirm-label="Excluir"
    danger
    :busy="removing"
    @cancel="customerToRemove = null"
    @confirm="confirmRemove"
  />
</template>
