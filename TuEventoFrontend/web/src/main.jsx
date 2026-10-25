import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './styles/global.css'
import './i18n' // Initialize i18next
import { restoreSession } from './services/httpClient'

// El access token vive solo en memoria: se recupera con la cookie antes del primer render
restoreSession().finally(() => {
  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  )
})
