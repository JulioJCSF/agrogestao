import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import './styles.css'
import { App } from './App.jsx'
import { AuthProvider } from './contexts/AuthContext.jsx'
import { DataProvider } from './contexts/DataContext.jsx'

// F14: o import dinâmico atrás de import.meta.env.DEV tira o MSW do build de produção.
async function iniciar() {
  if (import.meta.env.DEV && import.meta.env.VITE_USAR_MOCK === 'true') {
    const { iniciarMock } = await import('./mocks/navegador.js')
    await iniciarMock()
  }
  createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <BrowserRouter>
        <AuthProvider>
          <DataProvider>
            <App />
          </DataProvider>
        </AuthProvider>
      </BrowserRouter>
    </React.StrictMode>,
  )
}

iniciar()
