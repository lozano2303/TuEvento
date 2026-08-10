import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
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
import EventLayoutEditor from './pages/EventLayoutEditor';
import EventCreateWizard from './pages/EventCreateWizard';
import EventManage from './pages/EventManage';

/**
 * Redirects to /login if no token, or to /events if the user's role
 * doesn't match one of the required roles.
 * Reads role from localStorage (written at login time, source of truth until next login).
 */
function ProtectedRoute({ children, requiredRoles }) {
  const token = localStorage.getItem('token');
  const role  = localStorage.getItem('role');

  if (!token) return <Navigate to="/login" replace />;

  if (requiredRoles && !requiredRoles.includes(role)) {
    return <Navigate to="/events" replace />;
  }

  return children;
}

function AppContent() {
  const location = useLocation();
  // Rutas sin navbar — incluye el editor con ruta dinámica /events/:id/layout
  const showNavbar = !['/login', '/verification', '/events/create'].includes(location.pathname)
    && !location.pathname.match(/^\/events\/[^/]+\/layout$/);

  return (
    <div className="min-h-screen bg-background">
      {showNavbar && <Navbar />}
      <div>
        <Routes>
          <Route path="/" element={<LadingPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/nosotros" element={<AboutUs />} />
          <Route path="/events" element={<Events />} />
          <Route path="/events/create" element={
            <ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}>
              <EventCreateWizard />
            </ProtectedRoute>
          } />
          <Route path="/events/manage" element={
            <ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}>
              <EventManage />
            </ProtectedRoute>
          } />
          <Route path="/organizer-petition-form" element={<OrganizerPetitionForm />} />
          <Route path="/admin-panel" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminPanel />
            </ProtectedRoute>
          } />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/verification" element={<CodeVerification />} />
          <Route path="/events/:eventId/layout" element={<EventLayoutEditor />} />
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
