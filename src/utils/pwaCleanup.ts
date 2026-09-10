const LEGACY_SCOPE = '/salesbook-local/'

export async function cleanupLegacyPwa(): Promise<void> {
  if (!('serviceWorker' in navigator)) return

  const registrations = await navigator.serviceWorker
    .getRegistrations()
    .catch(() => [] as ServiceWorkerRegistration[])
  await Promise.all(
    registrations
      .filter((registration) => registration.scope.includes(LEGACY_SCOPE))
      .map((registration) => registration.unregister().catch(() => false)),
  )

  if (!('caches' in window)) return
  const cacheKeys = await caches.keys().catch(() => [] as string[])
  await Promise.all(
    cacheKeys.filter((key) => key.includes(LEGACY_SCOPE)).map((key) => caches.delete(key)),
  )
}
