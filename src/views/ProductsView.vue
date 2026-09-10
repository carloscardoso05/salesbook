<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import EmptyState from '../components/EmptyState.vue'
import ModalDialog from '../components/ModalDialog.vue'
import PageHeader from '../components/PageHeader.vue'
import { useDatabase, useSalesbook } from '../composables/useDatabase'
import { useRxQuery } from '../composables/useRxQuery'
import { errorMessage, toast } from '../composables/useToast'
import type { ProductDocType } from '../db/types'
import { pluralize } from '../utils/format'
import { stockBadgeClass } from '../utils/stock'

const route = useRoute()
const router = useRouter()
const db = useDatabase()
const service = useSalesbook()

const products = useRxQuery<ProductDocType>(() =>
  db.products.find({ sort: [{ nameNormalized: 'asc' }] }),
)

const isFormOpen = ref(false)
const editing = ref<ProductDocType | null>(null)
const name = ref('')
const stock = ref(0)
const saving = ref(false)
const productToRemove = ref<ProductDocType | null>(null)
const removing = ref(false)

function openCreate(): void {
  editing.value = null
  name.value = ''
  stock.value = 0
  isFormOpen.value = true
}

function openEdit(product: ProductDocType): void {
  editing.value = product
  name.value = product.name
  stock.value = product.stockQuantity
  isFormOpen.value = true
}

function openEditFromQuery(): void {
  const editId = route.query.edit
  if (typeof editId !== 'string') return
  const product = products.value.find((item) => item.id === editId)
  if (!product) return
  openEdit(product)
  void router.replace({ query: {} })
}

watch([products, () => route.query.edit], openEditFromQuery, { immediate: true })

async function submit(): Promise<void> {
  saving.value = true
  try {
    if (editing.value) {
      await service.updateProduct(editing.value.id, {
        name: name.value,
        stockQuantity: stock.value,
      })
      toast.success('Produto atualizado.')
    } else {
      await service.createProduct(name.value, stock.value)
      toast.success('Produto criado.')
    }
    isFormOpen.value = false
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    saving.value = false
  }
}

async function confirmRemove(): Promise<void> {
  if (!productToRemove.value) return
  removing.value = true
  try {
    await service.removeProduct(productToRemove.value.id)
    toast.success('Produto excluído.')
    productToRemove.value = null
  } catch (error) {
    toast.error(errorMessage(error))
  } finally {
    removing.value = false
  }
}
</script>

<template>
  <PageHeader title="Produtos" :subtitle="pluralize(products.length, 'cadastrado', 'cadastrados')">
    <template #actions>
      <button type="button" class="btn btn-primary" @click="openCreate">
        <AppIcon name="plus" class="h-4 w-4" />
        Novo produto
      </button>
    </template>
  </PageHeader>

  <EmptyState
    v-if="products.length === 0"
    icon="cube"
    title="Nenhum produto cadastrado"
    description="Cadastre um produto com a quantidade em estoque."
  >
    <button type="button" class="btn btn-primary" @click="openCreate">Novo produto</button>
  </EmptyState>

  <ul v-else class="space-y-2.5">
    <li v-for="product in products" :key="product.id" class="card card-pad flex items-center gap-1">
      <button
        type="button"
        class="tap-row flex min-w-0 flex-1 items-center gap-3 rounded-xl py-1 text-left"
        @click="openEdit(product)"
      >
        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-semibold text-slate-900">{{ product.name }}</p>
          <p class="mt-0.5 text-xs text-slate-500">Estoque</p>
        </div>
        <span class="badge" :class="stockBadgeClass(product.stockQuantity)">
          {{ product.stockQuantity }} un.
        </span>
        <AppIcon name="chevronRight" class="h-4 w-4 shrink-0 text-slate-300" />
      </button>
      <button
        type="button"
        class="btn btn-ghost btn-icon text-slate-400 hover:text-red-600"
        aria-label="Excluir produto"
        @click="productToRemove = product"
      >
        <AppIcon name="trash" class="h-5 w-5" />
      </button>
    </li>
  </ul>

  <ModalDialog
    :open="isFormOpen"
    :title="editing ? 'Editar produto' : 'Novo produto'"
    @close="isFormOpen = false"
  >
    <form class="space-y-4" @submit.prevent="submit">
      <div>
        <label class="label" for="product-name">Nome</label>
        <input
          id="product-name"
          v-model="name"
          class="input"
          type="text"
          required
          maxlength="200"
          placeholder="Ex.: Café 500g"
        />
        <p class="mt-1.5 text-xs text-slate-500">O nome precisa ser único.</p>
      </div>
      <div>
        <label class="label" for="product-stock">Quantidade em estoque</label>
        <input
          id="product-stock"
          v-model.number="stock"
          class="input"
          type="number"
          min="0"
          step="1"
          required
        />
        <p v-if="editing" class="mt-1.5 text-xs text-slate-500">
          Use este campo para repor ou corrigir o estoque.
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
    :open="productToRemove !== null"
    title="Excluir produto"
    :message="`Tem certeza que deseja excluir ${productToRemove?.name}? Esta ação não pode ser desfeita.`"
    confirm-label="Excluir"
    danger
    :busy="removing"
    @cancel="productToRemove = null"
    @confirm="confirmRemove"
  />
</template>
