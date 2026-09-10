<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/EmptyState.vue'
import ModalDialog from '../components/ModalDialog.vue'
import PageHeader from '../components/PageHeader.vue'
import SignedMoneyInput from '../components/SignedMoneyInput.vue'
import { useDatabase, useSalesbook } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import { errorMessage, toast } from '../composables/useToast'
import type {
  AdjustmentDocType,
  CustomerDocType,
  OrderDocType,
  OrderItemDocType,
  PaymentDocType,
} from '../db/types'
import { formatBRL, formatDateTime, formatSignedBRL } from '../utils/format'

type FinancialEntry =
  | { kind: 'payment'; id: string; createdAt: string; amount: number; payment: PaymentDocType }
  | {
      kind: 'adjustment'
      id: string
      createdAt: string
      amount: number
      adjustment: AdjustmentDocType
    }

const route = useRoute()
const router = useRouter()
const db = useDatabase()
const service = useSalesbook()

const customerId = computed(() => String(route.params.id))

const customers = useRxQuery<CustomerDocType>(
  () => db.customers.find({ selector: { id: { $eq: customerId.value } } }),
  [customerId],
)
const customer = computed(() => customers.value[0] ?? null)

const orders = useRxQuery<OrderDocType>(
  () =>
    db.orders.find({
      selector: { customerId: { $eq: customerId.value } },
      sort: [{ createdAt: 'desc' }],
    }),
  [customerId],
)

const payments = useRxQuery<PaymentDocType>(
  () =>
    db.payments.find({
      selector: { customerId: { $eq: customerId.value } },
      sort: [{ createdAt: 'desc' }],
    }),
  [customerId],
)

const adjustments = useRxQuery<AdjustmentDocType>(
  () =>
    db.adjustments.find({
      selector: { customerId: { $eq: customerId.value } },
      sort: [{ createdAt: 'desc' }],
    }),
  [customerId],
)

const financialEntries = computed<FinancialEntry[]>(() => {
  const entries: FinancialEntry[] = [
    ...payments.value.map((payment) => ({
      kind: 'payment' as const,
      id: payment.id,
      createdAt: payment.createdAt,
      amount: payment.amount,
      payment,
    })),
    ...adjustments.value.map((adjustment) => ({
      kind: 'adjustment' as const,
      id: adjustment.id,
      createdAt: adjustment.createdAt,
      amount: adjustment.amount,
      adjustment,
    })),
  ]
  return entries.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
})

const allItems = useRxQuery<OrderItemDocType>(() => db.orderitems.find())

const orderTotals = computed(() => {
  const totals = new Map<string, number>()
  for (const item of allItems.value) {
    totals.set(item.orderId, (totals.get(item.orderId) ?? 0) + item.price)
  }
  return totals
})

const orderItemCounts = computed(() => {
  const counts = new Map<string, number>()
  for (const item of allItems.value) {
    counts.set(item.orderId, (counts.get(item.orderId) ?? 0) + 1)
  }
  return counts
})

const isEditOpen = ref(false)
const editName = ref('')
const saving = ref(false)
const isRemoveOpen = ref(false)
const removing = ref(false)

const isAdjustOpen = ref(false)
const adjustBalance = ref<number | null>(null)
const savingAdjust = ref(false)

const editingAdjustment = ref<AdjustmentDocType | null>(null)
const editAdjustBalance = ref<number | null>(null)
const savingEditAdjust = ref(false)

const adjustmentToRemove = ref<AdjustmentDocType | null>(null)
const removingAdjust = ref(false)

const editingPayment = ref<PaymentDocType | null>(null)
const editPaymentAmount = ref<number | null>(null)
const savingEditPayment = ref(false)

const paymentToRemove = ref<PaymentDocType | null>(null)
const removingPayment = ref(false)

const adjustDelta = computed(() => {
  if (!customer.value || adjustBalance.value === null) return 0
  return Math.round((Number(adjustBalance.value) - customer.value.balance) * 100) / 100
})

const editAdjustDelta = computed(() => {
  if (!customer.value || !editingAdjustment.value || editAdjustBalance.value === null) return 0
  const balanceWithoutAdjustment = customer.value.balance - editingAdjustment.value.amount
  return Math.round((Number(editAdjustBalance.value) - balanceWithoutAdjustment) * 100) / 100
})

function openEdit(): void {
  if (!customer.value) return
  editName.value = customer.value.name
  isEditOpen.value = true
}

async function saveName(): Promise<void> {
  if (!customer.value) return
  saving.value = true
  try {
    await service.updateCustomerName(customer.value.id, editName.value)
    toast.success('Nome atualizado.')
    isEditOpen.value = false
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function removeCustomer(): Promise<void> {
  if (!customer.value) return
  removing.value = true
  try {
    await service.removeCustomer(customer.value.id)
    toast.success('Cliente excluído.')
    await router.push('/customers')
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removing.value = false
  }
}

function openAdjust(): void {
  if (!customer.value) return
  adjustBalance.value = customer.value.balance
  isAdjustOpen.value = true
}

async function saveAdjust(): Promise<void> {
  if (!customer.value) return
  savingAdjust.value = true
  try {
    await service.createBalanceAdjustment({
      customerId: customer.value.id,
      newBalance: Number(adjustBalance.value),
    })
    toast.success('Saldo ajustado.')
    isAdjustOpen.value = false
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    savingAdjust.value = false
  }
}

function openEditAdjustment(adjustment: AdjustmentDocType): void {
  if (!customer.value) return
  editingAdjustment.value = adjustment
  editAdjustBalance.value = customer.value.balance
}

async function saveEditAdjustment(): Promise<void> {
  if (!editingAdjustment.value) return
  savingEditAdjust.value = true
  try {
    await service.updateAdjustment(editingAdjustment.value.id, {
      newBalance: Number(editAdjustBalance.value),
    })
    toast.success('Ajuste atualizado.')
    editingAdjustment.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    savingEditAdjust.value = false
  }
}

async function confirmRemoveAdjustment(): Promise<void> {
  if (!adjustmentToRemove.value) return
  removingAdjust.value = true
  try {
    await service.removeAdjustment(adjustmentToRemove.value.id)
    toast.success('Ajuste excluído e saldo atualizado.')
    adjustmentToRemove.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removingAdjust.value = false
  }
}

function openEditPayment(payment: PaymentDocType): void {
  editingPayment.value = payment
  editPaymentAmount.value = payment.amount
}

async function saveEditPayment(): Promise<void> {
  if (!editingPayment.value) return
  savingEditPayment.value = true
  try {
    await service.updatePayment(editingPayment.value.id, {
      amount: Number(editPaymentAmount.value),
    })
    toast.success('Pagamento atualizado e saldo ajustado.')
    editingPayment.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    savingEditPayment.value = false
  }
}

async function confirmRemovePayment(): Promise<void> {
  if (!paymentToRemove.value) return
  removingPayment.value = true
  try {
    await service.removePayment(paymentToRemove.value.id)
    toast.success('Pagamento excluído e saldo ajustado.')
    paymentToRemove.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removingPayment.value = false
  }
}

function openEditEntry(entry: FinancialEntry): void {
  if (entry.kind === 'payment') openEditPayment(entry.payment)
  else openEditAdjustment(entry.adjustment)
}

function requestRemoveEntry(entry: FinancialEntry): void {
  if (entry.kind === 'payment') paymentToRemove.value = entry.payment
  else adjustmentToRemove.value = entry.adjustment
}

function adjustmentRemovalMessage(): string {
  if (!adjustmentToRemove.value || !customer.value) return ''
  const after = customer.value.balance - adjustmentToRemove.value.amount
  return `Excluir este ajuste de ${formatSignedBRL(adjustmentToRemove.value.amount)} altera o saldo do cliente de ${formatBRL(customer.value.balance)} para ${formatBRL(after)}. Deseja continuar?`
}

function paymentRemovalMessage(): string {
  if (!paymentToRemove.value || !customer.value) return ''
  const after = customer.value.balance - paymentToRemove.value.amount
  return `Excluir este pagamento de ${formatBRL(paymentToRemove.value.amount)} altera o saldo do cliente de ${formatBRL(customer.value.balance)} para ${formatBRL(after)}. Deseja continuar?`
}
</script>

<template>
  <RouterLink
    to="/customers"
    class="tap-row mb-4 inline-flex items-center gap-1.5 rounded-lg px-1 text-sm font-medium text-slate-500 hover:text-slate-800"
  >
    <AppIcon name="arrowLeft" class="h-4 w-4" />
    Clientes
  </RouterLink>

  <template v-if="customer">
    <PageHeader :title="customer.name" subtitle="Detalhes do cliente">
      <template #actions>
        <button type="button" class="btn btn-secondary" @click="openEdit">
          <AppIcon name="pencil" class="h-4 w-4" />
          Editar
        </button>
        <button type="button" class="btn btn-secondary" @click="openAdjust">
          <AppIcon name="adjustments" class="h-4 w-4" />
          Ajustar saldo
        </button>
        <RouterLink
          :to="{ path: '/payments', query: { customerId: customer.id } }"
          class="btn btn-primary"
        >
          <AppIcon name="banknotes" class="h-4 w-4" />
          Pagamento
        </RouterLink>
        <button
          type="button"
          class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
          aria-label="Excluir cliente"
          @click="isRemoveOpen = true"
        >
          <AppIcon name="trash" class="h-5 w-5" />
        </button>
      </template>
    </PageHeader>

    <div class="card card-pad mb-5">
      <p class="text-xs font-medium uppercase tracking-wide text-slate-500">Saldo</p>
      <p
        class="mt-1 text-2xl font-bold"
        :class="
          customer.balance < 0
            ? 'text-red-600'
            : customer.balance > 0
              ? 'text-emerald-600'
              : 'text-slate-900'
        "
      >
        {{ formatBRL(customer.balance) }}
      </p>
      <p class="mt-1 text-xs text-slate-500">
        {{
          customer.balance < 0
            ? 'Cliente deve este valor.'
            : customer.balance > 0
              ? 'Cliente tem crédito.'
              : 'Sem pendências.'
        }}
      </p>
    </div>

    <div class="grid gap-4 lg:grid-cols-2">
      <section class="card card-pad">
        <h2 class="mb-3 text-sm font-semibold text-slate-900">Pedidos</h2>
        <EmptyState v-if="orders.length === 0" icon="receipt" title="Nenhum pedido" />
        <ul v-else class="divide-y divide-slate-100">
          <li v-for="order in orders" :key="order.id">
            <RouterLink
              :to="`/orders/${order.id}`"
              class="tap-row -mx-1 flex items-center justify-between gap-3 rounded-xl px-1 py-2.5"
            >
              <div>
                <p class="text-sm font-medium text-slate-800">
                  {{ formatDateTime(order.createdAt) }}
                </p>
                <p class="text-xs text-slate-500">
                  {{ orderItemCounts.get(order.id) ?? 0 }} item(ns)
                </p>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-sm font-semibold text-slate-900">
                  {{ formatBRL(orderTotals.get(order.id) ?? 0) }}
                </span>
                <AppIcon name="chevronRight" class="h-4 w-4 shrink-0 text-slate-300" />
              </div>
            </RouterLink>
          </li>
        </ul>
      </section>

      <section class="card card-pad">
        <h2 class="mb-3 text-sm font-semibold text-slate-900">Histórico financeiro</h2>
        <EmptyState
          v-if="financialEntries.length === 0"
          icon="banknotes"
          title="Nenhum lançamento"
          description="Pagamentos e ajustes de saldo aparecem aqui."
        />
        <ul v-else class="divide-y divide-slate-100">
          <li
            v-for="entry in financialEntries"
            :key="`${entry.kind}-${entry.id}`"
            class="flex items-center gap-2 py-2.5"
          >
            <AppIcon
              :name="entry.kind === 'payment' ? 'banknotes' : 'adjustments'"
              class="h-4 w-4 shrink-0"
              :class="
                entry.kind === 'payment'
                  ? 'text-emerald-500'
                  : entry.amount < 0
                    ? 'text-red-400'
                    : 'text-emerald-500'
              "
            />
            <div class="min-w-0 flex-1">
              <p class="text-sm font-medium text-slate-800">
                {{ entry.kind === 'payment' ? 'Pagamento' : 'Ajuste' }}
              </p>
              <p class="text-xs text-slate-500">{{ formatDateTime(entry.createdAt) }}</p>
            </div>
            <span
              class="text-sm font-semibold"
              :class="
                entry.kind === 'adjustment' && entry.amount < 0 ? 'text-red-600' : 'text-emerald-600'
              "
            >
              {{ entry.kind === 'adjustment' ? formatSignedBRL(entry.amount) : formatBRL(entry.amount) }}
            </span>
            <button
              type="button"
              class="btn btn-ghost btn-icon text-slate-400 hover:text-indigo-600"
              aria-label="Editar lançamento"
              @click="openEditEntry(entry)"
            >
              <AppIcon name="pencil" class="h-5 w-5" />
            </button>
            <button
              type="button"
              class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
              aria-label="Excluir lançamento"
              @click="requestRemoveEntry(entry)"
            >
              <AppIcon name="trash" class="h-5 w-5" />
            </button>
          </li>
        </ul>
      </section>
    </div>
  </template>

  <EmptyState
    v-else
    icon="users"
    title="Cliente não encontrado"
    description="Ele pode ter sido excluído."
  >
    <RouterLink to="/customers" class="btn btn-primary">Voltar para clientes</RouterLink>
  </EmptyState>

  <ModalDialog :open="isEditOpen" title="Editar cliente" @close="isEditOpen = false">
    <form class="space-y-4" @submit.prevent="saveName">
      <div>
        <label class="label" for="edit-customer-name">Nome</label>
        <input
          id="edit-customer-name"
          v-model="editName"
          class="input"
          type="text"
          required
          maxlength="200"
        />
      </div>
      <div class="flex justify-end gap-2">
        <button type="button" class="btn btn-secondary" @click="isEditOpen = false">
          Cancelar
        </button>
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? 'Salvando...' : 'Salvar' }}
        </button>
      </div>
    </form>
  </ModalDialog>

  <ModalDialog :open="isAdjustOpen" title="Ajustar saldo" @close="isAdjustOpen = false">
    <form class="space-y-4" @submit.prevent="saveAdjust">
      <div>
        <label class="label" for="adjust-balance">Novo saldo (R$)</label>
        <SignedMoneyInput id="adjust-balance" v-model="adjustBalance" required />
        <p class="mt-1.5 text-xs text-slate-500">
          Saldo atual: {{ customer ? formatBRL(customer.balance) : '' }}
          <span
            class="ml-1"
            :class="adjustDelta === 0 ? '' : adjustDelta > 0 ? 'text-emerald-600' : 'text-red-600'"
          >
            {{
              adjustDelta === 0
                ? '· sem alteração'
                : '· diferença ' + formatSignedBRL(adjustDelta)
            }}
          </span>
        </p>
      </div>
      <div class="flex justify-end gap-2">
        <button type="button" class="btn btn-secondary" @click="isAdjustOpen = false">
          Cancelar
        </button>
        <button type="submit" class="btn btn-primary" :disabled="savingAdjust">
          {{ savingAdjust ? 'Salvando...' : 'Salvar' }}
        </button>
      </div>
    </form>
  </ModalDialog>

  <ModalDialog
    :open="editingAdjustment !== null"
    title="Editar ajuste"
    @close="editingAdjustment = null"
  >
    <form class="space-y-4" @submit.prevent="saveEditAdjustment">
      <div>
        <label class="label" for="edit-adjust-balance">Novo saldo (R$)</label>
        <SignedMoneyInput id="edit-adjust-balance" v-model="editAdjustBalance" required />
        <p class="mt-1.5 text-xs text-slate-500">
          Ajuste atual:
          {{ editingAdjustment ? formatSignedBRL(editingAdjustment.amount) : '' }}
          <span
            class="ml-1"
            :class="
              editAdjustDelta === 0 ? '' : editAdjustDelta > 0 ? 'text-emerald-600' : 'text-red-600'
            "
          >
            {{
              editAdjustDelta === 0
                ? '· sem alteração'
                : '· novo ajuste ' + formatSignedBRL(editAdjustDelta)
            }}
          </span>
        </p>
      </div>
      <div class="flex justify-end gap-2">
        <button type="button" class="btn btn-secondary" @click="editingAdjustment = null">
          Cancelar
        </button>
        <button type="submit" class="btn btn-primary" :disabled="savingEditAdjust">
          {{ savingEditAdjust ? 'Salvando...' : 'Salvar' }}
        </button>
      </div>
    </form>
  </ModalDialog>

  <ModalDialog :open="editingPayment !== null" title="Editar pagamento" @close="editingPayment = null">
    <form class="space-y-4" @submit.prevent="saveEditPayment">
      <div>
        <label class="label" for="edit-payment-amount">Valor (R$)</label>
        <input
          id="edit-payment-amount"
          v-model.number="editPaymentAmount"
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
        <button type="button" class="btn btn-secondary" @click="editingPayment = null">
          Cancelar
        </button>
        <button type="submit" class="btn btn-primary" :disabled="savingEditPayment">
          {{ savingEditPayment ? 'Salvando...' : 'Salvar' }}
        </button>
      </div>
    </form>
  </ModalDialog>

  <ConfirmDialog
    :open="isRemoveOpen"
    title="Excluir cliente"
    :message="`Tem certeza que deseja excluir ${customer?.name}? Esta ação não pode ser desfeita.`"
    confirm-label="Excluir"
    danger
    :busy="removing"
    @cancel="isRemoveOpen = false"
    @confirm="removeCustomer"
  />

  <ConfirmDialog
    :open="adjustmentToRemove !== null"
    title="Excluir ajuste"
    :message="adjustmentRemovalMessage()"
    confirm-label="Excluir"
    danger
    :busy="removingAdjust"
    @cancel="adjustmentToRemove = null"
    @confirm="confirmRemoveAdjustment"
  />

  <ConfirmDialog
    :open="paymentToRemove !== null"
    title="Excluir pagamento"
    :message="paymentRemovalMessage()"
    confirm-label="Excluir"
    danger
    :busy="removingPayment"
    @cancel="paymentToRemove = null"
    @confirm="confirmRemovePayment"
  />
</template>
