<template>
  <el-dialog v-model="visible" title="价格趋势（近7天）" width="600px" @close="handleClose">
    <div ref="chartRef" style="width: 100%; height: 360px"></div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, nextTick } from "vue";
import * as echarts from "echarts";
import request from "@/utils/request";

const props = defineProps({
  modelValue: Boolean,
  ticketId: Number,
  ticketType: Number, // 0=flight, 1=train
  ticketName: String,
});

const emit = defineEmits(["update:modelValue"]);

const visible = ref(false);
const chartRef = ref(null);
let chart = null;

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val;
    if (val) {
      nextTick(() => fetchAndRender());
    }
  },
  { immediate: true }
);

const handleClose = () => {
  emit("update:modelValue", false);
  if (chart) {
    chart.dispose();
    chart = null;
  }
};

const fetchAndRender = async () => {
  try {
    const data = await request.get("/api/price/trend", {
      params: { type: props.ticketType, id: props.ticketId },
    });
    if (!Array.isArray(data) || data.length === 0) return;
    renderChart(data);
  } catch (e) {
    // silently fail
  }
};

const renderChart = (data) => {
  if (!chartRef.value) return;
  if (chart) chart.dispose();
  chart = echarts.init(chartRef.value);

  const dates = data.map((d) => d.recordDate);
  const prices = data.map((d) => d.lowestPrice);

  chart.setOption({
    tooltip: {
      trigger: "axis",
      formatter: (params) =>
        `${params[0].axisValue}<br/>最低价：¥${params[0].value}`,
    },
    xAxis: {
      type: "category",
      data: dates,
      axisLabel: { rotate: 30 },
    },
    yAxis: {
      type: "value",
      name: "价格 (¥)",
      min: (val) => Math.floor(val.min * 0.9),
    },
    series: [
      {
        name: props.ticketName || "价格",
        type: "line",
        data: prices,
        smooth: true,
        lineStyle: { color: "#0D9488", width: 3 },
        itemStyle: { color: "#0D9488" },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(13,148,136,0.25)" },
            { offset: 1, color: "rgba(13,148,136,0.02)" },
          ]),
        },
        markLine: {
          silent: true,
          data: [{ type: "average", name: "均价" }],
          lineStyle: { color: "#F59E0B", type: "dashed" },
        },
      },
    ],
    grid: { left: 50, right: 20, top: 30, bottom: 50 },
  });
};
</script>
