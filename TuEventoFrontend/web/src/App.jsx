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
import EventLayoutEditorDemo from './pages/EventLayoutEditorDemo';
import EventCreateWizard from './pages/EventCreateWizard';
import EventManage from './pages/EventManage';


function AppContent() {
  const location = useLocation();
  const showNavbar = !['/login', '/verification', '/layout-editor-demo', '/events/create'].includes(location.pathname);

  return (
    <div className="min-h-screen bg-background">
      {showNavbar && <Navbar />}
      <div>
        <Routes>
          <Route path="/" element={<LadingPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/nosotros" element={<AboutUs />} />
          <Route path="/events" element={<Events />} />
          <Route path="/events/create" element={<EventCreateWizard />} />
          <Route path="/events/manage" element={<EventManage />} />
          <Route path="/organizer-petition-form" element={<OrganizerPetitionForm />} />
          <Route path="/admin-panel" element={<AdminPanel />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/verification" element={<CodeVerification />} />
          <Route path="/layout-editor-demo" element={<EventLayoutEditorDemo />} />
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
