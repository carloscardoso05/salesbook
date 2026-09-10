import type { RxCollection, RxDatabase } from 'rxdb'

export type CustomerDocType = {
  id: string
  name: string
  nameNormalized: string
  balance: number
}

export type ProductDocType = {
  id: string
  name: string
  nameNormalized: string
  stockQuantity: number
}

export type OrderDocType = {
  id: string
  customerId: string
  createdAt: string
}

export type OrderItemDocType = {
  id: string
  orderId: string
  productId: string
  price: number
}

export type PaymentDocType = {
  id: string
  customerId: string
  amount: number
  createdAt: string
}

export type AdjustmentDocType = {
  id: string
  customerId: string
  amount: number
  createdAt: string
}

export type SalesbookCollections = {
  customers: RxCollection<CustomerDocType>
  products: RxCollection<ProductDocType>
  orders: RxCollection<OrderDocType>
  orderitems: RxCollection<OrderItemDocType>
  payments: RxCollection<PaymentDocType>
  adjustments: RxCollection<AdjustmentDocType>
}

export type SalesbookDatabase = RxDatabase<SalesbookCollections>
