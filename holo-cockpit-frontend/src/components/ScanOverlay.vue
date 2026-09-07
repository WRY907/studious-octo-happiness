<template>
  <div class="scan-overlay" aria-hidden="true">
    <div class="scan-band">
      <span class="scan-core"></span>
    </div>
  </div>
</template>

<script setup>
/* 全屏水平扫描线：12s 循环自上而下扫过，纯 CSS（仅 transform 动画） */
</script>

<style scoped>
.scan-overlay {
  position: fixed;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}
.scan-band {
  position: absolute;
  left: 0;
  right: 0;
  height: 260px;
  background: linear-gradient(180deg, transparent, rgba(0, 229, 255, 0.03), transparent);
  animation: scan-loop 12s linear infinite;
  will-change: transform;
}
/* 扫描带中心的细亮线 */
.scan-core {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 229, 255, 0.14), transparent);
}
@keyframes scan-loop {
  0% { transform: translateY(-280px); }
  100% { transform: translateY(100vh); }
}
</style>
