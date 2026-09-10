import type {
  AdjustmentDocType,
  CustomerDocType,
  OrderItemDocType,
  PaymentDocType,
} from './types'

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

export function migrateCustomerToCents(oldDoc: LegacyCustomerDoc): CustomerDocType {
  return {
    id: oldDoc.id,
    name: oldDoc.name,
    nameNormalized: oldDoc.nameNormalized,
    balanceCents: Math.round(oldDoc.balance * 100),
  }
}

export function migrateOrderItemToCents(oldDoc: LegacyOrderItemDoc): OrderItemDocType {
  return {
    id: oldDoc.id,
    orderId: oldDoc.orderId,
    productId: oldDoc.productId,
    priceCents: Math.round(oldDoc.price * 100),
  }
}

export function migratePaymentToCents(oldDoc: LegacyPaymentDoc): PaymentDocType {
  return {
    id: oldDoc.id,
    customerId: oldDoc.customerId,
    amountCents: Math.round(oldDoc.amount * 100),
    createdAt: oldDoc.createdAt,
  }
}

export function migrateAdjustmentToCents(oldDoc: LegacyAdjustmentDoc): AdjustmentDocType {
  return {
    id: oldDoc.id,
    customerId: oldDoc.customerId,
    amountCents: Math.round(oldDoc.amount * 100),
    createdAt: oldDoc.createdAt,
  }
}
