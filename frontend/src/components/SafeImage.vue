<template>
  <img
    ref="imageElement"
    :src="currentSrc"
    :srcset="currentSrcset || undefined"
    :sizes="currentSrcset ? sizes : undefined"
    :alt="alt"
    :class="imageClass"
    :loading="loading"
    :decoding="decoding"
    :fetchpriority="fetchpriority"
    :referrerpolicy="referrerpolicy"
    :aria-busy="activated && !settled"
    @load="handleLoad"
    @error="useFallback"
  />
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { FALLBACK_IMAGE, getResponsiveImageData, normalizeImageUrl } from "@/utils/image";

const TRANSPARENT_PIXEL =
  "data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs=";

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
  decoding: {
    type: String,
    default: "async",
  },
  fetchpriority: {
    type: String,
    default: "auto",
  },
  sizes: {
    type: String,
    default: "100vw",
  },
  rootMargin: {
    type: String,
    default: "500px 0px",
  },
  timeout: {
    type: Number,
    default: 10000,
  },
  referrerpolicy: {
    type: String,
    default: "origin-when-cross-origin",
  },
});

const imageElement = ref(null);
const imageData = computed(() => getResponsiveImageData(props.src, props.fallback));
const activated = ref(props.loading === "eager");
const settled = ref(false);
const triedWithoutSrcset = ref(false);
const triedOriginal = ref(false);
const currentSrc = ref(activated.value ? imageData.value.src : TRANSPARENT_PIXEL);
const currentSrcset = ref(activated.value ? imageData.value.srcset : "");
let observer;
let timeoutId;

const clearLoadTimeout = () => {
  if (timeoutId) {
    window.clearTimeout(timeoutId);
    timeoutId = undefined;
  }
};

const scheduleLoadTimeout = () => {
  clearLoadTimeout();
  if (!activated.value || props.timeout <= 0 || !/^https?:\/\//i.test(currentSrc.value)) return;
  timeoutId = window.setTimeout(() => useFallback(), props.timeout);
};

const applyImageData = () => {
  settled.value = false;
  triedWithoutSrcset.value = false;
  triedOriginal.value = false;
  currentSrc.value = imageData.value.src;
  currentSrcset.value = imageData.value.srcset;
  scheduleLoadTimeout();
};

const activate = () => {
  if (activated.value) return;
  activated.value = true;
  observer?.disconnect();
  applyImageData();
};

watch(imageData, () => {
  if (activated.value) applyImageData();
});

const useFallback = () => {
  clearLoadTimeout();
  if (!triedWithoutSrcset.value && currentSrcset.value) {
    triedWithoutSrcset.value = true;
    currentSrcset.value = "";
    scheduleLoadTimeout();
    return;
  }
  if (!triedOriginal.value && imageData.value.original !== currentSrc.value) {
    triedOriginal.value = true;
    currentSrc.value = imageData.value.original;
    currentSrcset.value = "";
    scheduleLoadTimeout();
    return;
  }
  const normalizedFallback = normalizeImageUrl(props.fallback, FALLBACK_IMAGE);
  if (currentSrc.value !== normalizedFallback) {
    currentSrc.value = normalizedFallback;
    currentSrcset.value = "";
  } else {
    settled.value = true;
  }
};

const handleLoad = () => {
  clearLoadTimeout();
  settled.value = true;
};

onMounted(() => {
  if (activated.value) {
    scheduleLoadTimeout();
    return;
  }
  if (!("IntersectionObserver" in window)) {
    activate();
    return;
  }
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) activate();
    },
    { rootMargin: props.rootMargin },
  );
  observer.observe(imageElement.value);
});

onBeforeUnmount(() => {
  observer?.disconnect();
  clearLoadTimeout();
});
</script>
