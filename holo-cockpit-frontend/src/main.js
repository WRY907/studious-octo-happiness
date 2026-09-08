import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/global.css'
import './styles/pages-motion.css' // 全页面动效增强层

const app = createApp(App)
app.use(router)
app.mount('#app')
