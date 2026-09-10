<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/EmptyState.vue'
import ModalDialog from '../components/ModalDialog.vue'
import PageHeader from '../components/PageHeader.vue'
import { useDatabase, useSalesbook } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import { errorMessage, toast } from '../composables/useToast'
import type { CustomerDocType, OrderDocType, OrderItemDocType } from '../db/types'
import { formatBRL, formatDateTime, pluralize } from '../utils/format'

const db = useDatabase()
const service = useSalesbook()
const router = useRouter()

const customers = useRxQuery<CustomerDocType>(() =>
  db.customers.find({ sort: [{ nameNormalized: 'asc' }] }),
)
const orders = useRxQuery<OrderDocType>(() =>
  db.orders.find({ sort: [{ createdAt: 'desc' }] }),
)
const items = useRxQuery<OrderItemDocType>(() => db.orderitems.find())

const customerNames = computed(() => {
  const map = new Map<string, string>()
  for (const customer of customers.value) map.set(customer.id, customer.name)
  return map
})

const orderSummaries = computed(() => {
  const count = new Map<string, number>()
  const total = new Map<string, number>()
  for (const item of items.value) {
    count.set(item.orderId, (count.get(item.orderId) ?? 0) + 1)
    total.set(item.orderId, (total.get(item.orderId) ?? 0) + item.priceCents)
  }
  return { count, total }
})

const isFormOpen = ref(false)
const selectedCustomerId = ref('')
const saving = ref(false)
const orderToRemove = ref<OrderDocType | null>(null)
const removing = ref(false)

function openForm(): void {
  selectedCustomerId.value = customers.value[0]?.id ?? ''
  isFormOpen.value = true
}

async function submit(): Promise<void> {
  saving.value = true
  try {
    const order = await service.createOrder(selectedCustomerId.value)
    isFormOpen.value = false
    toast.success('Pedido criado.')
    await router.push(`/orders/${order.id}`)
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function confirmRemove(): Promise<void> {
  if (!orderToRemove.value) return
  removing.value = true
  try {
    await service.removeOrder(orderToRemove.value.id)
    toast.success('Pedido excluído, estoque devolvido e saldo estornado.')
    orderToRemove.value = null
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
  <PageHeader title="Pedidos" :subtitle="pluralize(orders.length, 'registrado', 'registrados')">
    <template #actions>
      <button
        type="button"
        class="btn btn-primary"
        :disabled="customers.length === 0"
        @click="openForm"
      >
        <AppIcon name="plus" class="h-4 w-4" />
        Novo pedido
      </button>
    </template>
  </PageHeader>

  <p v-if="customers.length === 0" class="mb-4 text-sm text-slate-500">
    Cadastre um cliente antes de criar pedidos.
  </p>

  <EmptyState
    v-if="orders.length === 0"
    icon="receipt"
    title="Nenhum pedido registrado"
    description="Crie um pedido, escolha o cliente e adicione os produtos vendidos."
  >
    <button
      type="button"
      class="btn btn-primary"
      :disabled="customers.length === 0"
      @click="openForm"
    >
      <AppIcon name="plus" class="h-4 w-4" />
      Novo pedido
    </button>
  </EmptyState>

  <ul v-else class="space-y-2.5">
    <li v-for="order in orders" :key="order.id" class="card card-pad flex items-center gap-1">
      <RouterLink
        :to="`/orders/${order.id}`"
        class="tap-row flex min-w-0 flex-1 items-center gap-3 rounded-xl py-1"
      >
        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-semibold text-slate-900">
            {{ customerName(order.customerId) }}
          </p>
          <p class="mt-0.5 text-xs text-slate-500">
            {{ formatDateTime(order.createdAt) }} ·
            {{ pluralize(orderSummaries.count.get(order.id) ?? 0, 'item', 'itens') }}
          </p>
        </div>
        <span class="text-sm font-semibold text-slate-900">
          {{ formatBRL(orderSummaries.total.get(order.id) ?? 0) }}
        </span>
        <AppIcon name="chevronRight" class="h-4 w-4 shrink-0 text-slate-300" />
      </RouterLink>
      <button
        type="button"
        class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
        aria-label="Excluir pedido"
        @click="orderToRemove = order"
      >
        <AppIcon name="trash" class="h-5 w-5" />
      </button>
    </li>
  </ul>

  <ModalDialog :open="isFormOpen" title="Novo pedido" @close="isFormOpen = false">
    <form class="space-y-4" @submit.prevent="submit">
      <div>
        <label class="label" for="order-customer">Cliente</label>
        <select id="order-customer" v-model="selectedCustomerId" class="input" required>
          <option v-for="customer in customers" :key="customer.id" :value="customer.id">
            {{ customer.name }}
          </option>
        </select>
      </div>
      <div class="flex justify-end gap-2">
        <button type="button" class="btn btn-secondary" @click="isFormOpen = false">
          Cancelar
        </button>
        <button type="submit" class="btn btn-primary" :disabled="saving">
          {{ saving ? 'Criando...' : 'Criar pedido' }}
        </button>
      </div>
    </form>
  </ModalDialog>

  <ConfirmDialog
    :open="orderToRemove !== null"
    title="Excluir pedido"
    :message="`Excluir este pedido remove ${pluralize(orderToRemove ? (orderSummaries.count.get(orderToRemove.id) ?? 0) : 0, 'item', 'itens')}, devolve o estoque e estorna o saldo do cliente. Deseja continuar?`"
    confirm-label="Excluir pedido"
    danger
    :busy="removing"
    @cancel="orderToRemove = null"
    @confirm="confirmRemove"
  />
</template>
