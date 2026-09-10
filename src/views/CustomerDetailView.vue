<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/EmptyState.vue'
import ModalDialog from '../components/ModalDialog.vue'
import PageHeader from '../components/PageHeader.vue'
import { useDatabase, useSalesbook } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import { errorMessage, toast } from '../composables/useToast'
import type {
  CustomerDocType,
  OrderDocType,
  OrderItemDocType,
  PaymentDocType,
} from '../db/types'
import { formatBRL, formatDateTime } from '../utils/format'

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
</script>

<template>
  <RouterLink
    to="/customers"
    class="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-800"
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
        <h2 class="mb-3 text-sm font-semibold text-slate-900">Pagamentos</h2>
        <EmptyState v-if="payments.length === 0" icon="banknotes" title="Nenhum pagamento" />
        <ul v-else class="divide-y divide-slate-100">
          <li
            v-for="payment in payments"
            :key="payment.id"
            class="flex items-center justify-between gap-3 py-2.5"
          >
            <p class="text-sm text-slate-600">{{ formatDateTime(payment.createdAt) }}</p>
            <span class="text-sm font-semibold text-emerald-600">
              {{ formatBRL(payment.amount) }}
            </span>
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
</template>
