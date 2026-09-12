<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/EmptyState.vue'
import MoneyInput from '../components/MoneyInput.vue'
import PageHeader from '../components/PageHeader.vue'
import SearchSelect from '../components/SearchSelect.vue'
import { useDatabase, useSalesbook } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import { errorMessage, toast } from '../composables/useToast'
import type { CustomerDocType, OrderDocType, OrderItemDocType, ProductDocType } from '../db/types'
import { formatBRL, formatDateTime, pluralize } from '../utils/format'

type ItemGroup = {
  productId: string
  name: string
  items: OrderItemDocType[]
}

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

const productOptions = computed(() =>
  products.value.map((product) => ({
    id: product.id,
    label: product.name,
    sublabel: pluralize(product.stockQuantity, 'unidade em estoque', 'unidades em estoque'),
    disabled: product.stockQuantity < 1,
  })),
)

const groups = computed<ItemGroup[]>(() => {
  const map = new Map<string, OrderItemDocType[]>()
  for (const item of items.value) {
    const list = map.get(item.productId)
    if (list) list.push(item)
    else map.set(item.productId, [item])
  }
  return [...map.entries()]
    .map(([productId, groupItems]) => ({
      productId,
      name: productNames.value.get(productId) ?? 'Produto removido',
      items: [...groupItems].sort((a, b) => a.priceCents - b.priceCents),
    }))
    .sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'))
})

const collapsed = ref<Set<string>>(new Set())

function toggleGroup(productId: string): void {
  const next = new Set(collapsed.value)
  if (next.has(productId)) next.delete(productId)
  else next.add(productId)
  collapsed.value = next
}

function isExpanded(productId: string): boolean {
  return !collapsed.value.has(productId)
}

function groupTotal(group: ItemGroup): number {
  return group.items.reduce((sum, item) => sum + item.priceCents, 0)
}

const total = computed(() => items.value.reduce((sum, item) => sum + item.priceCents, 0))

const selectedProductId = ref('')
const quantity = ref(1)
const priceCents = ref<number | null>(null)
const adding = ref(false)

const selectedProduct = computed(
  () => products.value.find((product) => product.id === selectedProductId.value) ?? null,
)
const maxQuantity = computed(() => selectedProduct.value?.stockQuantity ?? 0)

const itemToRemove = ref<OrderItemDocType | null>(null)
const removingItem = ref(false)
const groupToRemove = ref<ItemGroup | null>(null)
const removingGroup = ref(false)
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
      priceCents: priceCents.value ?? 0,
      quantity: quantity.value,
    })
    toast.success('Itens adicionados.')
    priceCents.value = null
    quantity.value = 1
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

async function confirmRemoveGroup(): Promise<void> {
  if (!groupToRemove.value) return
  removingGroup.value = true
  try {
    await service.removeOrderItems(groupToRemove.value.items.map((item) => item.id))
    toast.success('Itens removidos, estoque devolvido e saldo estornado.')
    groupToRemove.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removingGroup.value = false
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
    class="tap-row mb-4 inline-flex items-center gap-1.5 rounded-lg px-1 text-sm font-medium text-slate-500 hover:text-slate-800"
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
          v-if="groups.length === 0"
          icon="cube"
          title="Nenhum item"
          description="Adicione produtos a este pedido."
        />
        <ul v-else class="space-y-2">
          <template v-for="group in groups" :key="group.productId">
            <li
              v-if="group.items.length === 1"
              class="flex items-center gap-2 rounded-2xl border border-slate-200 px-3 py-2.5"
            >
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-semibold text-slate-800">{{ group.name }}</p>
                <p class="text-xs text-slate-500">1 unidade</p>
              </div>
              <span class="text-sm font-semibold text-slate-900">
                {{ formatBRL(group.items[0]?.priceCents ?? 0) }}
              </span>
              <button
                type="button"
                class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
                aria-label="Remover unidade"
                @click="itemToRemove = group.items[0] ?? null"
              >
                <AppIcon name="trash" class="h-5 w-5" />
              </button>
            </li>
            <li v-else class="overflow-hidden rounded-2xl border border-slate-200">
              <div class="flex items-center gap-1 pr-2">
                <button
                  type="button"
                  class="tap-row flex min-w-0 flex-1 items-center gap-2 rounded-xl px-3 py-2.5 text-left"
                  @click="toggleGroup(group.productId)"
                >
                  <AppIcon
                    :name="isExpanded(group.productId) ? 'chevronDown' : 'chevronRight'"
                    class="h-4 w-4 shrink-0 text-slate-400"
                  />
                  <span class="min-w-0 flex-1 truncate text-sm font-semibold text-slate-800">
                    {{ group.name }}
                  </span>
                  <span class="badge bg-slate-100 text-slate-600">{{ group.items.length }} un.</span>
                </button>
                <button
                  type="button"
                  class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
                  aria-label="Remover todas as unidades do produto"
                  @click="groupToRemove = group"
                >
                  <AppIcon name="trash" class="h-5 w-5" />
                </button>
              </div>
              <ul
                v-if="isExpanded(group.productId)"
                class="divide-y divide-slate-100 border-t border-slate-100"
              >
                <li
                  v-for="item in group.items"
                  :key="item.id"
                  class="flex items-center gap-3 py-2 pl-9 pr-2"
                >
                  <span class="flex-1 text-xs text-slate-500">1 unidade</span>
                  <span class="text-sm font-semibold text-slate-900">
                    {{ formatBRL(item.priceCents) }}
                  </span>
                  <button
                    type="button"
                    class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
                    aria-label="Remover unidade"
                    @click="itemToRemove = item"
                  >
                    <AppIcon name="trash" class="h-5 w-5" />
                  </button>
                </li>
              </ul>
            </li>
          </template>
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
            <SearchSelect
              id="item-product"
              v-model="selectedProductId"
              :options="productOptions"
              placeholder="Buscar produto..."
            />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="label" for="item-quantity">Quantidade</label>
              <input
                id="item-quantity"
                v-model.number="quantity"
                class="input"
                type="number"
                min="1"
                :max="maxQuantity"
                step="1"
                required
              />
            </div>
            <div>
              <label class="label" for="item-price">Preço (R$)</label>
              <MoneyInput
                id="item-price"
                v-model="priceCents"
                required
                :allow-negative="false"
                :show-preview="false"
              />
            </div>
          </div>
          <p v-if="selectedProduct" class="text-xs text-slate-500">
            {{ pluralize(maxQuantity, 'unidade disponível', 'unidades disponíveis') }} em estoque.
          </p>
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
    title="Remover unidade"
    :message="`Remover ${itemToRemove ? productName(itemToRemove.productId) : ''} devolve 1 unidade ao estoque e estorna ${itemToRemove ? formatBRL(itemToRemove.priceCents) : ''} do saldo do cliente.`"
    confirm-label="Remover"
    danger
    :busy="removingItem"
    @cancel="itemToRemove = null"
    @confirm="confirmRemoveItem"
  />

  <ConfirmDialog
    :open="groupToRemove !== null"
    title="Remover produto do pedido"
    :message="`Remover ${groupToRemove ? pluralize(groupToRemove.items.length, 'unidade', 'unidades') : ''} de ${groupToRemove?.name ?? ''} (${groupToRemove ? formatBRL(groupTotal(groupToRemove)) : ''}) devolve o estoque e estorna o valor no saldo do cliente.`"
    confirm-label="Remover tudo"
    danger
    :busy="removingGroup"
    @cancel="groupToRemove = null"
    @confirm="confirmRemoveGroup"
  />

  <ConfirmDialog
    :open="isRemoveOpen"
    title="Excluir pedido"
    :message="`Excluir este pedido remove ${pluralize(items.length, 'item', 'itens')}, devolve o estoque e estorna o saldo do cliente. Deseja continuar?`"
    confirm-label="Excluir pedido"
    danger
    :busy="removingOrder"
    @cancel="isRemoveOpen = false"
    @confirm="removeOrder"
  />
</template>
