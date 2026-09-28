import React, { useState, useEffect } from "react";
import { Eye, EyeOff, Mail, User, CheckCircle, ArrowRight, PartyPopper, Sparkles, FileText } from "lucide-react";
import loginHero from "../assets/images/tu-evento-login-hero-left.png";
import { loginUser, registerUser, resendActivationCode, googleLogin } from "../services/Login.js";
import { getProfileByUserId } from "../services/ProfileService.js";
import { useTheme } from "../context/ThemeContext";
import { performLogout } from "../services/httpClient.js";
import CodeVerification from "./CodeVerification.jsx";
import ForgotPassword from "./ForgotPassword.jsx";
import BaseModal from "../components/common/BaseModal.jsx";
import ReactivationModal from "../components/common/ReactivationModal.jsx";
import { GoogleOAuthProvider, GoogleLogin } from "@react-oauth/google";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

/**
 * GoogleButton — botón visualmente idéntico al de Facebook.
 *
 * Técnica overlay: el botón visual (con logo + "Google") está debajo.
 * Encima, con opacity-0 y posicionado absolute, vive el GoogleLogin real
 * de @react-oauth/google — ese es el que dispara el flujo OAuth auténtico.
 * El div wrapper tiene cursor-pointer; el overlay captura el clic y lo pasa
 * al GoogleLogin. pointer-events-none en la capa visual evita conflictos.
 *
 * El width del GoogleLogin se fija en 120px para que su botón interno ocupe
 * exactamente el contenedor del overlay sin desbordarse.
 */
/**
 * GoogleButton rediseñado para el nuevo layout glassmorphism.
 *
 * Usa la clase login-google-wrapper como contenedor (w-full en el grid de 2 col).
 * El overlay tiene position:absolute inset-0 con display:flex + stretch, de modo
 * que el iframe interno de GoogleLogin ocupa el 100% del wrapper y cualquier clic
 * dentro del botón visual dispara el flujo OAuth real.
 */
function GoogleButton({ onSuccess, onError }) {
  return (
    <div className="login-google-wrapper" role="button" tabIndex={0} aria-label="Iniciar sesión con Google">
      {/* ── Capa visual ── */}
      <div className="login-google-visual">
        <svg width="18" height="18" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
          <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
          <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
          <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
          <path fill="none" d="M0 0h48v48H0z"/>
        </svg>
        <span style={{ fontSize: '0.8125rem', fontWeight: 500 }}>Google</span>
      </div>

      {/* ── Overlay invisible full-width — dispara el flujo OAuth real ── */}
      <div className="login-google-overlay" aria-hidden="true">
        <GoogleLogin
          onSuccess={onSuccess}
          onError={onError}
          useOneTap={false}
          width="600"
        />
      </div>
    </div>
  );
}

export default function Login() {
  const { refreshPalette } = useTheme();
  const [showTermsModal, setShowTermsModal] = useState(false);
  const [view, setView] = useState('login');
  const [userID, setUserID] = useState(null);
  const [userData, setUserData] = useState(null);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [showSuccessNotification, setShowSuccessNotification] = useState(false);
  const [showLoginSuccessNotification, setShowLoginSuccessNotification] = useState(false);
  const [showActivateAccount, setShowActivateAccount] = useState(false);
  const [showReactivationModal, setShowReactivationModal] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);
  const [formData, setFormData] = useState({
    email: "", password: "", confirmPassword: "", name: "",
  });

  // ─── OAuth2 / sesión activa ───────────────────────────────────────────────
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    const userID = urlParams.get('userID');
    const role = urlParams.get('role');
    const oauth = urlParams.get('oauth');

    if (oauth === 'true' && token && userID && role) {
      // Validate the CSRF state param produced by handleFacebookLogin.
      // Google GSI does not come through this redirect path, so the check only
      // fires when a stored state exists in sessionStorage.
      const storedState = sessionStorage.getItem('oauth_state');
      const returnedState = urlParams.get('state');
      if (storedState && returnedState !== storedState) {
        console.error('OAuth state mismatch — possible CSRF attack, aborting login');
        sessionStorage.removeItem('oauth_state');
        return;
      }
      sessionStorage.removeItem('oauth_state');

      // Store all fields the backend includes in the redirect — mirrors what
      // handleGoogleSuccess does so both OAuth paths leave localStorage in the
      // same shape.
      localStorage.setItem('token',        token);
      localStorage.setItem('userID',       userID);
      localStorage.setItem('role',         role);

      const alias        = urlParams.get('alias')        || '';
      const refreshToken = urlParams.get('refreshToken') || '';
      const profileId    = urlParams.get('profileId');
      const needsOnboarding = urlParams.get('needsOnboarding') === 'true';

      if (alias)        localStorage.setItem('alias',        alias);
      if (refreshToken) localStorage.setItem('refreshToken', refreshToken);

      // Remove any stale profileId before conditionally setting the new one
      localStorage.removeItem('profileId');
      if (profileId)    localStorage.setItem('profileId',    profileId);

      // Redirect to onboarding when the provider did not return a valid name
      // (same behaviour as handleGoogleSuccess for the GSI flow).
      if (needsOnboarding) {
        window.history.replaceState({}, document.title, window.location.pathname);
        window.location.href = '/complete-profile';
        return;
      }

      (async () => {
        try {
          const profileResult = await getProfileByUserId(userID);
          if (profileResult.success && profileResult.data?.fullName) {
            localStorage.setItem('name', profileResult.data.fullName);
          } else {
            localStorage.setItem('name', alias);
          }
        } catch {
          localStorage.setItem('name', alias);
        }
      })();

      window.history.replaceState({}, document.title, window.location.pathname);
      window.location.href = '/';
      return;
    }

    const storedToken = localStorage.getItem('token');
    const storedUserID = localStorage.getItem('userID');
    const storedAlias = localStorage.getItem('alias');
    const storedEmail = localStorage.getItem('userEmail');
    const storedFullName = localStorage.getItem('name');
    if (storedToken && storedUserID) {
      setUserData({ userId: storedUserID, alias: storedAlias, email: storedEmail, fullName: storedFullName });
      setView('profile');
    }
  }, []);

  const clearAuth = async () => {
    await performLogout();
  };

  // ─── Calcular fortaleza de contraseña ────────────────────────────────────
  const calcStrength = (pw) => {
    let score = 0;
    if (pw.length >= 8)            score++;
    if (/[A-Z]/.test(pw))          score++;
    if (/\d/.test(pw))             score++;
    if (/[@$!%*?&]/.test(pw))      score++;
    return score;
  };

  const strengthLabel = ['', 'Muy débil', 'Débil', 'Buena', 'Fuerte'];
  const strengthColor = ['', 'strength-text-1', 'strength-text-2', 'strength-text-3', 'strength-text-4'];
  const barColors = [
    '',
    'strength-bar-1',
    'strength-bar-2',
    'strength-bar-3',
    'strength-bar-4',
  ];

  // ─── Handlers ────────────────────────────────────────────────────────────
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) setFieldErrors(prev => ({ ...prev, [name]: "" }));
    // Actualizar fortaleza solo cuando cambia el campo password
    if (name === 'password') setPasswordStrength(calcStrength(value));
  };

  // ─── Validaciones ────────────────────────────────────────────────────────
  const validateEmail = (email) => {
    if (!email?.trim()) return "El correo electrónico es obligatorio";
    if (email.trim().length > 255) return "Máximo 255 caracteres";
    if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email.trim()))
      return "Formato de correo no válido";
    if (!email.trim().toLowerCase().endsWith('@gmail.com'))
      return "Solo correos @gmail.com son aceptados";
    return "";
  };

  const validatePassword = (pw) => {
    if (!pw?.trim()) return "La contraseña es obligatoria";
    if (pw.length < 8) return "Mínimo 8 caracteres";
    if (pw.length > 100) return "Máximo 100 caracteres";
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/.test(pw))
      return "Debe contener minúscula, mayúscula, número y carácter especial (@$!%*?&)";
    return "";
  };

  const validateName = (name) => {
    if (!name?.trim()) return "El nombre completo es obligatorio";
    if (name.trim().length > 100) return "Máximo 100 caracteres";
    if (!/^[a-zA-ZÀ-ÿ\s'-]+$/.test(name.trim()))
      return "Solo letras, espacios y acentos";
    
    const words = name.trim().split(/\s+/);
    if (words.length < 2) return "Ingresa nombre y apellido";
    
    // Validar que cada palabra tenga al menos 3 caracteres
    const invalidWord = words.find(word => word.length < 3);
    if (invalidWord) return "Cada nombre y apellido debe tener al menos 3 caracteres";
    
    return "";
  };

  const validateConfirmPassword = (cp, pw) => {
    if (!cp?.trim()) return "Confirmar contraseña es obligatorio";
    if (cp !== pw) return "Las contraseñas no coinciden";
    return "";
  };

  const handleVerificationSuccess = () => setView('login');
  const handleContinueToVerification = async () => {
    setShowSuccessNotification(false);
    try {
      await resendActivationCode(formData.email);
    } catch (err) {
      // Si falla el reenvío, igual se permite ir a verificación
      console.error('No se pudo reenviar el código de activación:', err);
    }
    setUserID(formData.userID);
    setView('verification');
  };
  const handleContinueToHome = () => { setShowLoginSuccessNotification(false); window.location.href = '/'; };
  const handleLogout = async () => { 
    await clearAuth(); 
    setUserData(null); 
    setView('login'); 
  };

  const handleResendActivation = async () => {
    if (!formData.email || !formData.email.trim()) {
      setError("Por favor, ingresa tu correo electrónico para reenviar el código de activación.");
      return;
    }
    try {
      await resendActivationCode(formData.email);
      setView('verification'); // redirigir directo a CodeVerification con código ya reenviado
    } catch (err) {
      const errorMsg = err.message || "Error de conexión al reenviar código de activación";
      // Traducir mensajes del backend
      if (errorMsg === "EMAIL_NOT_FOUND") {
        setError("Este correo no está registrado en el sistema");
      } else if (errorMsg === "This email is already activated. Please login with your credentials.") {
        setError("Este correo ya está activado. Inicia sesión con tus credenciales.");
      } else {
        setError(errorMsg);
      }
    }
  };

  const handleGoogleSuccess = async ({ credential }) => {
    if (!credential) return;
    setError('');
    setLoading(true);
    try {
      // Decode the id_token payload (base64url, no signature needed here —
      // server-side verification already happened in the backend).
      const [, payloadB64] = credential.split('.');
      const payload = JSON.parse(atob(payloadB64.replace(/-/g, '+').replace(/_/g, '/')));
      const googleEmail = payload.email || '';

      const result = await googleLogin(credential);
      localStorage.setItem('token',        result.data.token);
      localStorage.setItem('refreshToken', result.data.refreshToken);
      localStorage.setItem('userID',       result.data.userID);
      localStorage.setItem('alias',        result.data.alias);
      localStorage.setItem('role',         result.data.role);
      localStorage.setItem('userEmail',    googleEmail);
      // profileId is returned when a profile row already exists but has an
      // invalid name (existing user onboarding).  Store it so CompleteProfile
      // knows to call PUT instead of POST.  Remove any stale value first.
      localStorage.removeItem('profileId');
      if (result.data.profileId != null) {
        localStorage.setItem('profileId', result.data.profileId);
      }

      // needsOnboarding is true when the backend could not create a profile
      // because Google did not return a valid display name, or when the stored
      // name fails validation (pre-fix existing users like "crislozanoshark2006").
      // Redirect immediately — do NOT fetch the profile or overwrite localStorage.name
      // with the invalid name before the user has a chance to correct it.
      if (result.data.needsOnboarding) {
        window.location.href = '/complete-profile';
        return;
      }

      // Only fetch the profile and store the name when onboarding is NOT needed.
      const [, profileResult] = await Promise.allSettled([
        refreshPalette(),
        getProfileByUserId(result.data.userID),
      ]);

      if (profileResult.status === 'fulfilled' && profileResult.value?.data?.fullName) {
        localStorage.setItem('name', profileResult.value.data.fullName);
      } else {
        localStorage.setItem('name', result.data.alias);
      }

      setUserData({ userId: result.data.userID, alias: result.data.alias, email: googleEmail });
      setShowLoginSuccessNotification(true);
      setTimeout(() => { window.location.href = '/'; }, 1500);
    } catch (err) {
      setError(err.message || 'No se pudo iniciar sesión con Google. Intenta de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleError = () => {
    // User dismissed the popup or Google returned an error — no action needed
    // unless the user explicitly triggered an error (not a cancel).
  };

  // ─── Facebook OAuth redirect flow ────────────────────────────────────────
  // Facebook does not support reliable popup flows the way Google GSI does,
  // so we use a server-side redirect: the backend redirects to Facebook, Facebook
  // redirects back to the backend callback, and the backend then redirects to
  // /login?token=...&userID=...&role=...&oauth=true.
  // The useEffect at the top of this component already handles that callback URL
  // (same shape used by the original redirect-based OAuth flow).
  const handleFacebookLogin = () => {
    // Generate a random CSRF state token and persist it in sessionStorage.
    // The backend encodes it inside the composite state that travels through
    // Facebook and returns it in the final redirect, so we can verify it here.
    const state = crypto.randomUUID();
    sessionStorage.setItem('oauth_state', state);

    // Pass the current page as the frontend_redirect_uri so the backend knows
    // where to send the browser after a successful OAuth callback.
    // Using window.location.origin + '/login' makes this work regardless of
    // whether the app is running on localhost:5173 or a production domain,
    // as long as the URL is on the server-side whitelist.
    const frontendRedirectUri = `${window.location.origin}/login`;

    const params = new URLSearchParams({
      state,
      frontend_redirect_uri: frontendRedirectUri,
    });
    window.location.href = `${API_URL}/auth/oauth/facebook?${params.toString()}`;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(""); setFieldErrors({}); setLoading(true);

    const errors = {};
    const emailErr = validateEmail(formData.email); if (emailErr) errors.email = emailErr;
    const pwErr = validatePassword(formData.password); if (pwErr) errors.password = pwErr;
    if (view !== 'login') {
      const nameErr = validateName(formData.name);
      if (nameErr) errors.name = nameErr;
      const cpErr = validateConfirmPassword(formData.confirmPassword, formData.password);
      if (cpErr) errors.confirmPassword = cpErr;
    }
    if (Object.keys(errors).length) { setFieldErrors(errors); setLoading(false); return; }

    try {
      if (view === 'login') {
        const result = await loginUser(formData.email, formData.password);
        if (result.success) {
          localStorage.setItem('token', result.data.token);
          localStorage.setItem('refreshToken', result.data.refreshToken ?? '');
          localStorage.setItem('userID', result.data.userID);
          localStorage.setItem('alias', result.data.alias);
          localStorage.setItem('userEmail', formData.email);
          localStorage.setItem('role', result.data.role || 'USER');

          // Carga la paleta del usuario recién autenticado y la del perfil en paralelo.
          // refreshPalette() se AGUARDA para que las CSS vars estén aplicadas ANTES
          // de mostrar el modal de bienvenida — evita la race condition donde el
          // modal se renderizaba con la paleta anterior (tema rosa/blanco incorrecto).
          const [, profileResult] = await Promise.allSettled([
            refreshPalette(),
            getProfileByUserId(result.data.userID),
          ]);

          // Obtener fullName del perfil (como en el móvil)
          let fullName = result.data.alias; // fallback al alias
          if (profileResult.status === 'fulfilled' && profileResult.value?.success && profileResult.value?.data?.fullName) {
            fullName = profileResult.value.data.fullName;
            localStorage.setItem('name', fullName);
          } else {
            if (profileResult.status === 'rejected') {
              console.error('Error al obtener perfil:', profileResult.reason);
            }
            localStorage.setItem('name', result.data.alias);
          }

          setUserData({ userId: result.data.userID, alias: result.data.alias, fullName: fullName, email: formData.email });
          setShowLoginSuccessNotification(true);
          setTimeout(() => {
            window.location.href = '/';
          }, 1500);
        } else {
          const msg = result.message || "";
          if (['no activada', 'not activated', 'activar', 'revisa tu correo'].some(s => msg.toLowerCase().includes(s))) {
            // Redirigir automáticamente a verificación y reenviar el código
            try { await resendActivationCode(formData.email); } catch (_) {}
            setView('verification');
          } else setError(msg || "Error en login");
        }
      } else {
        const result = await registerUser(formData.name, formData.email, formData.password);
        if (result.success) { setShowSuccessNotification(true); setUserID(result.data); }
        else setError(result.message || "Error en registro");
      }
    } catch (err) {
      const errorMsg = err.message || "Error de conexión";
      // Traducir mensajes del backend
      if (errorMsg === "This email is not registered in the system") {
        setError("Este correo no está registrado en el sistema");
      } else if (errorMsg === "Invalid email or password") {
        setError("Correo o contraseña incorrectos");
      } else if (errorMsg === "This email is already registered and activated. Please login with your credentials.") {
        setError("Este correo ya está registrado y activado. Inicia sesión con tus credenciales.");
      } else if (errorMsg === "This email is already registered but not activated. If you want to activate your account, click on Resend activation email") {
        setError("Este correo ya está registrado pero no activado. Si quieres activar tu cuenta, haz clic en Reenviar correo de activación");
      } else if (errorMsg === "Account is not activated") {
        setError("Cuenta no activada");
      } else if (
        errorMsg.toLowerCase().includes("inactive") ||
        errorMsg.toLowerCase().includes("inactiva") ||
        errorMsg.toLowerCase().includes("desactivada") ||
        errorMsg.toLowerCase().includes("account_inactive") ||
        errorMsg.toLowerCase().includes("account_deactivated")
      ) {
        // Abre el modal de reactivación en lugar de mostrar solo texto
        setShowReactivationModal(true);
      } else if (
        errorMsg.toLowerCase().includes("blocked") ||
        errorMsg.toLowerCase().includes("bloqueada") ||
        errorMsg.toLowerCase().includes("account_blocked")
      ) {
        setError("Tu cuenta ha sido bloqueada. Por favor contacta a soporte.");
      } else {
        setError(errorMsg);
      }
    }
    finally { setLoading(false); }
  };

  // ─── Vistas secundarias ───────────────────────────────────────────────────
  if (view === 'verification')
    return <CodeVerification userID={userID} userEmail={formData.email} onVerificationSuccess={handleVerificationSuccess} onBackToLogin={() => setView('login')} />;

  if (view === 'forgot')
    return <ForgotPassword onBackToLogin={() => setView('login')} />;

  if (view === 'profile' && userData)
    return (
      <div className="min-h-screen flex">
        <div className="relative w-full overflow-hidden">
          <img
            src={loginHero}
            alt="Tu Evento — plataforma de eventos en vivo"
            className="absolute inset-0 w-full h-full object-cover"
          />
        </div>
        <div className="w-full bg-background flex items-center justify-center p-8">
          <div className="w-full max-w-sm space-y-6">
            <div className="text-center">
              <h1 className="text-2xl font-bold text-textPrimary">Perfil de Usuario</h1>
              <p className="text-textMuted text-sm mt-1">Bienvenido de vuelta</p>
            </div>
            <div className="space-y-4">
              {[
                ['Nombre', userData.fullName || 'No definido'],
                ['Alias', userData.alias],
                ['Correo', userData.email],
                ['ID', userData.userId]
              ].map(([label, val]) => (
                <div key={label}>
                  <label className="block text-textMuted text-sm mb-1">{label}</label>
                  <p className="text-textPrimary bg-surface rounded-lg px-4 py-3 text-sm">{val}</p>
                </div>
              ))}
            </div>
            <button onClick={handleLogout} className="w-full bg-error hover:bg-error/80 text-textPrimary font-semibold py-3 rounded-lg transition-all text-sm">
              Cerrar Sesión
            </button>
          </div>
        </div>
      </div>
    );

  // ─── Vista principal Login / Registro ────────────────────────────────────
  return (
    <div className="login-root">

      {/* ── Imagen hero anclada a la izquierda (~65% ancho) ── */}
      <img
        src={loginHero}
        alt="Tu Evento — plataforma de eventos en vivo"
        className="login-hero-img"
      />

      {/* ── Overlay muy sutil ── */}
      <div className="login-hero-overlay" />

      {/* ── Forma decorativa SVG — esquina superior izquierda (paralelogramo) ── */}
      <svg
        className="login-deco-tl"
        viewBox="0 0 260 260"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <polygon
          points="0,0 200,0 260,60 260,260"
          fill="rgba(109,40,217,0.07)"
          stroke="#a855f7"
          strokeWidth="1.5"
        />
        <polygon
          points="0,0 140,0 200,60 200,200"
          fill="none"
          stroke="rgba(168,85,247,0.30)"
          strokeWidth="1"
        />
      </svg>

      {/* ── Forma decorativa SVG — inferior derecha (misma que tl, girada 180° por CSS) ── */}
      <svg
        className="login-deco-br"
        viewBox="0 0 260 260"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <polygon
          points="0,0 200,0 260,60 260,260"
          fill="rgba(109,40,217,0.07)"
          stroke="#a855f7"
          strokeWidth="1.5"
        />
        <polygon
          points="0,0 140,0 200,60 200,200"
          fill="none"
          stroke="rgba(168,85,247,0.30)"
          strokeWidth="1"
        />
      </svg>

      {/* ── Layout interior — tarjeta empujada a la derecha ── */}
      <div className="login-inner">

        {/* ── Columna del formulario ── */}
        <div className="login-form-col">

          {/* ── Tarjeta glassmorphism ── */}
          <div className="login-card">

            {/* Encabezado */}
            <p className="login-card-eyebrow">BIENVENIDO DE NUEVO</p>
            <h1 className="login-card-title">
              {view === 'login' ? 'Iniciar Sesión' : 'Registrarse'}
            </h1>
            <p className="login-card-subtitle">
              Ingresa tus datos personales para acceder a tu cuenta.
            </p>

            {/* ── Formulario ── */}
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>

              {/* Nombre — solo registro */}
              {view !== 'login' && (
                <div>
                  <div className="login-input-wrap">
                    <User className="login-input-icon" aria-hidden="true" />
                    <input
                      type="text"
                      name="name"
                      value={formData.name}
                      onChange={handleInputChange}
                      placeholder="Nombre completo"
                      className={`login-input${fieldErrors.name ? ' login-input-error' : ''}`}
                      required
                      autoComplete="name"
                      aria-invalid={!!fieldErrors.name}
                      aria-describedby={fieldErrors.name ? 'err-name' : undefined}
                    />
                  </div>
                  {fieldErrors.name && (
                    <p id="err-name" className="login-field-error">
                      <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                      {fieldErrors.name}
                    </p>
                  )}
                  {formData.name && !fieldErrors.name && (
                    <div style={{ marginTop: '0.2rem', display: 'flex', flexDirection: 'column', gap: '0.15rem' }}>
                      {formData.name.trim().split(/\s+/).length < 2 && (
                        <p className="login-field-error">
                          <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                          Nombre y apellido
                        </p>
                      )}
                      {!formData.name.trim().split(/\s+/).every(w => w.length >= 3) && (
                        <p className="login-field-error">
                          <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                          Mínimo 3 caracteres por palabra
                        </p>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* Email */}
              <div>
                <div className="login-input-wrap">
                  <Mail className="login-input-icon" aria-hidden="true" />
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleInputChange}
                    placeholder="Correo electrónico"
                    className={`login-input${fieldErrors.email ? ' login-input-error' : ''}`}
                    required
                    autoComplete="email"
                    aria-invalid={!!fieldErrors.email}
                    aria-describedby={fieldErrors.email ? 'err-email' : undefined}
                  />
                </div>
                {fieldErrors.email && (
                  <p id="err-email" className="login-field-error">
                    <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                    {fieldErrors.email}
                  </p>
                )}
                {formData.email && !fieldErrors.email && view !== 'login' && !formData.email.trim().toLowerCase().endsWith('@gmail.com') && (
                  <p className="login-field-error">
                    <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                    Debe ser @gmail.com
                  </p>
                )}
              </div>

              {/* Contraseña */}
              <div>
                <div className="login-input-wrap">
                  <svg className="login-input-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={formData.password}
                    onChange={handleInputChange}
                    placeholder="Contraseña"
                    autoComplete="new-password"
                    className={`login-input${fieldErrors.password ? ' login-input-error' : ''}`}
                    required
                    aria-invalid={!!fieldErrors.password}
                    aria-describedby={fieldErrors.password ? 'err-password' : undefined}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="login-input-eye"
                    aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {showPassword ? <EyeOff width={18} height={18} /> : <Eye width={18} height={18} />}
                  </button>
                </div>
                {fieldErrors.password && (
                  <p id="err-password" className="login-field-error">
                    <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                    {fieldErrors.password}
                  </p>
                )}

                {/* Barra de fortaleza — solo registro */}
                {view !== 'login' && formData.password.length > 0 && (
                  <div style={{ marginTop: '0.5rem' }}>
                    <div style={{ display: 'flex', gap: '4px', marginBottom: '4px' }}>
                      {[1, 2, 3, 4].map(i => (
                        <div
                          key={i}
                          className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${passwordStrength >= i ? barColors[passwordStrength] : 'bg-surfaceAlt'}`}
                          style={{ height: '5px', flex: 1, borderRadius: '9999px' }}
                        />
                      ))}
                    </div>
                    <p className={`text-xs font-medium transition-colors duration-300 ${strengthColor[passwordStrength]}`}>
                      {strengthLabel[passwordStrength]}
                    </p>
                    {(() => {
                      const missing = [];
                      if (formData.password.length < 8) missing.push('8 caracteres');
                      if (!/[A-Z]/.test(formData.password)) missing.push('mayúscula');
                      if (!/[a-z]/.test(formData.password)) missing.push('minúscula');
                      if (!/\d/.test(formData.password)) missing.push('número');
                      if (!/[@$!%*?&]/.test(formData.password)) missing.push('carácter especial (@$!%*?&)');
                      return missing.length > 0 ? (
                        <p className="login-field-error" style={{ marginTop: '0.2rem' }}>
                          <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                          Debe contener {missing.join(', ')}
                        </p>
                      ) : null;
                    })()}
                  </div>
                )}
              </div>

              {/* Confirmar contraseña — solo registro */}
              {view !== 'login' && (
                <div>
                  <div className="login-input-wrap">
                    <svg className="login-input-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      name="confirmPassword"
                      value={formData.confirmPassword}
                      onChange={handleInputChange}
                      placeholder="Confirmar contraseña"
                      autoComplete="new-password"
                      className={`login-input${fieldErrors.confirmPassword ? ' login-input-error' : ''}`}
                      required
                      aria-invalid={!!fieldErrors.confirmPassword}
                      aria-describedby={fieldErrors.confirmPassword ? 'err-confirm' : undefined}
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="login-input-eye"
                      aria-label={showConfirmPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    >
                      {showConfirmPassword ? <EyeOff width={18} height={18} /> : <Eye width={18} height={18} />}
                    </button>
                  </div>
                  {fieldErrors.confirmPassword && (
                    <p id="err-confirm" className="login-field-error">
                      <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                      {fieldErrors.confirmPassword}
                    </p>
                  )}
                  {formData.confirmPassword && !fieldErrors.confirmPassword && formData.confirmPassword !== formData.password && (
                    <p className="login-field-error">
                      <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                      Las contraseñas no coinciden
                    </p>
                  )}
                </div>
              )}

              {/* Error global */}
              {error && (
                <p className="login-field-error" style={{ fontSize: '0.8125rem' }}>
                  <svg aria-hidden="true" fill="currentColor" width="14" height="14" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                  {error}
                </p>
              )}

              {/* Botón principal */}
              <button
                type="submit"
                disabled={loading}
                className="login-btn-primary"
                style={{ marginTop: '0.25rem' }}
              >
                {loading ? 'Cargando...' : (view === 'login' ? 'INICIAR SESIÓN' : 'REGISTRARSE')}
                {!loading && <ArrowRight width={16} height={16} aria-hidden="true" />}
              </button>

              {/* Reenviar activación — solo registro */}
              {view !== 'login' && (
                <p style={{ textAlign: 'center', fontSize: '0.75rem', color: 'rgba(196,181,253,0.65)' }}>
                  ¿No has activado tu cuenta?{' '}
                  <button
                    type="button"
                    onClick={handleResendActivation}
                    className="login-footer-link"
                    style={{ fontSize: '0.75rem' }}
                  >
                    Reenviar correo de activación
                  </button>
                </p>
              )}

              {/* Separador con "¿Olvidaste tu contraseña?" — solo login */}
              {view === 'login' && (
                <div className="login-separator">
                  <div className="login-separator-line" />
                  <span className="login-separator-text">
                    ¿No recuerdas tu contraseña?{' '}
                    <button
                      type="button"
                      onClick={() => setView('forgot')}
                      className="login-footer-link"
                      style={{ fontSize: '0.7rem', letterSpacing: '0.02em' }}
                    >
                      Recupérala
                    </button>
                  </span>
                  <div className="login-separator-line" />
                </div>
              )}

              {/* Separador antes de redes sociales */}
              {view !== 'login' && (
                <div className="login-separator">
                  <div className="login-separator-line" />
                  <span className="login-separator-text">o continúa con</span>
                  <div className="login-separator-line" />
                </div>
              )}

              {/* Botones sociales en grid 2 columnas */}
              <div className="login-social-grid">
                {/* Google */}
                <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
                  <GoogleButton onSuccess={handleGoogleSuccess} onError={handleGoogleError} />
                </GoogleOAuthProvider>

                {/* Facebook */}
                <button
                  type="button"
                  onClick={handleFacebookLogin}
                  className="login-btn-social"
                  title="Iniciar con Facebook"
                  aria-label="Iniciar sesión con Facebook"
                >
                  <svg width="18" height="18" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                    <circle cx="12" cy="12" r="12" fill="#1877f2" />
                    <path fill="white" d="M16.3 8H14c-.3 0-.7.3-.7.8V10h3l-.4 2.5H13.3V19h-2.6v-6.5H9V10h1.7V8.6C10.7 6.6 12 5.5 13.9 5.5c.9 0 1.9.1 2.4.2V8z"/>
                  </svg>
                  <span>Facebook</span>
                </button>
              </div>

              {/* ¿Tienes cuenta? */}
              <div style={{ textAlign: 'center', paddingTop: '0.25rem' }}>
                <p style={{ fontSize: '0.8rem', color: 'rgba(196,181,253,0.70)', marginBottom: '0.4rem' }}>
                  {view === 'login' ? '¿No tienes una cuenta?' : '¿Ya tienes una cuenta?'}{' '}
                  <button
                    type="button"
                    onClick={() => { setView(view === 'login' ? 'register' : 'login'); setError(''); setFieldErrors({}); setPasswordStrength(0); }}
                    className="login-footer-link"
                    style={{ fontSize: '0.8rem' }}
                  >
                    {view === 'login' ? 'Crear cuenta' : 'Inicia sesión'}
                  </button>
                </p>
                <p style={{ fontSize: '0.68rem', color: 'rgba(196,181,253,0.40)' }}>
                  {view === 'login' ? 'Al iniciar sesión, aceptas nuestros' : 'Al registrarte, aceptas nuestros'}{' '}
                  <button
                    type="button"
                    onClick={() => setShowTermsModal(true)}
                    className="login-footer-link"
                    style={{ fontSize: '0.68rem' }}
                  >
                    Términos y condiciones
                  </button>
                </p>
              </div>

            </form>
          </div>{/* /login-card */}
        </div>{/* /login-form-col */}
      </div>{/* /login-inner */}

      {/* ══════════════ MODAL TÉRMINOS ══════════════ */}
      <BaseModal
        isOpen={showTermsModal}
        onClose={() => setShowTermsModal(false)}
        compact
        title="Términos y Condiciones de Uso"
        maxWidth="max-w-2xl"
        scrollableBody
        actions={[
          {
            label: 'Entendido',
            icon: <FileText className="w-4 h-4" />,
            variant: 'primary',
            onClick: () => setShowTermsModal(false),
          },
        ]}
      >
        <div className="text-sm text-textSecondary space-y-4">
          <p className="bm-hint"><strong className="text-textPrimary">Última actualización:</strong> 2/10/2025</p>
          <h4 className="font-semibold text-textPrimary">1. Aceptación de los términos</h4>
          <p>Al acceder, registrarse o utilizar la aplicación "Tu Evento" (en su versión web o Android), desarrollada por CapySoft, el usuario acepta expresamente los presentes Términos y Condiciones.</p>
          <h4 className="font-semibold text-textPrimary">2. Definiciones</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li><strong>Aplicación / Plataforma:</strong> "Tu Evento" en su versión web y móvil.</li>
            <li><strong>Usuario:</strong> Persona que accede y utiliza la aplicación.</li>
            <li><strong>Organizador:</strong> Usuario autorizado para crear y publicar eventos.</li>
            <li><strong>Asistente:</strong> Usuario que reserva o participa en eventos.</li>
            <li><strong>Administrador:</strong> Usuario con permisos especiales de gestión.</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">3. Uso de la plataforma</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>Requiere conexión estable a Internet.</li>
            <li>Los usuarios deben registrarse con datos verídicos.</li>
            <li>El sistema no gestiona pagos en línea.</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">4. Registro y cuentas</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>Cuenta con correo válido y contraseña segura.</li>
            <li>Posible registro mediante Google/Facebook (OAuth).</li>
            <li>El usuario es responsable de sus credenciales.</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">5. Reservas y tickets</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>Reserva en estado pendiente hasta validación de pago.</li>
            <li>Se genera un código QR único e intransferible al confirmar.</li>
            <li>La falsificación de QR implica denegación de acceso.</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">6. Responsabilidades del usuario</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>Uso correcto y lícito de la plataforma.</li>
            <li>No difundir información falsa u ofensiva.</li>
            <li>No vulnerar la seguridad del sistema.</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">7. Limitación de responsabilidades</h4>
          <p>CapySoft no se hace responsable de fallas de conexión, información falsa de terceros ni cancelaciones ajenas al control de la plataforma.</p>
          <h4 className="font-semibold text-textPrimary">8. Seguridad y privacidad</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>Autenticación de dos pasos en registro normal.</li>
            <li>En OAuth, la seguridad depende del proveedor externo.</li>
          </ul>
          <h4 className="font-semibold">9. Propiedad intelectual</h4>
          <p>Prohibida la reproducción o modificación sin autorización expresa.</p>
          <h4 className="font-semibold">10. Modificaciones</h4>
          <p>CapySoft puede modificar estos Términos en cualquier momento.</p>
          <h4 className="font-semibold">11. Legislación aplicable</h4>
          <p>Regidos por las leyes vigentes en Colombia.</p>
        </div>
      </BaseModal>

      {/* ══════════════ NOTIFICACIÓN REGISTRO EXITOSO ══════════════ */}
      <BaseModal
        isOpen={showSuccessNotification}
        onClose={() => {}}
        hideOverlayClose
        variant="success"
        icon={<CheckCircle className="w-8 h-8" />}
        title="¡Registro Exitoso!"
        subtitle="Tu cuenta ha sido creada correctamente"
        decorIcons={
          <>
            <PartyPopper aria-hidden="true" className="absolute top-3 left-3 w-5 h-5 -rotate-12" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <Sparkles    aria-hidden="true" className="absolute top-3 right-3 w-[18px] h-[18px]" style={{ color: 'rgba(253,224,71,0.75)' }} />
            <PartyPopper aria-hidden="true" className="absolute bottom-3 right-4 w-4 h-4 rotate-12"  style={{ color: 'rgba(255,255,255,0.35)' }} />
          </>
        }
        actions={[
          {
            label: 'Continuar a Verificación',
            icon: <CheckCircle className="w-4 h-4" />,
            variant: 'primary',
            onClick: handleContinueToVerification,
          },
        ]}
      >
        <div className="bm-info-card text-center">
          <div className="flex items-center justify-center mb-2">
            <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'color-mix(in srgb, var(--color-accent) 15%, transparent)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Mail className="w-5 h-5 text-accent" />
            </div>
          </div>
          <p className="bm-label font-medium">Revisa tu bandeja de entrada</p>
          <p className="bm-hint mt-1">Te enviamos un código de activación a</p>
          <p className="bm-accent-text mt-1">{formData.email}</p>
          <p className="bm-hint mt-2">¿No lo encuentras? Revisa tu carpeta de spam</p>
        </div>
      </BaseModal>

      {/* ══════════════ NOTIFICACIÓN LOGIN EXITOSO ══════════════ */}
      <BaseModal
        isOpen={showLoginSuccessNotification}
        onClose={() => {}}
        hideOverlayClose
        variant="success"
        icon={<CheckCircle className="w-8 h-8" />}
        title="¡Bienvenido de Vuelta!"
        subtitle="Has iniciado sesión exitosamente"
        decorIcons={
          <>
            <PartyPopper aria-hidden="true" className="absolute top-3 left-3 w-5 h-5 -rotate-12" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <Sparkles    aria-hidden="true" className="absolute top-3 right-3 w-[18px] h-[18px]" style={{ color: 'rgba(253,224,71,0.75)' }} />
            <PartyPopper aria-hidden="true" className="absolute bottom-3 right-4 w-4 h-4 rotate-12"  style={{ color: 'rgba(255,255,255,0.35)' }} />
          </>
        }
        actions={[
          {
            label: 'Ir a Inicio',
            icon: <ArrowRight className="w-4 h-4" />,
            variant: 'primary',
            onClick: handleContinueToHome,
          },
        ]}
      >
        <div className="bm-info-card text-center">
          <div className="flex items-center justify-center mb-2">
            <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'color-mix(in srgb, var(--color-accent) 15%, transparent)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <User className="w-5 h-5 text-accent" />
            </div>
          </div>
          <p className="bm-label font-medium">Sesión iniciada</p>
          <p className="bm-hint mt-1">Accede a todas las funcionalidades de TuEvento</p>
          <p className="bm-accent-text mt-2">👋 ¡Hola, {(() => {
            if (!userData?.fullName) return userData?.alias || formData.email;
            const parts = userData.fullName.split(' ').filter(p => p.trim().length > 0);
            if (parts.length === 0) return userData?.alias || formData.email;
            if (parts.length === 1) return parts[0];
            return parts[0].length <= 3 ? `${parts[0]} ${parts[1]}` : parts[0];
          })()}!</p>
        </div>
      </BaseModal>

      {/* ══════════════ MODAL REACTIVACIÓN ══════════════ */}
      <ReactivationModal
        isOpen={showReactivationModal}
        onClose={() => setShowReactivationModal(false)}
        email={formData.email}
      />

    </div>
  );
}