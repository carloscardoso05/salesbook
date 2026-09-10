<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppNav from './components/AppNav.vue'
import ToastHost from './components/ToastHost.vue'
</script>

<template>
  <div class="min-h-svh lg:pl-64">
    <AppNav />
    <main class="mx-auto w-full max-w-5xl px-4 pb-28 pt-4 lg:px-8 lg:pb-12 lg:pt-8">
      <RouterView />
    </main>
    <ToastHost />
  </div>
</template>
