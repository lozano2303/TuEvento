import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom'
import { ThemeProvider } from './context/ThemeContext'
import Login from './pages/login'
import LadingPage from './pages/ladingPage'
import AboutUs from './pages/AboutUs'
import Events from './pages/Events'
import Navbar from './layouts/Navbar'
import OrganizerPetitionForm from './pages/OrganizerPetitionForm'
import AdminDashboard from './pages/AdminDashboard'
import ProfilePage from './pages/ProfilePage';


function AppContent() {
  const location = useLocation();
  const showNavbar = !['/login'].includes(location.pathname);

  return (
    <div className="min-h-screen bg-background">
      {showNavbar && <Navbar />}
      <div className={showNavbar ? "pt-16" : ""}>
        <Routes>
          <Route path="/" element={<LadingPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/nosotros" element={<AboutUs />} />
          <Route path="/events" element={<Events />} />
          <Route path="/organizer-petition" element={<OrganizerPetitionForm />} />
          <Route path="/admin-dashboard" element={<AdminDashboard />} />
          <Route path="/profile" element={<ProfilePage />} />
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
