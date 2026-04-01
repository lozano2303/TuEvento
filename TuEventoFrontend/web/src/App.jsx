import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom'
import Login from './pages/login'
import LadingPage from './pages/ladingPage'
import Navbar from './layouts/Navbar'

function AppContent() {
  const location = useLocation();
  const showNavbar = location.pathname !== '/login';

  return (
    <>
      {showNavbar && <Navbar />}
      <Routes>
        <Route path="/" element={<LadingPage />} />
        <Route path="/login" element={<Login />} />
      </Routes>
    </>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  )
}

export default App
