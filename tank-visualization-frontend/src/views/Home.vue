<template>
  <div>
    <TankControl />
  </div>
</template>

<script>
import TankControl from '../components/TankControl.vue';

export default {
  name: 'Home',
  components: {
    TankControl
  }
}
</script>

<style scoped>
#canvas-container {
  position: relative;
  width: 800px;
  height: 600px;
  margin: 0 auto;
  border: 1px solid #ccc;
}
</style>
