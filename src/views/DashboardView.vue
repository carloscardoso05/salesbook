<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import { useDatabase } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import type {
  CustomerDocType,
  OrderDocType,
  OrderItemDocType,
  PaymentDocType,
  ProductDocType,
} from '../db/types'
import { formatBRL, formatDateTime } from '../utils/format'

const db = useDatabase()

const LOW_STOCK_THRESHOLD = 5

const customers = useRxQuery<CustomerDocType>(() => db.customers.find())
const products = useRxQuery<ProductDocType>(() => db.products.find())
const orders = useRxQuery<OrderDocType>(() => db.orders.find({ sort: [{ createdAt: 'desc' }] }))
const orderItems = useRxQuery<OrderItemDocType>(() => db.orderitems.find())
const payments = useRxQuery<PaymentDocType>(() => db.payments.find())

const totalToReceive = computed(() =>
  customers.value.reduce(
    (total, customer) => total + (customer.balance < 0 ? -customer.balance : 0),
    0,
  ),
)
const totalReceived = computed(() =>
  payments.value.reduce((total, payment) => total + payment.amount, 0),
)
const totalSales = computed(() =>
  orderItems.value.reduce((total, item) => total + item.price, 0),
)
const lowStockProducts = computed(() =>
  products.value
    .filter((product) => product.stockQuantity <= LOW_STOCK_THRESHOLD)
    .sort((a, b) => a.stockQuantity - b.stockQuantity),
)
const recentOrders = computed(() => orders.value.slice(0, 5))

const customerNames = computed(() => {
  const map = new Map<string, string>()
  for (const customer of customers.value) map.set(customer.id, customer.name)
  return map
})

const orderSummaries = computed(() => {
  const itemCount = new Map<string, number>()
  const itemTotal = new Map<string, number>()
  for (const item of orderItems.value) {
    itemCount.set(item.orderId, (itemCount.get(item.orderId) ?? 0) + 1)
    itemTotal.set(item.orderId, (itemTotal.get(item.orderId) ?? 0) + item.price)
  }
  return { itemCount, itemTotal }
})

function customerName(customerId: string): string {
  return customerNames.value.get(customerId) ?? 'Cliente removido'
}
</script>

<template>
  <PageHeader title="Início" subtitle="Resumo do seu negócio" />

  <div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
    <div class="card card-pad">
      <p class="text-xs font-medium uppercase tracking-wide text-slate-500">A receber</p>
      <p class="mt-1 text-lg font-bold text-red-600 sm:text-xl">{{ formatBRL(totalToReceive) }}</p>
    </div>
    <div class="card card-pad">
      <p class="text-xs font-medium uppercase tracking-wide text-slate-500">Recebido</p>
      <p class="mt-1 text-lg font-bold text-emerald-600 sm:text-xl">
        {{ formatBRL(totalReceived) }}
      </p>
    </div>
    <div class="card card-pad">
      <p class="text-xs font-medium uppercase tracking-wide text-slate-500">Vendas</p>
      <p class="mt-1 text-lg font-bold text-slate-900 sm:text-xl">{{ formatBRL(totalSales) }}</p>
    </div>
    <div class="card card-pad">
      <p class="text-xs font-medium uppercase tracking-wide text-slate-500">Clientes</p>
      <p class="mt-1 text-lg font-bold text-slate-900 sm:text-xl">{{ customers.length }}</p>
    </div>
  </div>

  <div class="mt-6 grid gap-4 lg:grid-cols-2">
    <section class="card card-pad">
      <div class="mb-3 flex items-center justify-between">
        <h2 class="text-sm font-semibold text-slate-900">Pedidos recentes</h2>
        <RouterLink
          to="/orders"
          class="text-xs font-semibold text-indigo-600 hover:text-indigo-700"
        >
          Ver todos
        </RouterLink>
      </div>
      <EmptyState
        v-if="recentOrders.length === 0"
        icon="receipt"
        title="Nenhum pedido ainda"
        description="Crie um pedido para começar a registrar vendas."
      />
      <ul v-else class="divide-y divide-slate-100">
        <li v-for="order in recentOrders" :key="order.id">
          <RouterLink
            :to="`/orders/${order.id}`"
            class="tap-row -mx-1 flex items-center justify-between gap-3 rounded-xl px-1 py-2.5"
          >
            <div class="min-w-0">
              <p class="truncate text-sm font-medium text-slate-800">
                {{ customerName(order.customerId) }}
              </p>
              <p class="text-xs text-slate-500">
                {{ formatDateTime(order.createdAt) }} ·
                {{ orderSummaries.itemCount.get(order.id) ?? 0 }} item(ns)
              </p>
            </div>
            <div class="flex items-center gap-2">
              <span class="text-sm font-semibold text-slate-900">
                {{ formatBRL(orderSummaries.itemTotal.get(order.id) ?? 0) }}
              </span>
              <AppIcon name="chevronRight" class="h-4 w-4 shrink-0 text-slate-300" />
            </div>
          </RouterLink>
        </li>
      </ul>
    </section>

    <section class="card card-pad">
      <div class="mb-3 flex items-center justify-between">
        <h2 class="text-sm font-semibold text-slate-900">Estoque baixo</h2>
        <RouterLink
          to="/products"
          class="text-xs font-semibold text-indigo-600 hover:text-indigo-700"
        >
          Ver produtos
        </RouterLink>
      </div>
      <EmptyState
        v-if="lowStockProducts.length === 0"
        icon="cube"
        title="Estoque em dia"
        description="Nenhum produto com estoque baixo."
      />
      <ul v-else class="divide-y divide-slate-100">
        <li
          v-for="product in lowStockProducts"
          :key="product.id"
          class="flex items-center justify-between gap-3 py-2.5"
        >
          <span class="truncate text-sm font-medium text-slate-800">{{ product.name }}</span>
          <span
            class="badge"
            :class="
              product.stockQuantity === 0
                ? 'bg-red-100 text-red-700'
                : 'bg-amber-100 text-amber-700'
            "
          >
            {{ product.stockQuantity }} un.
          </span>
        </li>
      </ul>
    </section>
  </div>
</template>
