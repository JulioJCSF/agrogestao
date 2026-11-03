import { setupWorker } from 'msw/browser'
import { handlers } from './handlers/index.js'

/**
 * Liga o mock da API no navegador (F14). Só é importado pelo main.jsx em `npm run dev` com
 * VITE_USAR_MOCK=true; o build de produção não inclui este módulo nem o worker.
 */
export async function iniciarMock() {
  await setupWorker(...handlers).start({
    quiet: true,
    // Rota da API sem handler é erro (mostra o endpoint esquecido); o resto, como os
    // módulos do Vite, segue para a rede.
    onUnhandledFrame({ frame, defaults }) {
      const url = frame.data?.request?.url
      if (url && new URL(url).pathname.startsWith('/api/v1/')) defaults.error()
    },
  })
}
