<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/EmptyState.vue'
import ModalDialog from '../components/ModalDialog.vue'
import PageHeader from '../components/PageHeader.vue'
import { useDatabase, useSalesbook } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import { errorMessage, toast } from '../composables/useToast'
import type { CustomerDocType, PaymentDocType } from '../db/types'
import { formatBRL, formatDateTime } from '../utils/format'
import { fromCents, toCents } from '../utils/money'

const route = useRoute()
const db = useDatabase()
const service = useSalesbook()

const customers = useRxQuery<CustomerDocType>(() =>
  db.customers.find({ sort: [{ nameNormalized: 'asc' }] }),
)
const payments = useRxQuery<PaymentDocType>(() =>
  db.payments.find({ sort: [{ createdAt: 'desc' }] }),
)

const customerNames = computed(() => {
  const map = new Map<string, string>()
  for (const customer of customers.value) map.set(customer.id, customer.name)
  return map
})

const selectedCustomerId = ref('')
const amount = ref<number | null>(null)
const saving = ref(false)

const editing = ref<PaymentDocType | null>(null)
const editAmount = ref<number | null>(null)
const savingEdit = ref(false)

const paymentToRemove = ref<PaymentDocType | null>(null)
const removing = ref(false)

onMounted(() => {
  const preselect = route.query.customerId
  if (typeof preselect === 'string') selectedCustomerId.value = preselect
})

watch(
  customers,
  (list) => {
    if (!selectedCustomerId.value && list.length > 0) {
      selectedCustomerId.value = list[0]?.id ?? ''
    }
  },
  { immediate: true },
)

async function submit(): Promise<void> {
  saving.value = true
  try {
    await service.addPayment({
      customerId: selectedCustomerId.value,
      amountCents: toCents(Number(amount.value)),
    })
    toast.success('Pagamento registrado.')
    amount.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

function openEdit(payment: PaymentDocType): void {
  editing.value = payment
  editAmount.value = fromCents(payment.amountCents)
}

async function saveEdit(): Promise<void> {
  if (!editing.value) return
  savingEdit.value = true
  try {
    await service.updatePayment(editing.value.id, {
      amountCents: toCents(Number(editAmount.value)),
    })
    toast.success('Pagamento atualizado e saldo ajustado.')
    editing.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    savingEdit.value = false
  }
}

async function confirmRemove(): Promise<void> {
  if (!paymentToRemove.value) return
  removing.value = true
  try {
    await service.removePayment(paymentToRemove.value.id)
    toast.success('Pagamento excluído e saldo ajustado.')
    paymentToRemove.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removing.value = false
  }
}

function customerName(customerId: string): string {
  return customerNames.value.get(customerId) ?? 'Cliente removido'
}
</script>

<template>
  <PageHeader title="Pagamentos" subtitle="Registre valores recebidos dos clientes" />

  <div class="grid gap-4 lg:grid-cols-[360px_1fr]">
    <section class="card card-pad h-fit">
      <h2 class="mb-3 text-sm font-semibold text-slate-900">Novo pagamento</h2>
      <form class="space-y-4" @submit.prevent="submit">
        <div>
          <label class="label" for="payment-customer">Cliente</label>
          <select id="payment-customer" v-model="selectedCustomerId" class="input" required>
            <option v-for="customer in customers" :key="customer.id" :value="customer.id">
              {{ customer.name }}
            </option>
          </select>
        </div>
        <div>
          <label class="label" for="payment-amount">Valor (R$)</label>
          <input
            id="payment-amount"
            v-model.number="amount"
            class="input"
            type="number"
            min="0.01"
            step="0.01"
            required
            placeholder="0,00"
          />
          <p class="mt-1.5 text-xs text-slate-500">O valor precisa ser maior que zero.</p>
        </div>
        <button
          type="submit"
          class="btn btn-primary w-full"
          :disabled="saving || customers.length === 0 || selectedCustomerId === ''"
        >
          <AppIcon name="banknotes" class="h-4 w-4" />
          {{ saving ? 'Registrando...' : 'Registrar pagamento' }}
        </button>
        <p v-if="customers.length === 0" class="text-xs text-slate-500">
          Cadastre um cliente antes de registrar pagamentos.
        </p>
      </form>
    </section>

    <section class="card card-pad">
      <h2 class="mb-3 text-sm font-semibold text-slate-900">Histórico</h2>
      <EmptyState
        v-if="payments.length === 0"
        icon="banknotes"
        title="Nenhum pagamento registrado"
        description="Os pagamentos aparecem aqui e creditam o saldo do cliente."
      />
      <ul v-else class="divide-y divide-slate-100">
        <li
          v-for="payment in payments"
          :key="payment.id"
          class="flex items-center gap-2 py-2.5"
        >
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-medium text-slate-800">
              {{ customerName(payment.customerId) }}
            </p>
            <p class="text-xs text-slate-500">{{ formatDateTime(payment.createdAt) }}</p>
          </div>
          <span class="text-sm font-semibold text-emerald-600">
            {{ formatBRL(payment.amountCents) }}
          </span>
          <button
            type="button"
            class="btn btn-ghost btn-icon text-slate-400 hover:text-indigo-600"
            aria-label="Editar pagamento"
            @click="openEdit(payment)"
          >
            <AppIcon name="pencil" class="h-5 w-5" />
          </button>
          <button
            type="button"
            class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
            aria-label="Excluir pagamento"
            @click="paymentToRemove = payment"
          >
            <AppIcon name="trash" class="h-5 w-5" />
          </button>
        </li>
      </ul>
    </section>
  </div>

  <ModalDialog :open="editing !== null" title="Editar pagamento" @close="editing = null">
    <form class="space-y-4" @submit.prevent="saveEdit">
      <div>
        <label class="label" for="edit-payment-amount">Valor (R$)</label>
        <input
          id="edit-payment-amount"
          v-model.number="editAmount"
          class="input"
          type="number"
          min="0.01"
          step="0.01"
          required
        />
        <p class="mt-1.5 text-xs text-slate-500">
          A diferença é aplicada automaticamente no saldo do cliente.
        </p>
      </div>
      <div class="flex justify-end gap-2">
        <button type="button" class="btn btn-secondary" @click="editing = null">Cancelar</button>
        <button type="submit" class="btn btn-primary" :disabled="savingEdit">
          {{ savingEdit ? 'Salvando...' : 'Salvar' }}
        </button>
      </div>
    </form>
  </ModalDialog>

  <ConfirmDialog
    :open="paymentToRemove !== null"
    title="Excluir pagamento"
    :message="`Excluir este pagamento de ${paymentToRemove ? formatBRL(paymentToRemove.amountCents) : ''} debita o valor do saldo de ${paymentToRemove ? customerName(paymentToRemove.customerId) : 'cliente'}. Deseja continuar?`"
    confirm-label="Excluir"
    danger
    :busy="removing"
    @cancel="paymentToRemove = null"
    @confirm="confirmRemove"
  />
</template>
