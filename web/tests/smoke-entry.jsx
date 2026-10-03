import React from 'react'
import { MemoryRouter } from 'react-router'
import { App } from '../src/App.jsx'
import { AuthProvider } from '../src/contexts/AuthContext.jsx'
import { DataProvider } from '../src/contexts/DataContext.jsx'

export function smokeTree(path) {
  return (
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <DataProvider>
          <App />
        </DataProvider>
      </AuthProvider>
    </MemoryRouter>
  )
}
