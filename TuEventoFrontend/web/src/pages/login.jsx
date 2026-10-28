import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Eye, EyeOff, Mail, User, CheckCircle, ArrowRight, PartyPopper, Sparkles, FileText, X } from "lucide-react";
import { useTranslation } from 'react-i18next';
import loginHero from "../assets/images/tu-evento-login-hero-left.png";
import { loginUser, registerUser, resendActivationCode, googleLogin } from "../services/Login.js";
import { getProfileByUserId } from "../services/ProfileService.js";
import { useTheme } from "../context/ThemeContext";
import { performLogout } from "../services/httpClient.js";
import { getAccessToken, setAccessToken } from "../services/tokenStore.js";
import CodeVerification from "./CodeVerification.jsx";
import ForgotPassword from "./ForgotPassword.jsx";
import BaseModal from "../components/common/BaseModal.jsx";
import ReactivationModal from "../components/common/ReactivationModal.jsx";
import LanguageSelector from "../components/common/LanguageSelector.jsx";
import { renderGoogleButton, releaseGoogleButton } from "../services/googleIdentity.js";

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

// oauth_error codes sent by GET /auth/oauth/{provider}/callback → i18n key.
// ACCOUNT_DEACTIVATED opens the reactivation modal instead; unknown codes use the generic message.
const OAUTH_ERROR_MESSAGES = {
  OAUTH_CANCELLED: 'login.oauthErrors.cancelled',
  OAUTH_FAILED: 'login.oauthErrors.failed',
  ACCOUNT_LOCKED: 'login.oauthErrors.locked',
  ACCOUNT_BLOCKED: 'login.errors.accountBlocked',
  ACCOUNT_DELETED: 'login.oauthErrors.deleted',
};

/**
 * GoogleButton — renders the Google Identity Services button with an overlay
 * technique so it matches the card's visual style.
 *
 * GSI is loaded and initialized once per page load (services/googleIdentity.js);
 * each mount only re-renders the button, so it keeps working after navigating back
 * to /login without a full page reload (e.g. after signing out from the profile view).
 */
function GoogleButton({ onSuccess, onError }) {
  const { t } = useTranslation();
  const label = `${t('login.signInWith')} ${t('login.google')}`;
  const containerRef = useRef(null);
  const onSuccessRef = useRef(onSuccess);
  const onErrorRef = useRef(onError);
  onSuccessRef.current = onSuccess;
  onErrorRef.current = onError;

  useEffect(() => {
    const container = containerRef.current;
    // Same contract as @react-oauth/google: onSuccess({ credential, select_by }) or onError()
    const onCredential = (response) => {
      if (!response?.credential) return onErrorRef.current?.();
      return onSuccessRef.current?.({ credential: response.credential, select_by: response.select_by });
    };

    if (GOOGLE_CLIENT_ID) {
      renderGoogleButton(container, {
        clientId: GOOGLE_CLIENT_ID,
        onCredential,
        buttonOptions: { type: 'standard', theme: 'outline', size: 'large', width: 400 },
      }).catch((err) => console.warn('[GoogleButton] Google Identity Services unavailable:', err.message));
    }

    return () => {
      releaseGoogleButton(onCredential);
      container.replaceChildren();
    };
  }, []);

  return (
    <div className="login-google-wrapper" role="button" tabIndex={0} aria-label={label}>
      {/* ── Capa visual ── */}
      <div className="login-google-visual">
        <svg width="18" height="18" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
          <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
          <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
          <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
          <path fill="none" d="M0 0h48v48H0z"/>
        </svg>
        <span style={{ fontSize: '0.9375rem', fontWeight: 600 }}>{label}</span>
      </div>

      {/* ── Overlay invisible full-width — dispara el flujo OAuth real ── */}
      <div className="login-google-overlay" aria-hidden="true">
        <div ref={containerRef} />
      </div>
    </div>
  );
}

export default function Login() {
  const { refreshPalette } = useTheme();
  const { t } = useTranslation();
  const navigate = useNavigate();
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

    // The OAuth callback redirects here with oauth_error=<code> when the provider was
    // cancelled or the sign-in was rejected (blocked, deactivated, ...).
    const oauthError = urlParams.get('oauth_error');
    if (oauthError) {
      sessionStorage.removeItem('oauth_state');
      window.history.replaceState({}, document.title, window.location.pathname);
      if (oauthError === 'ACCOUNT_DEACTIVATED') {
        setShowReactivationModal(true);
      } else {
        setError(t(OAUTH_ERROR_MESSAGES[oauthError] ?? 'login.oauthErrors.failed'));
      }
      return;
    }

    const userID = urlParams.get('userID');
    const role = urlParams.get('role');
    const oauth = urlParams.get('oauth');

    if (oauth === 'true' && userID && role) {
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
      // same shape. The tokens are not in the URL: the refresh token arrived as
      // an httpOnly cookie and restoreSession() gets the access token on the
      // full-page redirect below.
      localStorage.setItem('userID',       userID);
      localStorage.setItem('role',         role);

      const alias        = urlParams.get('alias')        || '';
      const profileId    = urlParams.get('profileId');
      const needsOnboarding = urlParams.get('needsOnboarding') === 'true';

      if (alias)        localStorage.setItem('alias',        alias);

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

      void (async () => {
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
      const oauthRole = urlParams.get('role') || '';
      window.location.href = (oauthRole === 'ADMIN' || localStorage.getItem('userEmail') === 'tuevento.capysoft@gmail.com') ? '/admin-panel' : '/';
      return;
    }

    const storedToken = getAccessToken();
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

  const strengthLabel = ['', t('login.passwordStrength.veryWeak'), t('login.passwordStrength.weak'), t('login.passwordStrength.good'), t('login.passwordStrength.strong')];
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
    if (!email?.trim()) return t('login.validation.emailRequired');
    if (email.trim().length > 255) return t('login.validation.emailMax');
    if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email.trim()))
      return t('login.validation.emailInvalid');
    if (!email.trim().toLowerCase().endsWith('@gmail.com'))
      return t('login.validation.emailGmailOnly');
    return "";
  };

  const validatePassword = (pw) => {
    if (!pw?.trim()) return t('login.validation.passwordRequired');
    if (pw.length < 8) return t('login.validation.passwordMin');
    if (pw.length > 100) return t('login.validation.passwordMax');
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/.test(pw))
      return t('login.validation.passwordWeak');
    return "";
  };

  const validateName = (name) => {
    if (!name?.trim()) return t('login.validation.nameRequired');
    if (name.trim().length > 100) return t('login.validation.nameMax');
    if (!/^[a-zA-ZÀ-ÿ\s'-]+$/.test(name.trim()))
      return t('login.validation.nameLettersOnly');
    
    const words = name.trim().split(/\s+/);
    if (words.length < 2) return t('login.validation.nameFirstLast');
    
    // Validar que cada palabra tenga al menos 3 caracteres
    const invalidWord = words.find(word => word.length < 3);
    if (invalidWord) return t('login.validation.nameMinPerWord');
    
    return "";
  };

  const validateConfirmPassword = (cp, pw) => {
    if (!cp?.trim()) return t('login.validation.confirmRequired');
    if (cp !== pw) return t('login.validation.confirmMatch');
    return "";
  };

  const handleVerificationSuccess = () => setView('login');
  // El registro ya envía el correo de activación: no se reenvía aquí, porque eso mandaba un
  // segundo correo con otro código que invalidaba el primero. El usuario puede pedir
  // un reenvío explícito desde la pantalla de verificación.
  const handleContinueToVerification = () => {
    setShowSuccessNotification(false);
    setUserID(formData.userID);
    setView('verification');
  };
  const handleContinueToHome = () => {
    setShowLoginSuccessNotification(false);
    const role = localStorage.getItem('role');
    const email = localStorage.getItem('userEmail');
    window.location.href = (role === 'ADMIN' || email === 'tuevento.capysoft@gmail.com') ? '/admin-panel' : '/';
  };
  const handleLogout = async () => { 
    await clearAuth(); 
    setUserData(null); 
    setView('login'); 
  };

  const handleResendActivation = async () => {
    if (!formData.email || !formData.email.trim()) {
      setError(t('login.errors.enterEmailResend'));
      return;
    }
    try {
      await resendActivationCode(formData.email);
      setView('verification'); // redirigir directo a CodeVerification con código ya reenviado
    } catch (err) {
      const errorMsg = err.message || t('login.errors.resendError');
      // Traducir mensajes del backend
      if (errorMsg === "EMAIL_NOT_FOUND") {
        setError(t('login.errors.emailNotRegistered'));
      } else if (errorMsg === "This email is already activated. Please login with your credentials.") {
        setError(t('login.errors.emailAlreadyRegistered'));
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
      setAccessToken(result.data.token);
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
      setTimeout(() => {
        const role = result.data.role || localStorage.getItem('role');
        window.location.href = (role === 'ADMIN' || googleEmail === 'tuevento.capysoft@gmail.com') ? '/admin-panel' : '/';
      }, 1500);
    } catch (err) {
      setError(err.message || t('login.errors.googleLoginError'));
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleError = () => {
    // User dismissed the popup or Google returned an error — no action needed
    // unless the user explicitly triggered an error (not a cancel).
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
          setAccessToken(result.data.token);
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
            const role = result.data.role || 'USER';
            window.location.href = (role === 'ADMIN' || formData.email === 'tuevento.capysoft@gmail.com') ? '/admin-panel' : '/';
          }, 1500);
        } else {
          const msg = result.message || "";
          if (['no activada', 'not activated', 'activar', 'revisa tu correo'].some(s => msg.toLowerCase().includes(s))) {
            // Redirigir automáticamente a verificación y reenviar el código
            try { await resendActivationCode(formData.email); } catch (_) {}
            setView('verification');
          } else setError(msg || t('login.errors.loginError'));
        }
      } else {
        const result = await registerUser(formData.name, formData.email, formData.password);
        if (result.success) { setShowSuccessNotification(true); setUserID(result.data); }
        else setError(result.message || t('login.errors.registerError'));
      }
    } catch (err) {
      const errorMsg = err.message || t('login.errors.connectionError');
      // Traducir mensajes del backend
      if (errorMsg === "This email is not registered in the system") {
        setError(t('login.errors.emailNotRegistered'));
      } else if (errorMsg === "Invalid email or password") {
        setError(t('login.errors.invalidCredentials'));
      } else if (errorMsg === "This email is already registered and activated. Please login with your credentials.") {
        setError(t('login.errors.emailAlreadyRegistered'));
      } else if (errorMsg === "This email is already registered but not activated. If you want to activate your account, click on Resend activation email") {
        setError(t('login.errors.emailNotActivated'));
      } else if (errorMsg === "Account is not activated") {
        setError(t('login.errors.accountNotActivated'));
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
        setError(t('login.errors.accountBlocked'));
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
              <h1 className="text-2xl font-bold text-textPrimary">{t('login.profile.title')}</h1>
              <p className="text-textMuted text-sm mt-1">{t('login.profile.welcomeBack')}</p>
            </div>
            <div className="space-y-4">
              {[
                [t('login.profile.name'), userData.fullName || t('login.profile.notDefined')],
                [t('login.profile.alias'), userData.alias],
                [t('login.profile.email'), userData.email],
                [t('login.profile.id'), userData.userId]
              ].map(([label, val]) => (
                <div key={label}>
                  <label className="block text-textMuted text-sm mb-1">{label}</label>
                  <p className="text-textPrimary bg-surface rounded-lg px-4 py-3 text-sm">{val}</p>
                </div>
              ))}
            </div>
            <button onClick={handleLogout} className="w-full bg-error hover:bg-error/80 text-textPrimary font-semibold py-3 rounded-lg transition-all text-sm">
              {t('login.profile.logout')}
            </button>
          </div>
        </div>
      </div>
    );

  // ─── Vista principal Login / Registro ────────────────────────────────────
  return (
    <div className="login-root">
      
      {/* ── Selector de idioma (fixed top-right) ── */}
      <LanguageSelector position="fixed-top-right" />

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

            {/* ── Botón volver al landing ── */}
            <button
              type="button"
              onClick={() => navigate('/')}
              className="login-back-btn"
              aria-label="Volver al inicio"
            >
              <ArrowRight
                size={26}
                style={{ transform: 'rotate(180deg)' }}
                aria-hidden="true"
              />
            </button>

            {/* Encabezado */}
            <p className="login-card-eyebrow">{t('login.welcomeBack').toUpperCase()}</p>
            <h1 className="login-card-title">
              {view === 'login' ? t('login.signIn') : t('login.signUp')}
            </h1>
            <p className="login-card-subtitle">
              {t('login.enterDetails')}
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
                      placeholder={t('login.fullName')}
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
                          {t('login.validation.nameFirstLast')}
                        </p>
                      )}
                      {!formData.name.trim().split(/\s+/).every(w => w.length >= 3) && (
                        <p className="login-field-error">
                          <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                          {t('login.validation.nameMinPerWord')}
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
                    placeholder={t('login.email')}
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
                    {t('login.mustBeGmail')}
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
                    placeholder={t('login.password')}
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
                    aria-label={showPassword ? t('login.hidePassword') : t('login.showPassword')}
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
                      if (formData.password.length < 8) missing.push(`8 ${t('login.passwordStrength.characters')}`);
                      if (!/[A-Z]/.test(formData.password)) missing.push(t('login.passwordStrength.uppercase'));
                      if (!/[a-z]/.test(formData.password)) missing.push(t('login.passwordStrength.lowercase'));
                      if (!/\d/.test(formData.password)) missing.push(t('login.passwordStrength.number'));
                      if (!/[@$!%*?&]/.test(formData.password)) missing.push(t('login.passwordStrength.specialChar'));
                      return missing.length > 0 ? (
                        <p className="login-field-error" style={{ marginTop: '0.2rem' }}>
                          <svg aria-hidden="true" fill="currentColor" width="13" height="13" viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
                          {t('login.passwordStrength.mustContain')} {missing.join(', ')}
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
                      placeholder={t('login.confirmPassword')}
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
                {loading ? t('login.loading') : (view === 'login' ? t('login.signInButton') : t('login.signUpButton'))}
                {!loading && <ArrowRight width={16} height={16} aria-hidden="true" />}
              </button>

              {/* Reenviar activación — solo registro */}
              {view !== 'login' && (
                <p style={{ textAlign: 'center', fontSize: '0.75rem', color: 'rgba(196,181,253,0.65)' }}>
                  {t('login.notActivated')}{' '}
                  <button
                    type="button"
                    onClick={handleResendActivation}
                    className="login-footer-link"
                    style={{ fontSize: '0.75rem' }}
                  >
                    {t('login.resendActivation')}
                  </button>
                </p>
              )}

              {/* Separador con "¿Olvidaste tu contraseña?" — solo login */}
              {view === 'login' && (
                <div className="login-separator">
                  <div className="login-separator-line" />
                  <span className="login-separator-text">
                    {t('login.forgotPassword')}{' '}
                    <button
                      type="button"
                      onClick={() => setView('forgot')}
                      className="login-footer-link"
                      style={{ fontSize: '0.7rem', letterSpacing: '0.02em' }}
                    >
                      {t('login.recoverIt')}
                    </button>
                  </span>
                  <div className="login-separator-line" />
                </div>
              )}

              {/* Separador antes de redes sociales */}
              {view !== 'login' && (
                <div className="login-separator">
                  <div className="login-separator-line" />
                  <span className="login-separator-text">{t('login.orContinueWith')}</span>
                  <div className="login-separator-line" />
                </div>
              )}

              {/* Botones sociales */}
              <div className="login-social-grid">
                {/* Google */}
                <GoogleButton onSuccess={handleGoogleSuccess} onError={handleGoogleError} />
              </div>

              {/* ¿Tienes cuenta? */}
              <div style={{ textAlign: 'center', paddingTop: '0.25rem' }}>
                <p style={{ fontSize: '0.8rem', color: 'rgba(196,181,253,0.70)', marginBottom: '0.4rem' }}>
                  {view === 'login' ? t('login.noAccount') : t('login.hasAccount')}{' '}
                  <button
                    type="button"
                    onClick={() => { setView(view === 'login' ? 'register' : 'login'); setError(''); setFieldErrors({}); setPasswordStrength(0); }}
                    className="login-footer-link"
                    style={{ fontSize: '0.8rem' }}
                  >
                    {view === 'login' ? t('login.createAccount') : t('login.signInLink')}
                  </button>
                </p>
                <p style={{ fontSize: '0.68rem', color: 'rgba(196,181,253,0.40)' }}>
                  {view === 'login' ? t('login.bySigningIn') : t('login.bySigningUp')}{' '}
                  <button
                    type="button"
                    onClick={() => setShowTermsModal(true)}
                    className="login-footer-link"
                    style={{ fontSize: '0.68rem' }}
                  >
                    {t('login.termsAndConditions')}
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
        title={t('login.modals.termsTitle')}
        maxWidth="max-w-2xl"
        scrollableBody
        actions={[
          {
            label: t('login.modals.understood'),
            icon: <FileText className="w-4 h-4" />,
            variant: 'primary',
            onClick: () => setShowTermsModal(false),
          },
        ]}
      >
        <div className="text-sm text-textSecondary space-y-4">
          <p className="bm-hint"><strong className="text-textPrimary">{t('login.modals.lastUpdate')}</strong> 2/10/2025</p>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section1Title')}</h4>
          <p>{t('login.terms.section1Text')}</p>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section2Title')}</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li><strong>{t('login.terms.app')}</strong> {t('login.terms.appDesc')}</li>
            <li><strong>{t('login.terms.user')}</strong> {t('login.terms.userDesc')}</li>
            <li><strong>{t('login.terms.organizer')}</strong> {t('login.terms.organizerDesc')}</li>
            <li><strong>{t('login.terms.attendee')}</strong> {t('login.terms.attendeeDesc')}</li>
            <li><strong>{t('login.terms.admin')}</strong> {t('login.terms.adminDesc')}</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section3Title')}</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>{t('login.terms.section3Item1')}</li>
            <li>{t('login.terms.section3Item2')}</li>
            <li>{t('login.terms.section3Item3')}</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section4Title')}</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>{t('login.terms.section4Item1')}</li>
            <li>{t('login.terms.section4Item2')}</li>
            <li>{t('login.terms.section4Item3')}</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section5Title')}</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>{t('login.terms.section5Item1')}</li>
            <li>{t('login.terms.section5Item2')}</li>
            <li>{t('login.terms.section5Item3')}</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section6Title')}</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>{t('login.terms.section6Item1')}</li>
            <li>{t('login.terms.section6Item2')}</li>
            <li>{t('login.terms.section6Item3')}</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section7Title')}</h4>
          <p>{t('login.terms.section7Text')}</p>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section8Title')}</h4>
          <ul className="list-disc pl-6 space-y-1">
            <li>{t('login.terms.section8Item1')}</li>
            <li>{t('login.terms.section8Item2')}</li>
          </ul>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section9Title')}</h4>
          <p>{t('login.terms.section9Text')}</p>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section10Title')}</h4>
          <p>{t('login.terms.section10Text')}</p>
          <h4 className="font-semibold text-textPrimary">{t('login.terms.section11Title')}</h4>
          <p>{t('login.terms.section11Text')}</p>
        </div>
      </BaseModal>

      {/* ══════════════ NOTIFICACIÓN REGISTRO EXITOSO ══════════════ */}
      <BaseModal
        isOpen={showSuccessNotification}
        onClose={() => {}}
        hideOverlayClose
        variant="success"
        icon={<CheckCircle className="w-8 h-8" />}
        title={t('login.modals.successTitle')}
        subtitle={t('login.modals.successSubtitle')}
        decorIcons={
          <>
            <PartyPopper aria-hidden="true" className="absolute top-3 left-3 w-5 h-5 -rotate-12" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <Sparkles    aria-hidden="true" className="absolute top-3 right-3 w-[18px] h-[18px]" style={{ color: 'rgba(253,224,71,0.75)' }} />
            <PartyPopper aria-hidden="true" className="absolute bottom-3 right-4 w-4 h-4 rotate-12"  style={{ color: 'rgba(255,255,255,0.35)' }} />
          </>
        }
        actions={[
          {
            label: t('login.modals.continueVerification'),
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
          <p className="bm-label font-medium">{t('login.modals.checkInbox')}</p>
          <p className="bm-hint mt-1">{t('login.modals.sentCodeTo')}</p>
          <p className="bm-accent-text mt-1">{formData.email}</p>
          <p className="bm-hint mt-2">{t('login.modals.checkSpam')}</p>
        </div>
      </BaseModal>

      {/* ══════════════ NOTIFICACIÓN LOGIN EXITOSO ══════════════ */}
      <BaseModal
        isOpen={showLoginSuccessNotification}
        onClose={() => {}}
        hideOverlayClose
        variant="success"
        icon={<CheckCircle className="w-8 h-8" />}
        title={t('login.modals.loginSuccessTitle')}
        subtitle={t('login.modals.loginSuccessSubtitle')}
        decorIcons={
          <>
            <PartyPopper aria-hidden="true" className="absolute top-3 left-3 w-5 h-5 -rotate-12" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <Sparkles    aria-hidden="true" className="absolute top-3 right-3 w-[18px] h-[18px]" style={{ color: 'rgba(253,224,71,0.75)' }} />
            <PartyPopper aria-hidden="true" className="absolute bottom-3 right-4 w-4 h-4 rotate-12"  style={{ color: 'rgba(255,255,255,0.35)' }} />
          </>
        }
        actions={[
          {
            label: t('login.modals.goHome'),
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
          <p className="bm-label font-medium">{t('login.modals.sessionStarted')}</p>
          <p className="bm-hint mt-1">{t('login.modals.accessAll')}</p>
          <p className="bm-accent-text mt-2">👋 {t('login.modals.hello')}, {(() => {
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