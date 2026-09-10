import { createApp } from 'vue'
import { registerSW } from 'virtual:pwa-register'
import App from './App.vue'
import './style.css'
import { databaseKey, salesbookKey } from './composables/useDatabase'
import { getDatabase } from './db/database'
import { installRipple } from './directives/ripple'
import { router } from './router'
import { createSalesbookService } from './services/salesbook'

async function bootstrap(): Promise<void> {
  const db = await getDatabase()

  const app = createApp(App)
  installRipple()
  app.provide(databaseKey, db)
  app.provide(salesbookKey, createSalesbookService(db))
  app.use(router)
  app.mount('#app')

  registerSW({ immediate: true })
}

void bootstrap()
