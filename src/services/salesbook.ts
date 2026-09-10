import type { RxDocument } from 'rxdb'
import type {
  AdjustmentDocType,
  CustomerDocType,
  OrderDocType,
  OrderItemDocType,
  PaymentDocType,
  ProductDocType,
  SalesbookDatabase,
} from '../db/types'
import { newId } from '../utils/id'
import { cleanName, normalizeName } from '../utils/text'
import {
  AdjustmentNotFoundError,
  CustomerNotFoundError,
  DuplicateNameError,
  InsufficientStockError,
  InvalidBalanceError,
  InvalidNameError,
  InvalidPaymentError,
  InvalidPriceError,
  InvalidQuantityError,
  InvalidStockError,
  NoBalanceChangeError,
  OrderItemNotFoundError,
  OrderNotFoundError,
  PaymentNotFoundError,
  ProductNotFoundError,
  ReferencedEntityError,
} from './errors'

class Mutex {
  private queue: Promise<unknown> = Promise.resolve()

  run<T>(task: () => Promise<T>): Promise<T> {
    const result = this.queue.then(task, task)
    this.queue = result.catch(() => undefined)
    return result
  }
}

function validateName(name: string): string {
  const cleaned = cleanName(name)
  if (cleaned.length === 0) throw new InvalidNameError()
  return cleaned
}

function validatePriceCents(priceCents: number): number {
  if (!Number.isInteger(priceCents) || priceCents < 0) throw new InvalidPriceError()
  return priceCents
}

function validateStockQuantity(quantity: number): number {
  if (!Number.isInteger(quantity) || quantity < 0) throw new InvalidStockError()
  return quantity
}

function validateItemQuantity(quantity: number): number {
  if (!Number.isInteger(quantity) || quantity < 1) throw new InvalidQuantityError()
  return quantity
}

function validatePaymentAmountCents(amountCents: number): number {
  if (!Number.isInteger(amountCents) || amountCents <= 0) throw new InvalidPaymentError()
  return amountCents
}

function validateBalanceCents(balanceCents: number): number {
  if (!Number.isInteger(balanceCents)) throw new InvalidBalanceError()
  return balanceCents
}

export function createSalesbookService(db: SalesbookDatabase) {
  const mutex = new Mutex()

  async function assertCustomerNameAvailable(
    normalized: string,
    excludeId?: string,
  ): Promise<void> {
    const existing = await db.customers.findOne({ selector: { nameNormalized: normalized } }).exec()
    if (existing && existing.id !== excludeId) {
      throw new DuplicateNameError('cliente', existing.name)
    }
  }

  async function assertProductNameAvailable(normalized: string, excludeId?: string): Promise<void> {
    const existing = await db.products.findOne({ selector: { nameNormalized: normalized } }).exec()
    if (existing && existing.id !== excludeId) {
      throw new DuplicateNameError('produto', existing.name)
    }
  }

  async function createCustomer(name: string, initialBalanceCents = 0): Promise<CustomerDocType> {
    return mutex.run(async () => {
      const displayName = validateName(name)
      const normalized = normalizeName(displayName)
      const balanceCents = validateBalanceCents(initialBalanceCents)
      await assertCustomerNameAvailable(normalized)
      const doc = await db.customers.insert({
        id: newId(),
        name: displayName,
        nameNormalized: normalized,
        balanceCents,
      })
      if (balanceCents !== 0) {
        try {
          await db.adjustments.insert({
            id: newId(),
            customerId: doc.id,
            amountCents: balanceCents,
            createdAt: new Date().toISOString(),
          })
        } catch (error) {
          await doc.remove().catch(() => undefined)
          throw error
        }
      }
      return doc.toMutableJSON()
    })
  }

  async function updateCustomerName(id: string, name: string): Promise<void> {
    await mutex.run(async () => {
      const customer = await db.customers.findOne(id).exec()
      if (!customer) throw new CustomerNotFoundError()
      const displayName = validateName(name)
      const normalized = normalizeName(displayName)
      await assertCustomerNameAvailable(normalized, id)
      await customer.incrementalModify((data) => {
        data.name = displayName
        data.nameNormalized = normalized
        return data
      })
    })
  }

  async function removeCustomer(id: string): Promise<void> {
    await mutex.run(async () => {
      const customer = await db.customers.findOne(id).exec()
      if (!customer) throw new CustomerNotFoundError()
      const orderCount = await db.orders.count({ selector: { customerId: id } }).exec()
      const paymentCount = await db.payments.count({ selector: { customerId: id } }).exec()
      const adjustmentCount = await db.adjustments.count({ selector: { customerId: id } }).exec()
      if (orderCount > 0 || paymentCount > 0 || adjustmentCount > 0) {
        throw new ReferencedEntityError(
          'cliente',
          'existem pedidos, pagamentos ou ajustes vinculados.',
        )
      }
      await customer.remove()
    })
  }

  async function createProduct(name: string, stockQuantity = 0): Promise<ProductDocType> {
    return mutex.run(async () => {
      const displayName = validateName(name)
      const normalized = normalizeName(displayName)
      const stock = validateStockQuantity(stockQuantity)
      await assertProductNameAvailable(normalized)
      const doc = await db.products.insert({
        id: newId(),
        name: displayName,
        nameNormalized: normalized,
        stockQuantity: stock,
      })
      return doc.toMutableJSON()
    })
  }

  async function updateProduct(
    id: string,
    changes: { name?: string; stockQuantity?: number },
  ): Promise<void> {
    await mutex.run(async () => {
      const product = await db.products.findOne(id).exec()
      if (!product) throw new ProductNotFoundError()
      const nextName = changes.name === undefined ? product.name : validateName(changes.name)
      const nextNormalized = normalizeName(nextName)
      const nextStock =
        changes.stockQuantity === undefined
          ? product.stockQuantity
          : validateStockQuantity(changes.stockQuantity)
      if (nextNormalized !== product.nameNormalized) {
        await assertProductNameAvailable(nextNormalized, id)
      }
      await product.incrementalModify((data) => {
        data.name = nextName
        data.nameNormalized = nextNormalized
        data.stockQuantity = nextStock
        return data
      })
    })
  }

  async function removeProduct(id: string): Promise<void> {
    await mutex.run(async () => {
      const product = await db.products.findOne(id).exec()
      if (!product) throw new ProductNotFoundError()
      const itemCount = await db.orderitems.count({ selector: { productId: id } }).exec()
      if (itemCount > 0) {
        throw new ReferencedEntityError('produto', 'existem itens de pedido vinculados.')
      }
      await product.remove()
    })
  }

  async function createOrder(customerId: string): Promise<OrderDocType> {
    return mutex.run(async () => {
      const customer = await db.customers.findOne(customerId).exec()
      if (!customer) throw new CustomerNotFoundError()
      const doc = await db.orders.insert({
        id: newId(),
        customerId: customer.id,
        createdAt: new Date().toISOString(),
      })
      return doc.toMutableJSON()
    })
  }

  async function addOrderItem(input: {
    orderId: string
    productId: string
    priceCents: number
    quantity?: number
  }): Promise<OrderItemDocType[]> {
    return mutex.run(async () => {
      const priceCents = validatePriceCents(input.priceCents)
      const quantity = validateItemQuantity(input.quantity ?? 1)
      const order = await db.orders.findOne(input.orderId).exec()
      if (!order) throw new OrderNotFoundError()
      const product = await db.products.findOne(input.productId).exec()
      if (!product) throw new ProductNotFoundError()
      const customer = await db.customers.findOne(order.customerId).exec()
      if (!customer) throw new CustomerNotFoundError()
      if (product.stockQuantity < quantity) throw new InsufficientStockError(product.name)

      const totalCents = priceCents * quantity
      let stockDebited = false
      let balanceDebited = false
      try {
        await product.incrementalModify((data) => {
          if (data.stockQuantity < quantity) throw new InsufficientStockError(data.name)
          data.stockQuantity -= quantity
          return data
        })
        stockDebited = true

        await customer.incrementalModify((data) => {
          data.balanceCents -= totalCents
          return data
        })
        balanceDebited = true

        const items: OrderItemDocType[] = []
        for (let index = 0; index < quantity; index += 1) {
          const item = await db.orderitems.insert({
            id: newId(),
            orderId: order.id,
            productId: product.id,
            priceCents,
          })
          items.push(item.toMutableJSON())
        }
        return items
      } catch (error) {
        if (balanceDebited) {
          await customer
            .incrementalModify((data) => {
              data.balanceCents += totalCents
              return data
            })
            .catch(() => undefined)
        }
        if (stockDebited) {
          await product
            .incrementalModify((data) => {
              data.stockQuantity += quantity
              return data
            })
            .catch(() => undefined)
        }
        throw error
      }
    })
  }

  async function removeOrderItemDocument(item: RxDocument<OrderItemDocType>): Promise<void> {
    const order = await db.orders.findOne(item.orderId).exec()
    const product = await db.products.findOne(item.productId).exec()
    const customer = order ? await db.customers.findOne(order.customerId).exec() : null

    let stockRestored = false
    let balanceCredited = false
    try {
      if (product) {
        await product.incrementalModify((data) => {
          data.stockQuantity += 1
          return data
        })
        stockRestored = true
      }
      if (customer) {
        await customer.incrementalModify((data) => {
          data.balanceCents += item.priceCents
          return data
        })
        balanceCredited = true
      }
      await item.remove()
    } catch (error) {
      if (balanceCredited && customer) {
        await customer
          .incrementalModify((data) => {
            data.balanceCents -= item.priceCents
            return data
          })
          .catch(() => undefined)
      }
      if (stockRestored && product) {
        await product
          .incrementalModify((data) => {
            data.stockQuantity -= 1
            return data
          })
          .catch(() => undefined)
      }
      throw error
    }
  }

  async function removeOrderItem(itemId: string): Promise<void> {
    await mutex.run(async () => {
      const item = await db.orderitems.findOne(itemId).exec()
      if (!item) throw new OrderItemNotFoundError()
      await removeOrderItemDocument(item)
    })
  }

  async function removeOrderItems(itemIds: string[]): Promise<void> {
    await mutex.run(async () => {
      for (const itemId of itemIds) {
        const item = await db.orderitems.findOne(itemId).exec()
        if (!item) continue
        await removeOrderItemDocument(item)
      }
    })
  }

  async function removeOrder(orderId: string): Promise<void> {
    await mutex.run(async () => {
      const order = await db.orders.findOne(orderId).exec()
      if (!order) throw new OrderNotFoundError()
      const items = await db.orderitems.find({ selector: { orderId } }).exec()
      for (const item of items) {
        await removeOrderItemDocument(item)
      }
      await order.remove()
    })
  }

  async function addPayment(input: {
    customerId: string
    amountCents: number
  }): Promise<PaymentDocType> {
    return mutex.run(async () => {
      const amountCents = validatePaymentAmountCents(input.amountCents)
      const customer = await db.customers.findOne(input.customerId).exec()
      if (!customer) throw new CustomerNotFoundError()

      await customer.incrementalModify((data) => {
        data.balanceCents += amountCents
        return data
      })
      try {
        const payment = await db.payments.insert({
          id: newId(),
          customerId: customer.id,
          amountCents,
          createdAt: new Date().toISOString(),
        })
        return payment.toMutableJSON()
      } catch (error) {
        await customer
          .incrementalModify((data) => {
            data.balanceCents -= amountCents
            return data
          })
          .catch(() => undefined)
        throw error
      }
    })
  }

  async function updatePayment(
    paymentId: string,
    changes: { amountCents: number },
  ): Promise<void> {
    await mutex.run(async () => {
      const amountCents = validatePaymentAmountCents(changes.amountCents)
      const payment = await db.payments.findOne(paymentId).exec()
      if (!payment) throw new PaymentNotFoundError()
      const customer = await db.customers.findOne(payment.customerId).exec()
      if (!customer) throw new CustomerNotFoundError()

      const deltaCents = amountCents - payment.amountCents
      await customer.incrementalModify((data) => {
        data.balanceCents += deltaCents
        return data
      })
      try {
        await payment.incrementalModify((data) => {
          data.amountCents = amountCents
          return data
        })
      } catch (error) {
        await customer
          .incrementalModify((data) => {
            data.balanceCents -= deltaCents
            return data
          })
          .catch(() => undefined)
        throw error
      }
    })
  }

  async function removePayment(paymentId: string): Promise<void> {
    await mutex.run(async () => {
      const payment = await db.payments.findOne(paymentId).exec()
      if (!payment) throw new PaymentNotFoundError()
      const customer = await db.customers.findOne(payment.customerId).exec()

      let balanceDebited = false
      try {
        if (customer) {
          await customer.incrementalModify((data) => {
            data.balanceCents -= payment.amountCents
            return data
          })
          balanceDebited = true
        }
        await payment.remove()
      } catch (error) {
        if (balanceDebited && customer) {
          await customer
            .incrementalModify((data) => {
              data.balanceCents += payment.amountCents
              return data
            })
            .catch(() => undefined)
        }
        throw error
      }
    })
  }

  async function createBalanceAdjustment(input: {
    customerId: string
    newBalanceCents: number
  }): Promise<AdjustmentDocType> {
    return mutex.run(async () => {
      const newBalanceCents = validateBalanceCents(input.newBalanceCents)
      const customer = await db.customers.findOne(input.customerId).exec()
      if (!customer) throw new CustomerNotFoundError()

      const deltaCents = newBalanceCents - customer.balanceCents
      if (deltaCents === 0) throw new NoBalanceChangeError()

      await customer.incrementalModify((data) => {
        data.balanceCents += deltaCents
        return data
      })
      try {
        const adjustment = await db.adjustments.insert({
          id: newId(),
          customerId: customer.id,
          amountCents: deltaCents,
          createdAt: new Date().toISOString(),
        })
        return adjustment.toMutableJSON()
      } catch (error) {
        await customer
          .incrementalModify((data) => {
            data.balanceCents -= deltaCents
            return data
          })
          .catch(() => undefined)
        throw error
      }
    })
  }

  async function updateAdjustment(
    adjustmentId: string,
    changes: { newBalanceCents: number },
  ): Promise<void> {
    await mutex.run(async () => {
      const newBalanceCents = validateBalanceCents(changes.newBalanceCents)
      const adjustment = await db.adjustments.findOne(adjustmentId).exec()
      if (!adjustment) throw new AdjustmentNotFoundError()
      const customer = await db.customers.findOne(adjustment.customerId).exec()
      if (!customer) throw new CustomerNotFoundError()

      const balanceWithoutAdjustmentCents = customer.balanceCents - adjustment.amountCents
      const newDeltaCents = newBalanceCents - balanceWithoutAdjustmentCents
      if (newDeltaCents === adjustment.amountCents) {
        throw new NoBalanceChangeError(
          'O novo saldo não altera este ajuste. Para removê-lo, exclua o ajuste.',
        )
      }
      const balanceDeltaCents = newDeltaCents - adjustment.amountCents

      await customer.incrementalModify((data) => {
        data.balanceCents += balanceDeltaCents
        return data
      })
      try {
        await adjustment.incrementalModify((data) => {
          data.amountCents = newDeltaCents
          return data
        })
      } catch (error) {
        await customer
          .incrementalModify((data) => {
            data.balanceCents -= balanceDeltaCents
            return data
          })
          .catch(() => undefined)
        throw error
      }
    })
  }

  async function removeAdjustment(adjustmentId: string): Promise<void> {
    await mutex.run(async () => {
      const adjustment = await db.adjustments.findOne(adjustmentId).exec()
      if (!adjustment) throw new AdjustmentNotFoundError()
      const customer = await db.customers.findOne(adjustment.customerId).exec()

      let balanceRestored = false
      try {
        if (customer) {
          await customer.incrementalModify((data) => {
            data.balanceCents -= adjustment.amountCents
            return data
          })
          balanceRestored = true
        }
        await adjustment.remove()
      } catch (error) {
        if (balanceRestored && customer) {
          await customer
            .incrementalModify((data) => {
              data.balanceCents += adjustment.amountCents
              return data
            })
            .catch(() => undefined)
        }
        throw error
      }
    })
  }

  return {
    createCustomer,
    updateCustomerName,
    removeCustomer,
    createProduct,
    updateProduct,
    removeProduct,
    createOrder,
    addOrderItem,
    removeOrderItem,
    removeOrderItems,
    removeOrder,
    addPayment,
    updatePayment,
    removePayment,
    createBalanceAdjustment,
    updateAdjustment,
    removeAdjustment,
  }
}

export type SalesbookService = ReturnType<typeof createSalesbookService>
