import type { RxJsonSchema } from 'rxdb'
import type {
  AdjustmentDocType,
  CustomerDocType,
  OrderDocType,
  OrderItemDocType,
  PaymentDocType,
  ProductDocType,
} from './types'

const idProperty = { type: 'string', maxLength: 100 } as const

export const customerSchema: RxJsonSchema<CustomerDocType> = {
  title: 'customer',
  version: 1,
  primaryKey: 'id',
  type: 'object',
  properties: {
    id: idProperty,
    name: { type: 'string', maxLength: 200 },
    nameNormalized: { type: 'string', maxLength: 200 },
    balanceCents: { type: 'integer' },
  },
  required: ['id', 'name', 'nameNormalized', 'balanceCents'],
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
  version: 1,
  primaryKey: 'id',
  type: 'object',
  properties: {
    id: idProperty,
    orderId: { type: 'string', maxLength: 100 },
    productId: { type: 'string', maxLength: 100 },
    priceCents: { type: 'integer', minimum: 0 },
  },
  required: ['id', 'orderId', 'productId', 'priceCents'],
  indexes: ['orderId', 'productId'],
}

export const paymentSchema: RxJsonSchema<PaymentDocType> = {
  title: 'payment',
  version: 1,
  primaryKey: 'id',
  type: 'object',
  properties: {
    id: idProperty,
    customerId: { type: 'string', maxLength: 100 },
    amountCents: { type: 'integer', exclusiveMinimum: 0 },
    createdAt: { type: 'string', maxLength: 32, format: 'date-time' },
  },
  required: ['id', 'customerId', 'amountCents', 'createdAt'],
  indexes: ['customerId', 'createdAt'],
}

export const adjustmentSchema: RxJsonSchema<AdjustmentDocType> = {
  title: 'adjustment',
  version: 1,
  primaryKey: 'id',
  type: 'object',
  properties: {
    id: idProperty,
    customerId: { type: 'string', maxLength: 100 },
    amountCents: { type: 'integer' },
    createdAt: { type: 'string', maxLength: 32, format: 'date-time' },
  },
  required: ['id', 'customerId', 'amountCents', 'createdAt'],
  indexes: ['customerId', 'createdAt'],
}
