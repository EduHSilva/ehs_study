import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// O build de produção é servido pelo nginx dos microservices em /app/.
// Em dev/E2E continua na raiz ("/") para não quebrar os testes existentes.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'VITE_')

  return {
    base: env.VITE_BASE_PATH || '/',
    plugins: [vue()],
  }
})
