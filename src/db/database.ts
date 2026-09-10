import { addRxPlugin, createRxDatabase } from 'rxdb'
import { RxDBDevModePlugin } from 'rxdb/plugins/dev-mode'
import { getRxStorageDexie } from 'rxdb/plugins/storage-dexie'
import { wrappedValidateAjvStorage } from 'rxdb/plugins/validate-ajv'
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

function enableDevMode(): void {
  if (devModeEnabled) return
  addRxPlugin(RxDBDevModePlugin)
  devModeEnabled = true
}

export async function createSalesbookDatabase(
  name: string = DATABASE_NAME,
): Promise<SalesbookDatabase> {
  if (import.meta.env.DEV) enableDevMode()

  const storage = wrappedValidateAjvStorage({ storage: getRxStorageDexie() })

  const db = await createRxDatabase<SalesbookCollections>({
    name,
    storage,
    multiInstance: true,
  })

  await db.addCollections({
    customers: { schema: customerSchema },
    products: { schema: productSchema },
    orders: { schema: orderSchema },
    orderitems: { schema: orderItemSchema },
    payments: { schema: paymentSchema },
    adjustments: { schema: adjustmentSchema },
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
