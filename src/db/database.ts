import { addRxPlugin, createRxDatabase } from 'rxdb'
import { RxDBDevModePlugin } from 'rxdb/plugins/dev-mode'
import { RxDBMigrationSchemaPlugin } from 'rxdb/plugins/migration-schema'
import { getRxStorageDexie } from 'rxdb/plugins/storage-dexie'
import { wrappedValidateAjvStorage } from 'rxdb/plugins/validate-ajv'
import {
  migrateAdjustmentToCents,
  migrateCustomerToCents,
  migrateOrderItemToCents,
  migratePaymentToCents,
} from './migrations'
import {
  adjustmentSchema,
  customerSchema,
  orderItemSchema,
  orderSchema,
  paymentSchema,
  productSchema,
} from './schemas'
import type { SalesbookCollections, SalesbookDatabase } from './types'

export const DATABASE_NAME = 'salesbook'

let databasePromise: Promise<SalesbookDatabase> | null = null
let devModeEnabled = false
let migrationPluginEnabled = false

function enableMigrationPlugin(): void {
  if (migrationPluginEnabled) return
  addRxPlugin(RxDBMigrationSchemaPlugin)
  migrationPluginEnabled = true
}

function enableDevMode(): void {
  if (devModeEnabled) return
  addRxPlugin(RxDBDevModePlugin)
  devModeEnabled = true
}

export async function createSalesbookDatabase(
  name: string = DATABASE_NAME,
): Promise<SalesbookDatabase> {
  enableMigrationPlugin()
  if (import.meta.env.DEV) enableDevMode()

  const storage = wrappedValidateAjvStorage({ storage: getRxStorageDexie() })

  const db = await createRxDatabase<SalesbookCollections>({
    name,
    storage,
    multiInstance: true,
  })

  await db.addCollections({
    customers: {
      schema: customerSchema,
      migrationStrategies: { 1: migrateCustomerToCents },
    },
    products: { schema: productSchema },
    orders: { schema: orderSchema },
    orderitems: {
      schema: orderItemSchema,
      migrationStrategies: { 1: migrateOrderItemToCents },
    },
    payments: {
      schema: paymentSchema,
      migrationStrategies: { 1: migratePaymentToCents },
    },
    adjustments: {
      schema: adjustmentSchema,
      migrationStrategies: { 1: migrateAdjustmentToCents },
    },
  })

  return db
}

export function getDatabase(): Promise<SalesbookDatabase> {
  databasePromise ??= createSalesbookDatabase()
  return databasePromise
}

if (import.meta.hot) {
  import.meta.hot.dispose(() => {
    const pending = databasePromise
    databasePromise = null
    void pending?.then((db) => db.close())
  })
}
