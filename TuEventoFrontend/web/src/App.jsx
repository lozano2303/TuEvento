import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom'
import { ThemeProvider } from './context/ThemeContext'
import Login from './pages/login'
import LadingPage from './pages/ladingPage'
import AboutUs from './pages/AboutUs'
import Events from './pages/Events'
import Navbar from './layouts/Navbar'
import OrganizerPetitionForm from './pages/OrganizerPetitionForm'
import AdminPanel from './pages/AdminPanel'
import ProfilePage from './pages/ProfilePage';
import CodeVerification from './pages/CodeVerification';


function AppContent() {
  const location = useLocation();
  const showNavbar = !['/login', '/verification'].includes(location.pathname);
  const showPadding = showNavbar && location.pathname !== '/profile';

  return (
    <div className="min-h-screen bg-background">
      {showNavbar && <Navbar />}
      <div className={showPadding ? "pt-16" : ""}>
        <Routes>
          <Route path="/" element={<LadingPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/nosotros" element={<AboutUs />} />
          <Route path="/events" element={<Events />} />
          <Route path="/organizer-petition-form" element={<OrganizerPetitionForm />} />
          <Route path="/admin-panel" element={<AdminPanel />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/verification" element={<CodeVerification />} />
        </Routes>
      </div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AppContent />
      </ThemeProvider>
    </BrowserRouter>
  )
}

export default App
