import 'fake-indexeddb/auto'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createRxDatabase, type RxJsonSchema } from 'rxdb'
import { getRxStorageDexie } from 'rxdb/plugins/storage-dexie'
import { wrappedValidateAjvStorage } from 'rxdb/plugins/validate-ajv'
import { createSalesbookDatabase } from '../src/db/database'
import {
  migrateAdjustmentToCents,
  migrateCustomerToCents,
  migrateOrderItemToCents,
  migratePaymentToCents,
} from '../src/db/migrations'
import type { SalesbookDatabase } from '../src/db/types'
import {
  DuplicateNameError,
  InsufficientStockError,
  InvalidBalanceError,
  InvalidPaymentError,
  InvalidPriceError,
  InvalidQuantityError,
  NoBalanceChangeError,
  ReferencedEntityError,
} from '../src/services/errors'
import { createSalesbookService, type SalesbookService } from '../src/services/salesbook'
import {
  formatAmountInput,
  fromCents,
  normalizeAmountText,
  parseAmountText,
  toCents,
} from '../src/utils/money'

let db: SalesbookDatabase
let service: SalesbookService
let counter = 0

beforeEach(async () => {
  counter += 1
  db = await createSalesbookDatabase(`salesbook-test-${Date.now()}-${counter}`)
  service = createSalesbookService(db)
})

afterEach(async () => {
  await db.remove()
})

async function readCustomer(id: string) {
  const doc = await db.customers.findOne(id).exec()
  if (!doc) throw new Error('Cliente não encontrado no teste')
  return doc.toMutableJSON()
}

async function readProduct(id: string) {
  const doc = await db.products.findOne(id).exec()
  if (!doc) throw new Error('Produto não encontrado no teste')
  return doc.toMutableJSON()
}

describe('utilitários de dinheiro', () => {
  it('converte reais para centavos e formata para edição', () => {
    expect(toCents(30.3)).toBe(3030)
    expect(toCents(0.1)).toBe(10)
    expect(fromCents(3030)).toBe(30.3)
    expect(formatAmountInput(3030)).toBe('30,30')
    expect(formatAmountInput(-2550)).toBe('25,50')
  })

  it('limita a duas casas e interpreta separadores pt-BR', () => {
    expect(normalizeAmountText('25,505')).toBe('25,50')
    expect(normalizeAmountText('25.509')).toBe('25.50')
    expect(normalizeAmountText('1.234,567')).toBe('1234,56')
    expect(normalizeAmountText('abc')).toBe('')
    expect(parseAmountText('1.234,56')).toBe(1234.56)
    expect(parseAmountText('25,5')).toBe(25.5)
    expect(parseAmountText('')).toBeNull()
  })
})

describe('migração para centavos', () => {
  it('converte e arredonda valores antigos com artefatos', () => {
    expect(
      migrateCustomerToCents({
        id: 'c1',
        name: 'Maria',
        nameNormalized: 'maria',
        balance: 30.299999999999997,
      }),
    ).toEqual({ id: 'c1', name: 'Maria', nameNormalized: 'maria', balanceCents: 3030 })

    expect(
      migrateOrderItemToCents({ id: 'i1', orderId: 'o1', productId: 'p1', price: 10.1 }),
    ).toEqual({ id: 'i1', orderId: 'o1', productId: 'p1', priceCents: 1010 })

    expect(
      migratePaymentToCents({
        id: 'p1',
        customerId: 'c1',
        amount: 0.2,
        createdAt: '2026-01-01T00:00:00.000Z',
      }),
    ).toEqual({
      id: 'p1',
      customerId: 'c1',
      amountCents: 20,
      createdAt: '2026-01-01T00:00:00.000Z',
    })

    expect(
      migrateAdjustmentToCents({
        id: 'a1',
        customerId: 'c1',
        amount: -25.5,
        createdAt: '2026-01-01T00:00:00.000Z',
      }),
    ).toEqual({
      id: 'a1',
      customerId: 'c1',
      amountCents: -2550,
      createdAt: '2026-01-01T00:00:00.000Z',
    })
  })
})

describe('migração do banco', () => {
  type LegacyCustomerDoc = {
    id: string
    name: string
    nameNormalized: string
    balance: number
  }

  type LegacyOrderItemDoc = {
    id: string
    orderId: string
    productId: string
    price: number
  }

  type LegacyPaymentDoc = {
    id: string
    customerId: string
    amount: number
    createdAt: string
  }

  type LegacyAdjustmentDoc = {
    id: string
    customerId: string
    amount: number
    createdAt: string
  }

  const idProperty = { type: 'string', maxLength: 100 } as const

  const legacyCustomerSchema: RxJsonSchema<LegacyCustomerDoc> = {
    title: 'customer',
    version: 0,
    primaryKey: 'id',
    type: 'object',
    properties: {
      id: idProperty,
      name: { type: 'string', maxLength: 200 },
      nameNormalized: { type: 'string', maxLength: 200 },
      balance: { type: 'number' },
    },
    required: ['id', 'name', 'nameNormalized', 'balance'],
    indexes: ['nameNormalized'],
  }

  const legacyOrderItemSchema: RxJsonSchema<LegacyOrderItemDoc> = {
    title: 'orderitem',
    version: 0,
    primaryKey: 'id',
    type: 'object',
    properties: {
      id: idProperty,
      orderId: { type: 'string', maxLength: 100 },
      productId: { type: 'string', maxLength: 100 },
      price: { type: 'number', minimum: 0 },
    },
    required: ['id', 'orderId', 'productId', 'price'],
    indexes: ['orderId', 'productId'],
  }

  const legacyPaymentSchema: RxJsonSchema<LegacyPaymentDoc> = {
    title: 'payment',
    version: 0,
    primaryKey: 'id',
    type: 'object',
    properties: {
      id: idProperty,
      customerId: { type: 'string', maxLength: 100 },
      amount: { type: 'number', exclusiveMinimum: 0 },
      createdAt: { type: 'string', maxLength: 32, format: 'date-time' },
    },
    required: ['id', 'customerId', 'amount', 'createdAt'],
    indexes: ['customerId', 'createdAt'],
  }

  const legacyAdjustmentSchema: RxJsonSchema<LegacyAdjustmentDoc> = {
    title: 'adjustment',
    version: 0,
    primaryKey: 'id',
    type: 'object',
    properties: {
      id: idProperty,
      customerId: { type: 'string', maxLength: 100 },
      amount: { type: 'number' },
      createdAt: { type: 'string', maxLength: 32, format: 'date-time' },
    },
    required: ['id', 'customerId', 'amount', 'createdAt'],
    indexes: ['customerId', 'createdAt'],
  }

  it('converte valores em reais para centavos ao abrir banco antigo', async () => {
    const name = `salesbook-migration-${Date.now()}-${counter}`
    const storage = wrappedValidateAjvStorage({ storage: getRxStorageDexie() })
    const legacy = await createRxDatabase({ name, storage })
    await legacy.addCollections({
      customers: { schema: legacyCustomerSchema },
      orderitems: { schema: legacyOrderItemSchema },
      payments: { schema: legacyPaymentSchema },
      adjustments: { schema: legacyAdjustmentSchema },
    })
    await legacy.customers.insert({
      id: 'legacy-customer',
      name: 'Maria',
      nameNormalized: 'maria',
      balance: 30.299999999999997,
    })
    await legacy.orderitems.insert({
      id: 'legacy-item',
      orderId: 'order-1',
      productId: 'product-1',
      price: 10.1,
    })
    await legacy.payments.insert({
      id: 'legacy-payment',
      customerId: 'legacy-customer',
      amount: 0.2,
      createdAt: '2026-01-01T00:00:00.000Z',
    })
    await legacy.adjustments.insert({
      id: 'legacy-adjustment',
      customerId: 'legacy-customer',
      amount: -25.5,
      createdAt: '2026-01-01T00:00:00.000Z',
    })
    await legacy.close()

    const migrated = await createSalesbookDatabase(name)
    try {
      const customer = await migrated.customers.findOne('legacy-customer').exec()
      expect(customer?.balanceCents).toBe(3030)

      const item = await migrated.orderitems.findOne('legacy-item').exec()
      expect(item?.priceCents).toBe(1010)

      const payment = await migrated.payments.findOne('legacy-payment').exec()
      expect(payment?.amountCents).toBe(20)

      const adjustment = await migrated.adjustments.findOne('legacy-adjustment').exec()
      expect(adjustment?.amountCents).toBe(-2550)
    } finally {
      await migrated.remove()
    }
  })
})

describe('addOrderItem', () => {
  it('debita 1 unidade do estoque e o preço do saldo do cliente', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)

    await service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 1050 })

    expect((await readProduct(product.id)).stockQuantity).toBe(1)
    expect((await readCustomer(customer.id)).balanceCents).toBe(-1050)
    expect(await db.orderitems.count().exec()).toBe(1)
  })

  it('cria um registro de item por unidade vendida', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)

    await service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 1000 })
    await service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 2000 })

    expect(await db.orderitems.count().exec()).toBe(2)
    expect((await readProduct(product.id)).stockQuantity).toBe(0)
    expect((await readCustomer(customer.id)).balanceCents).toBe(-3000)
  })

  it('não permite estoque negativo e não altera o saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 0)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 1000 }),
    ).rejects.toBeInstanceOf(InsufficientStockError)

    expect((await readProduct(product.id)).stockQuantity).toBe(0)
    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
    expect(await db.orderitems.count().exec()).toBe(0)
  })

  it('rejeita preço inválido', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 1)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: -100 }),
    ).rejects.toBeInstanceOf(InvalidPriceError)

    expect((await readProduct(product.id)).stockQuantity).toBe(1)
    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
  })
})

describe('addOrderItem com quantidade', () => {
  it('cria um registro por unidade e debita estoque e saldo em lote', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 5)
    const order = await service.createOrder(customer.id)

    const items = await service.addOrderItem({
      orderId: order.id,
      productId: product.id,
      priceCents: 1000,
      quantity: 3,
    })

    expect(items).toHaveLength(3)
    expect(items.every((item) => item.priceCents === 1000)).toBe(true)
    expect(await db.orderitems.count().exec()).toBe(3)
    expect((await readProduct(product.id)).stockQuantity).toBe(2)
    expect((await readCustomer(customer.id)).balanceCents).toBe(-3000)
  })

  it('não vende parcialmente quando o estoque é insuficiente', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({
        orderId: order.id,
        productId: product.id,
        priceCents: 1000,
        quantity: 3,
      }),
    ).rejects.toBeInstanceOf(InsufficientStockError)

    expect((await readProduct(product.id)).stockQuantity).toBe(2)
    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
    expect(await db.orderitems.count().exec()).toBe(0)
  })

  it('rejeita quantidade inválida', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 5)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({
        orderId: order.id,
        productId: product.id,
        priceCents: 1000,
        quantity: 0,
      }),
    ).rejects.toBeInstanceOf(InvalidQuantityError)
    await expect(
      service.addOrderItem({
        orderId: order.id,
        productId: product.id,
        priceCents: 1000,
        quantity: 1.5,
      }),
    ).rejects.toBeInstanceOf(InvalidQuantityError)

    expect((await readProduct(product.id)).stockQuantity).toBe(5)
    expect(await db.orderitems.count().exec()).toBe(0)
  })
})

describe('removeOrderItem', () => {
  it('devolve 1 unidade ao estoque e estorna o preço no saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)
    const [item] = await service.addOrderItem({
      orderId: order.id,
      productId: product.id,
      priceCents: 1050,
    })

    await service.removeOrderItem(item?.id ?? '')

    expect((await readProduct(product.id)).stockQuantity).toBe(2)
    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
    expect(await db.orderitems.count().exec()).toBe(0)
  })
})

describe('removeOrderItems (em lote)', () => {
  it('devolve estoque e estorna saldo de todas as unidades', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 5)
    const order = await service.createOrder(customer.id)
    const items = await service.addOrderItem({
      orderId: order.id,
      productId: product.id,
      priceCents: 1000,
      quantity: 3,
    })

    await service.removeOrderItems(items.map((item) => item.id))

    expect(await db.orderitems.count().exec()).toBe(0)
    expect((await readProduct(product.id)).stockQuantity).toBe(5)
    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
  })

  it('ignora ids inexistentes', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)
    const items = await service.addOrderItem({
      orderId: order.id,
      productId: product.id,
      priceCents: 1000,
      quantity: 2,
    })
    const ids = items.map((item) => item.id)

    await service.removeOrderItems([ids[0] ?? '', 'inexistente'])

    expect(await db.orderitems.count().exec()).toBe(1)
    expect((await readProduct(product.id)).stockQuantity).toBe(1)
    expect((await readCustomer(customer.id)).balanceCents).toBe(-1000)
  })
})

describe('removeOrder', () => {
  it('exclui itens em cascata, devolvendo estoque e estornando saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const coffee = await service.createProduct('Café', 2)
    const tea = await service.createProduct('Chá', 1)
    const order = await service.createOrder(customer.id)

    await service.addOrderItem({ orderId: order.id, productId: coffee.id, priceCents: 1000 })
    await service.addOrderItem({ orderId: order.id, productId: coffee.id, priceCents: 2000 })
    await service.addOrderItem({ orderId: order.id, productId: tea.id, priceCents: 500 })

    expect((await readProduct(coffee.id)).stockQuantity).toBe(0)
    expect((await readProduct(tea.id)).stockQuantity).toBe(0)
    expect((await readCustomer(customer.id)).balanceCents).toBe(-3500)

    await service.removeOrder(order.id)

    expect(await db.orders.count().exec()).toBe(0)
    expect(await db.orderitems.count().exec()).toBe(0)
    expect((await readProduct(coffee.id)).stockQuantity).toBe(2)
    expect((await readProduct(tea.id)).stockQuantity).toBe(1)
    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
  })

  it('remove apenas os itens do pedido excluído', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 5)
    const orderA = await service.createOrder(customer.id)
    const orderB = await service.createOrder(customer.id)

    await service.addOrderItem({ orderId: orderA.id, productId: product.id, priceCents: 1000 })
    await service.addOrderItem({ orderId: orderB.id, productId: product.id, priceCents: 1000 })

    await service.removeOrder(orderA.id)

    expect(await db.orders.count().exec()).toBe(1)
    expect(await db.orderitems.count().exec()).toBe(1)
    expect((await readProduct(product.id)).stockQuantity).toBe(4)
    expect((await readCustomer(customer.id)).balanceCents).toBe(-1000)
  })
})

describe('addPayment', () => {
  it('credita o valor no saldo do cliente', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 1)
    const order = await service.createOrder(customer.id)
    await service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 3000 })

    await service.addPayment({ customerId: customer.id, amountCents: 2000 })

    expect((await readCustomer(customer.id)).balanceCents).toBe(-1000)
    expect(await db.payments.count().exec()).toBe(1)
  })

  it('rejeita valor zero ou negativo sem gravar pagamento', async () => {
    const customer = await service.createCustomer('Maria')

    await expect(
      service.addPayment({ customerId: customer.id, amountCents: 0 }),
    ).rejects.toBeInstanceOf(InvalidPaymentError)
    await expect(
      service.addPayment({ customerId: customer.id, amountCents: -500 }),
    ).rejects.toBeInstanceOf(InvalidPaymentError)

    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
    expect(await db.payments.count().exec()).toBe(0)
  })
})

describe('updatePayment e removePayment', () => {
  it('editar o valor ajusta o saldo pela diferença', async () => {
    const customer = await service.createCustomer('Maria')
    const payment = await service.addPayment({ customerId: customer.id, amountCents: 2000 })

    await service.updatePayment(payment.id, { amountCents: 5000 })
    expect((await readCustomer(customer.id)).balanceCents).toBe(5000)

    await service.updatePayment(payment.id, { amountCents: 500 })
    expect((await readCustomer(customer.id)).balanceCents).toBe(500)
  })

  it('rejeita valor inválido sem alterar o pagamento nem o saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const payment = await service.addPayment({ customerId: customer.id, amountCents: 2000 })

    await expect(
      service.updatePayment(payment.id, { amountCents: 0 }),
    ).rejects.toBeInstanceOf(InvalidPaymentError)

    const stored = await db.payments.findOne(payment.id).exec()
    expect(stored?.amountCents).toBe(2000)
    expect((await readCustomer(customer.id)).balanceCents).toBe(2000)
  })

  it('excluir o pagamento debita o valor do saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 1)
    const order = await service.createOrder(customer.id)
    await service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 3000 })
    const payment = await service.addPayment({ customerId: customer.id, amountCents: 2000 })

    await service.removePayment(payment.id)

    expect(await db.payments.count().exec()).toBe(0)
    expect((await readCustomer(customer.id)).balanceCents).toBe(-3000)
  })
})

describe('saldo inicial do cliente', () => {
  it('cadastra com saldo inicial e registra o ajuste no histórico', async () => {
    const customer = await service.createCustomer('Maria', -2550)

    expect((await readCustomer(customer.id)).balanceCents).toBe(-2550)
    const adjustments = await db.adjustments
      .find({ selector: { customerId: customer.id } })
      .exec()
    expect(adjustments).toHaveLength(1)
    expect(adjustments[0]?.amountCents).toBe(-2550)
  })

  it('não registra ajuste quando o saldo inicial é zero', async () => {
    const customer = await service.createCustomer('Maria')

    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
    expect(await db.adjustments.count().exec()).toBe(0)
  })

  it('rejeita saldo inicial inválido sem cadastrar', async () => {
    await expect(service.createCustomer('Maria', Number.NaN)).rejects.toBeInstanceOf(
      InvalidBalanceError,
    )
    await expect(service.createCustomer('Maria', Number.POSITIVE_INFINITY)).rejects.toBeInstanceOf(
      InvalidBalanceError,
    )
    await expect(service.createCustomer('Maria', 10.5)).rejects.toBeInstanceOf(InvalidBalanceError)
    expect(await db.customers.count().exec()).toBe(0)
  })
})

describe('ajustes de saldo', () => {
  it('cria ajustes positivos e negativos refletindo no saldo', async () => {
    const customer = await service.createCustomer('Maria')

    await service.createBalanceAdjustment({ customerId: customer.id, newBalanceCents: 3000 })
    expect((await readCustomer(customer.id)).balanceCents).toBe(3000)

    await service.createBalanceAdjustment({ customerId: customer.id, newBalanceCents: -1000 })
    expect((await readCustomer(customer.id)).balanceCents).toBe(-1000)
    expect(await db.adjustments.count().exec()).toBe(2)
  })

  it('rejeita ajuste sem diferença', async () => {
    const customer = await service.createCustomer('Maria', 1500)

    await expect(
      service.createBalanceAdjustment({ customerId: customer.id, newBalanceCents: 1500 }),
    ).rejects.toBeInstanceOf(NoBalanceChangeError)

    expect((await readCustomer(customer.id)).balanceCents).toBe(1500)
    expect(await db.adjustments.count().exec()).toBe(1)
  })

  it('edita o ajuste definindo o novo saldo desejado', async () => {
    const customer = await service.createCustomer('Maria')
    const adjustment = await service.createBalanceAdjustment({
      customerId: customer.id,
      newBalanceCents: 2000,
    })

    await service.updateAdjustment(adjustment.id, { newBalanceCents: 5000 })
    expect((await readCustomer(customer.id)).balanceCents).toBe(5000)
    const stored = await db.adjustments.findOne(adjustment.id).exec()
    expect(stored?.amountCents).toBe(5000)

    await service.updateAdjustment(adjustment.id, { newBalanceCents: 500 })
    expect((await readCustomer(customer.id)).balanceCents).toBe(500)
    expect((await db.adjustments.findOne(adjustment.id).exec())?.amountCents).toBe(500)
  })

  it('rejeita edição que não altera o ajuste', async () => {
    const customer = await service.createCustomer('Maria')
    const adjustment = await service.createBalanceAdjustment({
      customerId: customer.id,
      newBalanceCents: 2000,
    })

    await expect(
      service.updateAdjustment(adjustment.id, { newBalanceCents: 2000 }),
    ).rejects.toBeInstanceOf(NoBalanceChangeError)

    expect((await readCustomer(customer.id)).balanceCents).toBe(2000)
    expect((await db.adjustments.findOne(adjustment.id).exec())?.amountCents).toBe(2000)
  })

  it('exclui o ajuste revertendo o efeito no saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const adjustment = await service.createBalanceAdjustment({
      customerId: customer.id,
      newBalanceCents: 2000,
    })

    await service.removeAdjustment(adjustment.id)

    expect(await db.adjustments.count().exec()).toBe(0)
    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
  })

  it('bloqueia excluir cliente com ajustes vinculados', async () => {
    const customer = await service.createCustomer('Maria', 1000)

    await expect(service.removeCustomer(customer.id)).rejects.toBeInstanceOf(
      ReferencedEntityError,
    )
    expect(await db.customers.count().exec()).toBe(1)
  })
})

describe('precisão decimal', () => {
  it('soma valores com centavos sem artefatos', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 5)
    const order = await service.createOrder(customer.id)

    await service.addOrderItem({
      orderId: order.id,
      productId: product.id,
      priceCents: 1010,
      quantity: 2,
    })
    expect((await readCustomer(customer.id)).balanceCents).toBe(-2020)

    await service.addPayment({ customerId: customer.id, amountCents: 10 })
    await service.addPayment({ customerId: customer.id, amountCents: 20 })
    expect((await readCustomer(customer.id)).balanceCents).toBe(-1990)
  })

  it('estorna exatamente o valor pago ao excluir os itens', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 3)
    const order = await service.createOrder(customer.id)
    const items = await service.addOrderItem({
      orderId: order.id,
      productId: product.id,
      priceCents: 333,
      quantity: 3,
    })

    expect((await readCustomer(customer.id)).balanceCents).toBe(-999)

    await service.removeOrderItems(items.map((item) => item.id))

    expect((await readCustomer(customer.id)).balanceCents).toBe(0)
  })

  it('rejeita valores fracionados de centavo', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 1)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 10.5 }),
    ).rejects.toBeInstanceOf(InvalidPriceError)
    await expect(
      service.addPayment({ customerId: customer.id, amountCents: 10.5 }),
    ).rejects.toBeInstanceOf(InvalidPaymentError)
  })
})

describe('nomes únicos (case insensitive)', () => {
  it('impede clientes com nomes iguais ignorando maiúsculas e espaços', async () => {
    await service.createCustomer('Maria')

    await expect(service.createCustomer('maria')).rejects.toBeInstanceOf(DuplicateNameError)
    await expect(service.createCustomer('  MARIA ')).rejects.toBeInstanceOf(DuplicateNameError)

    await service.createCustomer('João')
    expect(await db.customers.count().exec()).toBe(2)
  })

  it('impede produtos com nomes iguais ignorando maiúsculas', async () => {
    await service.createProduct('Café 500g')

    await expect(service.createProduct('café 500g')).rejects.toBeInstanceOf(DuplicateNameError)
    await expect(service.createProduct('CAFÉ 500G')).rejects.toBeInstanceOf(DuplicateNameError)

    await service.createProduct('Chá 250g')
    expect(await db.products.count().exec()).toBe(2)
  })

  it('permite renomear mantendo o próprio nome com casing diferente', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café')

    await service.updateCustomerName(customer.id, 'MARIA')
    await service.updateProduct(product.id, { name: 'CAFÉ' })

    expect((await readCustomer(customer.id)).name).toBe('MARIA')
    expect((await readProduct(product.id)).name).toBe('CAFÉ')
  })

  it('impede renomear para um nome já existente', async () => {
    const ana = await service.createCustomer('Ana')
    await service.createCustomer('Bia')

    await expect(service.updateCustomerName(ana.id, 'bia')).rejects.toBeInstanceOf(
      DuplicateNameError,
    )
  })
})

describe('consultas usadas pela interface', () => {
  it('encontra por id, filtra por referência, ordena e conta', async () => {
    const customer = await service.createCustomer('Maria')
    await service.createCustomer('Ana')
    const product = await service.createProduct('Café', 3)
    const order = await service.createOrder(customer.id)
    await service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 1000 })
    await service.addPayment({ customerId: customer.id, amountCents: 500 })

    const byId = await db.customers.find({ selector: { id: { $eq: customer.id } } }).exec()
    expect(byId).toHaveLength(1)
    expect(byId[0]?.name).toBe('Maria')

    const sorted = await db.customers.find({ sort: [{ nameNormalized: 'asc' }] }).exec()
    expect(sorted.map((doc) => doc.name)).toEqual(['Ana', 'Maria'])

    const orders = await db.orders
      .find({ selector: { customerId: { $eq: customer.id } }, sort: [{ createdAt: 'desc' }] })
      .exec()
    expect(orders).toHaveLength(1)

    const items = await db.orderitems.find({ selector: { orderId: { $eq: order.id } } }).exec()
    expect(items).toHaveLength(1)

    const payments = await db.payments
      .find({ selector: { customerId: { $eq: customer.id } }, sort: [{ createdAt: 'desc' }] })
      .exec()
    expect(payments).toHaveLength(1)

    const itemCount = await db.orderitems.count({ selector: { productId: product.id } }).exec()
    expect(itemCount).toBe(1)
  })
})

describe('exclusões protegidas', () => {
  it('impede excluir cliente com pedidos ou pagamentos', async () => {
    const customer = await service.createCustomer('Maria')
    await service.createOrder(customer.id)

    await expect(service.removeCustomer(customer.id)).rejects.toBeInstanceOf(ReferencedEntityError)
    expect(await db.customers.count().exec()).toBe(1)
  })

  it('impede excluir produto com itens de pedido', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 1)
    const order = await service.createOrder(customer.id)
    await service.addOrderItem({ orderId: order.id, productId: product.id, priceCents: 1000 })

    await expect(service.removeProduct(product.id)).rejects.toBeInstanceOf(ReferencedEntityError)
    expect(await db.products.count().exec()).toBe(1)
  })

  it('permite excluir cliente e produto sem vínculos', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 3)

    await service.removeCustomer(customer.id)
    await service.removeProduct(product.id)

    expect(await db.customers.count().exec()).toBe(0)
    expect(await db.products.count().exec()).toBe(0)
  })
})
