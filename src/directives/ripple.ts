function createRipple(event: PointerEvent, el: HTMLElement): void {
  if (getComputedStyle(el).position === 'static') {
    el.style.position = 'relative'
  }
  el.classList.add('ripple-host')

  const rect = el.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const wave = document.createElement('span')
  wave.className = 'ripple-wave'
  wave.style.width = `${size}px`
  wave.style.height = `${size}px`
  wave.style.left = `${event.clientX - rect.left - size / 2}px`
  wave.style.top = `${event.clientY - rect.top - size / 2}px`
  wave.addEventListener('animationend', () => wave.remove())
  el.appendChild(wave)
}

const RIPPLE_SELECTOR = '.btn, .tap-row'

export function installRipple(): void {
  document.addEventListener(
    'pointerdown',
    (event) => {
      const origin = event.target
      if (!(origin instanceof Element)) return
      const target = origin.closest<HTMLElement>(RIPPLE_SELECTOR)
      if (!target) return
      if (target.hasAttribute('disabled') || target.getAttribute('aria-disabled') === 'true') return
      createRipple(event, target)
    },
    { passive: true },
  )
}
