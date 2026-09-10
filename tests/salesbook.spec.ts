import 'fake-indexeddb/auto'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createSalesbookDatabase } from '../src/db/database'
import type { SalesbookDatabase } from '../src/db/types'
import {
  DuplicateNameError,
  InsufficientStockError,
  InvalidPaymentError,
  InvalidPriceError,
  InvalidQuantityError,
  ReferencedEntityError,
} from '../src/services/errors'
import { createSalesbookService, type SalesbookService } from '../src/services/salesbook'

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

describe('addOrderItem', () => {
  it('debita 1 unidade do estoque e o preço do saldo do cliente', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)

    await service.addOrderItem({ orderId: order.id, productId: product.id, price: 10.5 })

    expect((await readProduct(product.id)).stockQuantity).toBe(1)
    expect((await readCustomer(customer.id)).balance).toBe(-10.5)
    expect(await db.orderitems.count().exec()).toBe(1)
  })

  it('cria um registro de item por unidade vendida', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)

    await service.addOrderItem({ orderId: order.id, productId: product.id, price: 10 })
    await service.addOrderItem({ orderId: order.id, productId: product.id, price: 20 })

    expect(await db.orderitems.count().exec()).toBe(2)
    expect((await readProduct(product.id)).stockQuantity).toBe(0)
    expect((await readCustomer(customer.id)).balance).toBe(-30)
  })

  it('não permite estoque negativo e não altera o saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 0)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({ orderId: order.id, productId: product.id, price: 10 }),
    ).rejects.toBeInstanceOf(InsufficientStockError)

    expect((await readProduct(product.id)).stockQuantity).toBe(0)
    expect((await readCustomer(customer.id)).balance).toBe(0)
    expect(await db.orderitems.count().exec()).toBe(0)
  })

  it('rejeita preço inválido', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 1)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({ orderId: order.id, productId: product.id, price: -1 }),
    ).rejects.toBeInstanceOf(InvalidPriceError)

    expect((await readProduct(product.id)).stockQuantity).toBe(1)
    expect((await readCustomer(customer.id)).balance).toBe(0)
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
      price: 10,
      quantity: 3,
    })

    expect(items).toHaveLength(3)
    expect(items.every((item) => item.price === 10)).toBe(true)
    expect(await db.orderitems.count().exec()).toBe(3)
    expect((await readProduct(product.id)).stockQuantity).toBe(2)
    expect((await readCustomer(customer.id)).balance).toBe(-30)
  })

  it('não vende parcialmente quando o estoque é insuficiente', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({
        orderId: order.id,
        productId: product.id,
        price: 10,
        quantity: 3,
      }),
    ).rejects.toBeInstanceOf(InsufficientStockError)

    expect((await readProduct(product.id)).stockQuantity).toBe(2)
    expect((await readCustomer(customer.id)).balance).toBe(0)
    expect(await db.orderitems.count().exec()).toBe(0)
  })

  it('rejeita quantidade inválida', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 5)
    const order = await service.createOrder(customer.id)

    await expect(
      service.addOrderItem({ orderId: order.id, productId: product.id, price: 10, quantity: 0 }),
    ).rejects.toBeInstanceOf(InvalidQuantityError)
    await expect(
      service.addOrderItem({ orderId: order.id, productId: product.id, price: 10, quantity: 1.5 }),
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
      price: 10.5,
    })

    await service.removeOrderItem(item?.id ?? '')

    expect((await readProduct(product.id)).stockQuantity).toBe(2)
    expect((await readCustomer(customer.id)).balance).toBe(0)
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
      price: 10,
      quantity: 3,
    })

    await service.removeOrderItems(items.map((item) => item.id))

    expect(await db.orderitems.count().exec()).toBe(0)
    expect((await readProduct(product.id)).stockQuantity).toBe(5)
    expect((await readCustomer(customer.id)).balance).toBe(0)
  })

  it('ignora ids inexistentes', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 2)
    const order = await service.createOrder(customer.id)
    const items = await service.addOrderItem({
      orderId: order.id,
      productId: product.id,
      price: 10,
      quantity: 2,
    })
    const ids = items.map((item) => item.id)

    await service.removeOrderItems([ids[0] ?? '', 'inexistente'])

    expect(await db.orderitems.count().exec()).toBe(1)
    expect((await readProduct(product.id)).stockQuantity).toBe(1)
    expect((await readCustomer(customer.id)).balance).toBe(-10)
  })
})

describe('removeOrder', () => {
  it('exclui itens em cascata, devolvendo estoque e estornando saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const coffee = await service.createProduct('Café', 2)
    const tea = await service.createProduct('Chá', 1)
    const order = await service.createOrder(customer.id)

    await service.addOrderItem({ orderId: order.id, productId: coffee.id, price: 10 })
    await service.addOrderItem({ orderId: order.id, productId: coffee.id, price: 20 })
    await service.addOrderItem({ orderId: order.id, productId: tea.id, price: 5 })

    expect((await readProduct(coffee.id)).stockQuantity).toBe(0)
    expect((await readProduct(tea.id)).stockQuantity).toBe(0)
    expect((await readCustomer(customer.id)).balance).toBe(-35)

    await service.removeOrder(order.id)

    expect(await db.orders.count().exec()).toBe(0)
    expect(await db.orderitems.count().exec()).toBe(0)
    expect((await readProduct(coffee.id)).stockQuantity).toBe(2)
    expect((await readProduct(tea.id)).stockQuantity).toBe(1)
    expect((await readCustomer(customer.id)).balance).toBe(0)
  })

  it('remove apenas os itens do pedido excluído', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 5)
    const orderA = await service.createOrder(customer.id)
    const orderB = await service.createOrder(customer.id)

    await service.addOrderItem({ orderId: orderA.id, productId: product.id, price: 10 })
    await service.addOrderItem({ orderId: orderB.id, productId: product.id, price: 10 })

    await service.removeOrder(orderA.id)

    expect(await db.orders.count().exec()).toBe(1)
    expect(await db.orderitems.count().exec()).toBe(1)
    expect((await readProduct(product.id)).stockQuantity).toBe(4)
    expect((await readCustomer(customer.id)).balance).toBe(-10)
  })
})

describe('addPayment', () => {
  it('credita o valor no saldo do cliente', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 1)
    const order = await service.createOrder(customer.id)
    await service.addOrderItem({ orderId: order.id, productId: product.id, price: 30 })

    await service.addPayment({ customerId: customer.id, amount: 20 })

    expect((await readCustomer(customer.id)).balance).toBe(-10)
    expect(await db.payments.count().exec()).toBe(1)
  })

  it('rejeita valor zero ou negativo sem gravar pagamento', async () => {
    const customer = await service.createCustomer('Maria')

    await expect(
      service.addPayment({ customerId: customer.id, amount: 0 }),
    ).rejects.toBeInstanceOf(InvalidPaymentError)
    await expect(
      service.addPayment({ customerId: customer.id, amount: -5 }),
    ).rejects.toBeInstanceOf(InvalidPaymentError)

    expect((await readCustomer(customer.id)).balance).toBe(0)
    expect(await db.payments.count().exec()).toBe(0)
  })
})

describe('updatePayment e removePayment', () => {
  it('editar o valor ajusta o saldo pela diferença', async () => {
    const customer = await service.createCustomer('Maria')
    const payment = await service.addPayment({ customerId: customer.id, amount: 20 })

    await service.updatePayment(payment.id, { amount: 50 })
    expect((await readCustomer(customer.id)).balance).toBe(50)

    await service.updatePayment(payment.id, { amount: 5 })
    expect((await readCustomer(customer.id)).balance).toBe(5)
  })

  it('rejeita valor inválido sem alterar o pagamento nem o saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const payment = await service.addPayment({ customerId: customer.id, amount: 20 })

    await expect(service.updatePayment(payment.id, { amount: 0 })).rejects.toBeInstanceOf(
      InvalidPaymentError,
    )

    const stored = await db.payments.findOne(payment.id).exec()
    expect(stored?.amount).toBe(20)
    expect((await readCustomer(customer.id)).balance).toBe(20)
  })

  it('excluir o pagamento debita o valor do saldo', async () => {
    const customer = await service.createCustomer('Maria')
    const product = await service.createProduct('Café', 1)
    const order = await service.createOrder(customer.id)
    await service.addOrderItem({ orderId: order.id, productId: product.id, price: 30 })
    const payment = await service.addPayment({ customerId: customer.id, amount: 20 })

    await service.removePayment(payment.id)

    expect(await db.payments.count().exec()).toBe(0)
    expect((await readCustomer(customer.id)).balance).toBe(-30)
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
    await service.addOrderItem({ orderId: order.id, productId: product.id, price: 10 })
    await service.addPayment({ customerId: customer.id, amount: 5 })

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
    await service.addOrderItem({ orderId: order.id, productId: product.id, price: 10 })

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
