import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/global.css'
import './i18n' // Initialize i18next
import { restoreSession } from './services/httpClient'
import { applySessionLanguage } from './services/sessionLanguage'

// El access token vive solo en memoria: se recupera con la cookie antes del primer render.
// Con sesión, la interfaz arranca en el idioma preferido del perfil (sin sesión, en español).
restoreSession()
  .then(applySessionLanguage)
  .finally(() => {
    ReactDOM.createRoot(document.getElementById('root')).render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    )
  })
