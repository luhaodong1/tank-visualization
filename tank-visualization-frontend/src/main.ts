import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import JsonViewer from 'vue-json-viewer'
import 'element-plus/dist/index.css'
import * as echarts from 'echarts'

const app = createApp(App);   //建立一个vue3app
app.use(store).use(router).use(ElementPlus, { size: 'small', zIndex: 3000 }).mount('#app')
app.config.globalProperties.$echarts = echarts
app.use(JsonViewer)