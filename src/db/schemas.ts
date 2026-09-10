import type { RxJsonSchema } from 'rxdb'
import type {
  CustomerDocType,
  OrderDocType,
  OrderItemDocType,
  PaymentDocType,
  ProductDocType,
} from './types'

const idProperty = { type: 'string', maxLength: 100 } as const

export const customerSchema: RxJsonSchema<CustomerDocType> = {
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

export const productSchema: RxJsonSchema<ProductDocType> = {
  title: 'product',
  version: 0,
  primaryKey: 'id',
  type: 'object',
  properties: {
    id: idProperty,
    name: { type: 'string', maxLength: 200 },
    nameNormalized: { type: 'string', maxLength: 200 },
    stockQuantity: { type: 'integer', minimum: 0, maximum: 1000000000 },
  },
  required: ['id', 'name', 'nameNormalized', 'stockQuantity'],
  indexes: ['nameNormalized'],
}

export const orderSchema: RxJsonSchema<OrderDocType> = {
  title: 'order',
  version: 0,
  primaryKey: 'id',
  type: 'object',
  properties: {
    id: idProperty,
    customerId: { type: 'string', maxLength: 100 },
    createdAt: { type: 'string', maxLength: 32, format: 'date-time' },
  },
  required: ['id', 'customerId', 'createdAt'],
  indexes: ['customerId', 'createdAt'],
}

export const orderItemSchema: RxJsonSchema<OrderItemDocType> = {
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

export const paymentSchema: RxJsonSchema<PaymentDocType> = {
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
