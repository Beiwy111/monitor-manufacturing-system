import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/typography.css'
import './styles/index.css'
import './styles/home.css'
import './styles/mes.css'

const app = createApp(App)
const pinia = createPinia()

pinia.use(({ store }) => {
  if (store.$id !== 'mes') return
  store.$subscribe((_mutation, state) => {
    const { selectedId, ...data } = state
    localStorage.setItem('mes-store-data', JSON.stringify(data))
  })
})

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
