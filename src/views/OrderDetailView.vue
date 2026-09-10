<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import { useDatabase, useSalesbook } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import { errorMessage, toast } from '../composables/useToast'
import type { CustomerDocType, OrderDocType, OrderItemDocType, ProductDocType } from '../db/types'
import { formatBRL, formatDateTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const db = useDatabase()
const service = useSalesbook()

const orderId = computed(() => String(route.params.id))

const orders = useRxQuery<OrderDocType>(
  () => db.orders.find({ selector: { id: { $eq: orderId.value } } }),
  [orderId],
)
const order = computed(() => orders.value[0] ?? null)

const customerId = computed(() => order.value?.customerId ?? '')
const customers = useRxQuery<CustomerDocType>(
  () => db.customers.find({ selector: { id: { $eq: customerId.value } } }),
  [customerId],
)
const customer = computed(() => customers.value[0] ?? null)

const items = useRxQuery<OrderItemDocType>(
  () => db.orderitems.find({ selector: { orderId: { $eq: orderId.value } } }),
  [orderId],
)

const products = useRxQuery<ProductDocType>(() =>
  db.products.find({ sort: [{ nameNormalized: 'asc' }] }),
)

const productNames = computed(() => {
  const map = new Map<string, string>()
  for (const product of products.value) map.set(product.id, product.name)
  return map
})

const total = computed(() => items.value.reduce((sum, item) => sum + item.price, 0))

const selectedProductId = ref('')
const price = ref<number | null>(null)
const adding = ref(false)
const itemToRemove = ref<OrderItemDocType | null>(null)
const removingItem = ref(false)
const isRemoveOpen = ref(false)
const removingOrder = ref(false)

watch(
  products,
  (list) => {
    const current = list.find((product) => product.id === selectedProductId.value)
    if (!current || current.stockQuantity < 1) {
      selectedProductId.value = list.find((product) => product.stockQuantity > 0)?.id ?? ''
    }
  },
  { immediate: true },
)

async function addItem(): Promise<void> {
  adding.value = true
  try {
    await service.addOrderItem({
      orderId: orderId.value,
      productId: selectedProductId.value,
      price: price.value ?? Number.NaN,
    })
    toast.success('Item adicionado.')
    price.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    adding.value = false
  }
}

async function confirmRemoveItem(): Promise<void> {
  if (!itemToRemove.value) return
  removingItem.value = true
  try {
    await service.removeOrderItem(itemToRemove.value.id)
    toast.success('Item removido, estoque devolvido e saldo estornado.')
    itemToRemove.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removingItem.value = false
  }
}

async function removeOrder(): Promise<void> {
  if (!order.value) return
  removingOrder.value = true
  try {
    await service.removeOrder(order.value.id)
    toast.success('Pedido excluído, estoque devolvido e saldo estornado.')
    await router.push('/orders')
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removingOrder.value = false
  }
}

function productName(productId: string): string {
  return productNames.value.get(productId) ?? 'Produto removido'
}
</script>

<template>
  <RouterLink
    to="/orders"
    class="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-800"
  >
    <AppIcon name="arrowLeft" class="h-4 w-4" />
    Pedidos
  </RouterLink>

  <template v-if="order">
    <PageHeader
      :title="`Pedido de ${customer?.name ?? 'cliente'}`"
      :subtitle="formatDateTime(order.createdAt)"
    >
      <template #actions>
        <button
          type="button"
          class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
          aria-label="Excluir pedido"
          @click="isRemoveOpen = true"
        >
          <AppIcon name="trash" class="h-5 w-5" />
        </button>
      </template>
    </PageHeader>

    <div class="grid gap-4 lg:grid-cols-[1fr_320px]">
      <section class="card card-pad">
        <h2 class="mb-3 text-sm font-semibold text-slate-900">Itens</h2>
        <EmptyState
          v-if="items.length === 0"
          icon="cube"
          title="Nenhum item"
          description="Adicione produtos a este pedido."
        />
        <ul v-else class="divide-y divide-slate-100">
          <li
            v-for="item in items"
            :key="item.id"
            class="flex items-center justify-between gap-3 py-2.5"
          >
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-slate-800">
                {{ productName(item.productId) }}
              </p>
              <p class="text-xs text-slate-500">1 unidade</p>
            </div>
            <span class="text-sm font-semibold text-slate-900">{{ formatBRL(item.price) }}</span>
            <button
              type="button"
              class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
              aria-label="Remover item"
              @click="itemToRemove = item"
            >
              <AppIcon name="trash" class="h-5 w-5" />
            </button>
          </li>
        </ul>
        <div class="mt-4 flex items-center justify-between border-t border-slate-200 pt-3">
          <span class="text-sm font-medium text-slate-600">Total</span>
          <span class="text-lg font-bold text-slate-900">{{ formatBRL(total) }}</span>
        </div>
      </section>

      <section class="card card-pad h-fit">
        <h2 class="mb-3 text-sm font-semibold text-slate-900">Adicionar item</h2>
        <form class="space-y-4" @submit.prevent="addItem">
          <div>
            <label class="label" for="item-product">Produto</label>
            <select id="item-product" v-model="selectedProductId" class="input" required>
              <option
                v-for="product in products"
                :key="product.id"
                :value="product.id"
                :disabled="product.stockQuantity < 1"
              >
                {{ product.name }} ({{ product.stockQuantity }} em estoque)
              </option>
            </select>
          </div>
          <div>
            <label class="label" for="item-price">Preço (R$)</label>
            <input
              id="item-price"
              v-model.number="price"
              class="input"
              type="number"
              min="0"
              step="0.01"
              required
              placeholder="0,00"
            />
          </div>
          <button
            type="submit"
            class="btn btn-primary w-full"
            :disabled="adding || products.length === 0 || selectedProductId === ''"
          >
            <AppIcon name="plus" class="h-4 w-4" />
            {{ adding ? 'Adicionando...' : 'Adicionar' }}
          </button>
          <p v-if="products.length === 0" class="text-xs text-slate-500">
            Cadastre um produto antes de vender.
          </p>
        </form>
      </section>
    </div>
  </template>

  <EmptyState
    v-else
    icon="receipt"
    title="Pedido não encontrado"
    description="Ele pode ter sido excluído."
  >
    <RouterLink to="/orders" class="btn btn-primary">Voltar para pedidos</RouterLink>
  </EmptyState>

  <ConfirmDialog
    :open="itemToRemove !== null"
    title="Remover item"
    :message="`Remover ${itemToRemove ? productName(itemToRemove.productId) : ''} devolve 1 unidade ao estoque e estorna ${itemToRemove ? formatBRL(itemToRemove.price) : ''} do saldo do cliente.`"
    confirm-label="Remover"
    danger
    :busy="removingItem"
    @cancel="itemToRemove = null"
    @confirm="confirmRemoveItem"
  />

  <ConfirmDialog
    :open="isRemoveOpen"
    title="Excluir pedido"
    :message="`Excluir este pedido remove ${items.length} item(ns), devolve o estoque e estorna o saldo do cliente. Deseja continuar?`"
    confirm-label="Excluir pedido"
    danger
    :busy="removingOrder"
    @cancel="isRemoveOpen = false"
    @confirm="removeOrder"
  />
</template>
