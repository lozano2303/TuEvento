import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import React from 'react'
import { ThemeProvider } from './context/ThemeContext'
import { NotificationProvider } from './context/NotificationContext'
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
import EventDetail from './pages/EventDetail';
import ReactivateAccountPage from './pages/ReactivateAccountPage';
import CompleteProfile from './pages/CompleteProfile';
import AdminEventManagement from './pages/AdminEventManagement';
import AdminDashboard from './pages/AdminDashboard';
import AdminFinancial from './pages/AdminFinancial';
import AdminCategoryManagement from './pages/AdminCategoryManagement';
import AdminSectionTypeManagement from './pages/AdminSectionTypeManagement';
import AdminEventTranslations from './pages/AdminEventTranslations';
import AdminCategoryTranslations from './pages/AdminCategoryTranslations';
import AdminSectionTypeTranslations from './pages/AdminSectionTypeTranslations';
import AdminCommentTranslations from './pages/AdminCommentTranslations';
import AdminProfileTranslations from './pages/AdminProfileTranslations';
import AdminReports from './pages/AdminReports';
import AdminOrganizerManagement from './pages/AdminOrganizerManagement';
import WalletPage from './pages/WalletPage';
import Checkout from './pages/Checkout';
import PaymentPending from './pages/PaymentPending';
import PaymentConfirmation from './pages/PaymentConfirmation';
import OrganizerEarnings from './pages/OrganizerEarnings';
import TerminosDeUso from './pages/TerminosDeUso';
import Privacidad from './pages/Privacidad';
import Notifications from './pages/Notifications';
import SpanishOnlyRoute from './components/common/SpanishOnlyRoute';
import OrganizerWelcomeModal from './components/common/OrganizerWelcomeModal';
import RoleRevokedModal from './components/common/RoleRevokedModal';
import { useNotifications } from './context/NotificationContext';

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

/**
 * Blocks ADMIN users from accessing regular user/organizer routes.
 * Redirects them to /admin-panel if they try to navigate there manually.
 */
function UserOnlyRoute({ children }) {
  const token = localStorage.getItem('token');
  const role  = localStorage.getItem('role');

  if (token && role === 'ADMIN') return <Navigate to="/admin-panel" replace />;

  return children;
}

function AppContent() {
  const location = useLocation();
  const { roleRevokedPending, clearRoleRevokedPending } = useNotifications();

  // Check on mount if a role-revoked acknowledgement is pending from a previous session
  // (e.g. the user was offline when the WS fired, then logged back in)
  const [showRoleRevokedModal, setShowRoleRevokedModal] = React.useState(() => {
    const userId = localStorage.getItem('userID');
    return userId ? localStorage.getItem(`role_revoked_ack_${userId}`) === 'pending' : false;
  });

  React.useEffect(() => {
    if (roleRevokedPending) setShowRoleRevokedModal(true);
  }, [roleRevokedPending]);

  const handleCloseRoleRevokedModal = () => {
    setShowRoleRevokedModal(false);
    clearRoleRevokedPending();
  };

  // Rutas sin navbar — incluye el editor con ruta dinámica /events/:id/layout
  const showNavbar = ![
    '/login', '/verification', '/events/create',
    '/reactivate-account', '/complete-profile',
    '/admin-panel', '/admin-events', '/admin-dashboard', '/admin-financial', '/admin-reports',
    '/admin-categories', '/admin-section-types', '/admin-organizers',
    '/checkout',
  ].includes(location.pathname)
    && !location.pathname.match(/^\/events\/[^/]+\/layout$/)
    && !location.pathname.match(/^\/checkout/)
    && !location.pathname.match(/^\/admin-translations/);

  return (
    <div className="min-h-screen bg-background">
      <RoleRevokedModal open={showRoleRevokedModal} onClose={handleCloseRoleRevokedModal} />
      {showNavbar && <Navbar />}
      {/* Bienvenida tras aprobarse la solicitud de organizador (se activa vía WebSocket) */}
      <OrganizerWelcomeModal />
      <div>
        <Routes>
          <Route path="/" element={<UserOnlyRoute><LadingPage /></UserOnlyRoute>} />
          <Route path="/login" element={<Login />} />
          <Route path="/nosotros" element={<UserOnlyRoute><AboutUs /></UserOnlyRoute>} />
          <Route path="/events" element={<UserOnlyRoute><Events /></UserOnlyRoute>} />
          <Route path="/events/create" element={
            <ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}>
              <SpanishOnlyRoute>
                <EventCreateWizard />
              </SpanishOnlyRoute>
            </ProtectedRoute>
          } />
          <Route path="/events/manage" element={
            <ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}>
              <EventManage />
            </ProtectedRoute>
          } />
          <Route path="/organizer/earnings" element={
            <ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}>
              <OrganizerEarnings />
            </ProtectedRoute>
          } />
          <Route path="/events/:eventId" element={<UserOnlyRoute><EventDetail /></UserOnlyRoute>} />
          <Route path="/organizer-petition-form" element={
            <ProtectedRoute>
              <OrganizerPetitionForm />
            </ProtectedRoute>
          } />
          <Route path="/admin-panel" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminPanel />
            </ProtectedRoute>
          } />
          <Route path="/admin-events" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminEventManagement />
            </ProtectedRoute>
          } />
          <Route path="/admin-dashboard" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedRoute>
          } />
          <Route path="/admin-financial" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminFinancial />
            </ProtectedRoute>
          } />
          <Route path="/admin-categories" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminCategoryManagement />
            </ProtectedRoute>
          } />
          <Route path="/admin-section-types" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminSectionTypeManagement />
            </ProtectedRoute>
          } />
          <Route path="/admin-reports" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminReports />
            </ProtectedRoute>
          } />
          <Route path="/admin-organizers" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminOrganizerManagement />
            </ProtectedRoute>
          } />
          <Route path="/admin-translations/:lang/events" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminEventTranslations />
            </ProtectedRoute>
          } />
          <Route path="/admin-translations/:lang/categories" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminCategoryTranslations />
            </ProtectedRoute>
          } />
          <Route path="/admin-translations/:lang/section-types" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminSectionTypeTranslations />
            </ProtectedRoute>
          } />
          <Route path="/admin-translations/:lang/comments" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminCommentTranslations />
            </ProtectedRoute>
          } />
          <Route path="/admin-translations/:lang/profiles" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <AdminProfileTranslations />
            </ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute>
              <UserOnlyRoute><ProfilePage /></UserOnlyRoute>
            </ProtectedRoute>
          } />
          <Route path="/notifications" element={
            <ProtectedRoute>
              <UserOnlyRoute><Notifications /></UserOnlyRoute>
            </ProtectedRoute>
          } />
          <Route path="/verification" element={<CodeVerification />} />
          <Route path="/reactivate-account" element={<ReactivateAccountPage />} />
          <Route path="/complete-profile" element={
            <ProtectedRoute>
              <CompleteProfile />
            </ProtectedRoute>
          } />
          <Route path="/terminos-de-uso" element={<TerminosDeUso />} />
          <Route path="/privacidad" element={<Privacidad />} />
          <Route path="/events/:eventId/layout" element={
            <ProtectedRoute requiredRoles={['ORGANIZER', 'ADMIN']}>
              <SpanishOnlyRoute>
                <EventLayoutEditor />
              </SpanishOnlyRoute>
            </ProtectedRoute>
          } />
          <Route path="/wallet" element={
            <ProtectedRoute>
              <UserOnlyRoute><WalletPage /></UserOnlyRoute>
            </ProtectedRoute>
          } />
          <Route path="/checkout" element={
            <ProtectedRoute>
              <UserOnlyRoute><Checkout /></UserOnlyRoute>
            </ProtectedRoute>
          } />
          <Route path="/checkout/pending/:paymentId" element={
            <ProtectedRoute>
              <UserOnlyRoute><PaymentPending /></UserOnlyRoute>
            </ProtectedRoute>
          } />
          <Route path="/checkout/confirmed/:paymentId" element={
            <ProtectedRoute>
              <UserOnlyRoute><PaymentConfirmation /></UserOnlyRoute>
            </ProtectedRoute>
          } />
        </Routes>
      </div>
    </div>
  );
}

function App() {
  // Sincronización de logout entre pestañas
  useEffect(() => {
    const handleStorageChange = (event) => {
      // El evento storage solo se dispara en OTRAS pestañas, no en la que hizo el cambio.
      // Si el token fue removido (logout) en otra pestaña, redirigir a /login inmediatamente.
      if (event.key === 'token' && event.newValue === null) {
        // Logout detectado en otra pestaña — forzar redirección
        window.location.href = '/login';
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  return (
    <BrowserRouter>
      <ThemeProvider>
        <NotificationProvider>
          <AppContent />
        </NotificationProvider>
      </ThemeProvider>
    </BrowserRouter>
  )
}

export default App
