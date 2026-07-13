import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/typography.css'
import './styles/theme.css'
import { registerMesChartTheme } from './styles/chartTheme'
import './styles/index.css'
import './styles/home.css'
import './styles/mes.css'
import './styles/ruoyi.css'
import { MES_LIVE_MODE } from '@/config/mes'

const app = createApp(App)
const pinia = createPinia()

if (!MES_LIVE_MODE) {
  pinia.use(({ store }) => {
    if (store.$id !== 'mes') return
    store.$subscribe((_mutation, state) => {
      const { selectedId, ...data } = state
      localStorage.setItem('mes-store-data', JSON.stringify(data))
    })
  })
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)
registerMesChartTheme()
app.mount('#app')
