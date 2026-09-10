import { inject, type InjectionKey } from 'vue'
import type { SalesbookDatabase } from '../db/types'
import type { SalesbookService } from '../services/salesbook'

export const databaseKey: InjectionKey<SalesbookDatabase> = Symbol('salesbook-database')
export const salesbookKey: InjectionKey<SalesbookService> = Symbol('salesbook-service')

export function useDatabase(): SalesbookDatabase {
  const db = inject(databaseKey)
  if (!db) throw new Error('Banco de dados não foi fornecido pelo aplicativo.')
  return db
}

export function useSalesbook(): SalesbookService {
  const service = inject(salesbookKey)
  if (!service) throw new Error('Serviço de vendas não foi fornecido pelo aplicativo.')
  return service
}
