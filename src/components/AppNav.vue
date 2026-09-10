<script setup lang="ts">
import { RouterLink, useRoute } from 'vue-router'
import AppIcon from './AppIcon.vue'
import type { IconName } from './icons'

type NavItem = {
  to: string
  label: string
  icon: IconName
}

const navItems: NavItem[] = [
  { to: '/', label: 'Início', icon: 'home' },
  { to: '/customers', label: 'Clientes', icon: 'users' },
  { to: '/products', label: 'Produtos', icon: 'cube' },
  { to: '/orders', label: 'Pedidos', icon: 'receipt' },
  { to: '/payments', label: 'Pagamentos', icon: 'banknotes' },
]

const route = useRoute()

function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(`${path}/`)
}
</script>

<template>
  <aside
    class="fixed inset-y-0 left-0 z-40 hidden w-64 flex-col border-r border-slate-200 bg-white lg:flex"
  >
    <div class="flex items-center gap-3 px-6 py-6">
      <img src="/logo.svg" alt="" class="h-10 w-10 rounded-xl" />
      <div>
        <p class="text-base font-bold text-slate-900">Salesbook</p>
        <p class="text-xs text-slate-500">Gestão offline</p>
      </div>
    </div>
    <nav class="flex flex-1 flex-col gap-1 px-3">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="tap-row flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium"
        :class="
          isActive(item.to)
            ? 'bg-indigo-50 text-indigo-700 active:bg-indigo-100'
            : 'text-slate-600 hover:bg-slate-100 active:bg-slate-200'
        "
      >
        <AppIcon :name="item.icon" class="h-5 w-5" />
        {{ item.label }}
      </RouterLink>
    </nav>
    <p class="px-6 py-4 text-xs text-slate-400">Dados salvos no seu dispositivo</p>
  </aside>

  <header class="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur lg:hidden">
    <div class="flex items-center gap-3 px-4 py-3">
      <img src="/logo.svg" alt="" class="h-9 w-9 rounded-lg" />
      <p class="text-base font-bold text-slate-900">Salesbook</p>
    </div>
  </header>

  <nav
    class="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur lg:hidden"
  >
    <div class="grid grid-cols-5">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="tap-row flex flex-col items-center gap-1 py-2.5 text-[11px] font-medium"
        :class="
          isActive(item.to)
            ? 'text-indigo-600 active:bg-indigo-50'
            : 'text-slate-500 active:bg-slate-100'
        "
      >
        <AppIcon :name="item.icon" class="h-5 w-5" />
        {{ item.label }}
      </RouterLink>
    </div>
  </nav>
</template>
