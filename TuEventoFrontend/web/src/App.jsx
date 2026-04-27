import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom'
import Login from './pages/login'
import LadingPage from './pages/ladingPage'
import AboutUs from './pages/AboutUs'
import Events from './pages/Events'
import Navbar from './layouts/Navbar'
import OrganizerPetitionForm from './pages/OrganizerPetitionForm'
import AdminDashboard from './pages/AdminDashboard'
import ProfilePage from './pages/ProfilePage';
import CodeVerification from './pages/CodeVerification';


function AppContent() {
  const location = useLocation();
  const showNavbar = !['/login', '/profile', '/verification'].includes(location.pathname);

  return (
    <>
      {showNavbar && <Navbar />}
      <Routes>
        <Route path="/" element={<LadingPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/nosotros" element={<AboutUs />} />
        <Route path="/events" element={<Events />} />
        <Route path="/organizer-petition" element={<OrganizerPetitionForm />} />
        <Route path="/admin-dashboard" element={<AdminDashboard />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/verification" element={<CodeVerification />} />
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
