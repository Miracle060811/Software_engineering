<template>
  <span class="count-up">{{ displayValue }}</span>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'

const props = defineProps({
  target: { type: Number, required: true },
  duration: { type: Number, default: 1500 },
  isDecimal: { type: Boolean, default: false }
})

const displayValue = ref('0')

const animate = () => {
  const start = 0
  const end = props.target
  const duration = props.duration
  const startTime = performance.now()

  const step = (currentTime) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)

    // easeOutCubic
    const eased = 1 - Math.pow(1 - progress, 3)
    const current = start + (end - start) * eased

    if (props.isDecimal) {
      displayValue.value = current.toFixed(1)
    } else {
      displayValue.value = Math.floor(current).toLocaleString()
    }

    if (progress < 1) {
      requestAnimationFrame(step)
    } else {
      if (props.isDecimal) {
        displayValue.value = end.toFixed(1)
      } else {
        displayValue.value = end.toLocaleString()
      }
    }
  }

  requestAnimationFrame(step)
}

onMounted(() => {
  animate()
})

watch(() => props.target, () => {
  animate()
})
</script>

<style scoped>
.count-up {
  font-variant-numeric: tabular-nums;
}
</style>
