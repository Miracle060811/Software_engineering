<template>
  <img
    :src="currentSrc"
    :alt="alt"
    :class="imageClass"
    :loading="loading"
    :referrerpolicy="referrerpolicy"
    @error="useFallback"
  />
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { FALLBACK_IMAGE, normalizeImageUrl } from "@/utils/image";

const props = defineProps({
  src: {
    type: String,
    default: "",
  },
  alt: {
    type: String,
    default: "",
  },
  fallback: {
    type: String,
    default: FALLBACK_IMAGE,
  },
  imageClass: {
    type: [String, Array, Object],
    default: "",
  },
  loading: {
    type: String,
    default: "lazy",
  },
  referrerpolicy: {
    type: String,
    default: "origin-when-cross-origin",
  },
});

const normalizedSrc = computed(() => normalizeImageUrl(props.src, props.fallback));
const currentSrc = ref(normalizedSrc.value);

watch(normalizedSrc, (value) => {
  currentSrc.value = value;
});

const useFallback = () => {
  if (currentSrc.value !== props.fallback) {
    currentSrc.value = props.fallback;
  }
};
</script>
