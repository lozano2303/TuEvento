import React, { useState, useEffect } from "react";
import { Eye, EyeOff, Mail, User, CheckCircle, ArrowRight } from "lucide-react";
import { loginUser, registerUser, resendActivationCode } from "../services/Login.js";
import { getProfileByUserId } from "../services/ProfileService.js";
import CodeVerification from "./CodeVerification.jsx";
import ForgotPassword from "./ForgotPassword.jsx";

export default function Login() {
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
      localStorage.setItem('token', token);
      localStorage.setItem('userID', userID);
      localStorage.setItem('role', role);
      window.history.replaceState({}, document.title, window.location.pathname);
      window.location.href = '/';
      return;
    }

    const storedToken = localStorage.getItem('token');
    const storedUserID = localStorage.getItem('userID');
    const storedAlias = localStorage.getItem('alias');
    const storedEmail = localStorage.getItem('userEmail');
    const storedFullName = localStorage.getItem('fullName');
    if (storedToken && storedUserID) {
      setUserData({ userId: storedUserID, alias: storedAlias, email: storedEmail, fullName: storedFullName });
      setView('profile');
    }
  }, []);

  const clearAuth = () => {
    ['token', 'userID', 'alias', 'userEmail', 'fullName', 'pendingActivationUserID', 'adminLoggedIn']
      .forEach(k => localStorage.removeItem(k));
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
  const strengthColor = ['', 'text-red-400', 'text-yellow-400', 'text-blue-400', 'text-green-400'];
  const barColors = [
    '',
    'bg-red-500',
    'bg-yellow-500',
    'bg-blue-500',
    'bg-green-500',
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
  const handleContinueToVerification = () => { setShowSuccessNotification(false); setView('verification'); };
  const handleContinueToHome = () => { setShowLoginSuccessNotification(false); window.location.href = '/'; };
  const handleLogout = () => { clearAuth(); setUserData(null); setView('login'); };

  const handleResendActivation = async () => {
    if (!formData.email || !formData.email.trim()) {
      setError("Por favor, ingresa tu correo electrónico para reenviar el código de activación.");
      return;
    }
    try {
      const result = await resendActivationCode(formData.email);
      if (result.success) {
        setError("Se ha enviado un nuevo código de activación a tu correo.");
        setShowActivateAccount(true);
      } else {
        setError(result.message || "Error al reenviar código de activación");
      }
    } catch (err) {
      setError("Error de conexión al reenviar código de activación");
    }
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
          localStorage.setItem('userID', result.data.userID);
          localStorage.setItem('alias', result.data.alias);
          localStorage.setItem('userEmail', formData.email);
          localStorage.setItem('role', result.data.role || 'USER');
          
          // Obtener el perfil del usuario para conseguir el fullName (como en el móvil)
          let fullName = result.data.alias; // fallback al alias
          try {
            const profileResult = await getProfileByUserId(result.data.userID);
            if (profileResult.success && profileResult.data.fullName) {
              fullName = profileResult.data.fullName;
              localStorage.setItem('name', fullName);
            }
          } catch (error) {
            console.error('Error al obtener perfil:', error);
            // Si falla, usar el alias como nombre
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
            setShowActivateAccount(true);
            setError("Tu cuenta no está activada. Activa tu cuenta para continuar.");
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
        <div className="w-full bg-gradient-to-br from-purple-600 via-purple-700 to-purple-800 flex items-center justify-center p-8">
          <img
            src="/src/assets/images/fondologin.png"
            alt="Ilustración escritorio"
            className="w-full max-w-xs drop-shadow-2xl hover:scale-105 transition-transform duration-500"
          />
        </div>
        <div className="w-full bg-gray-900 flex items-center justify-center p-8">
          <div className="w-full max-w-sm space-y-6">
            <div className="text-center">
              <h1 className="text-2xl font-bold text-white">Perfil de Usuario</h1>
              <p className="text-gray-400 text-sm mt-1">Bienvenido de vuelta</p>
            </div>
            <div className="space-y-4">
              {[
                ['Nombre', userData.fullName || 'No definido'],
                ['Alias', userData.alias], 
                ['Correo', userData.email], 
                ['ID', userData.userId]
              ].map(([label, val]) => (
                <div key={label}>
                  <label className="block text-gray-400 text-sm mb-1">{label}</label>
                  <p className="text-white bg-gray-800 rounded-lg px-4 py-3 text-sm">{val}</p>
                </div>
              ))}
            </div>
            <button onClick={handleLogout} className="w-full bg-red-600 hover:bg-red-700 text-white font-semibold py-3 rounded-lg transition-all text-sm">
              Cerrar Sesión
            </button>
          </div>
        </div>
      </div>
    );

  // ─── Vista principal Login / Registro ────────────────────────────────────
  return (
    <div className="min-h-screen flex items-stretch">

      {/* Columna izquierda */}
      <div className="w-full bg-gradient-to-br from-purple-600 via-purple-700 to-purple-800 flex items-center justify-center p-8">
        <img src="/src/assets/images/fondologin.png" alt="Ilustración escritorio" className="w-full max-w-xs drop-shadow-2xl" />
      </div>

      {/* Columna derecha */}
      <div className="w-full bg-gray-900 flex items-center justify-center p-10">
        <div className="w-full max-w-sm">

          {/* Título */}
          <div className="text-center mb-8">
            <h1 className="text-3xl font-bold text-white">
              {view === 'login' ? "Iniciar Sesión" : "Registrarse"}
            </h1>
            <p className="text-gray-400 text-sm mt-2">Ingresa tus datos personales</p>
          </div>

          {/* Formulario */}
          <form onSubmit={handleSubmit} className="space-y-4">

            {/* Nombre — solo registro */}
            {view !== 'login' && (
              <div>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                  <input
                    type="text" name="name" value={formData.name}
                    onChange={handleInputChange}
                    placeholder="Nombre completo"
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg pl-10 pr-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
                    required
                  />
                </div>
                {fieldErrors.name && <p className="text-red-400 text-xs mt-1">{fieldErrors.name}</p>}
                {formData.name && !fieldErrors.name && (
                  <div className="mt-1 space-y-0.5">
                    {formData.name.trim().split(/\s+/).length < 2 && (
                      <p className="text-xs text-red-400 flex items-center">
                        <svg aria-hidden="true" className="Qk3oof xTjuxe mr-1" fill="currentColor" focusable="false" width="16px" height="16px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path></svg> Nombre y apellido
                      </p>
                    )}
                    {!formData.name.trim().split(/\s+/).every(w => w.length >= 3) && (
                      <p className="text-xs text-red-400 flex items-center">
                        <svg aria-hidden="true" className="Qk3oof xTjuxe mr-1" fill="currentColor" focusable="false" width="16px" height="16px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path></svg> Mínimo 3 caracteres por palabra
                      </p>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Email */}
            <div>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                <input
                  type="email" name="email" value={formData.email}
                  onChange={handleInputChange}
                  placeholder="Correo electrónico"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg pl-10 pr-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
                  required
                />
              </div>
              {fieldErrors.email && <p className="text-red-400 text-xs mt-1">{fieldErrors.email}</p>}
              {formData.email && !fieldErrors.email && view !== 'login' && (
                <div className="mt-1 space-y-0.5">
                  {!formData.email.trim().toLowerCase().endsWith('@gmail.com') && (
                    <p className="text-xs text-red-400 flex items-center">
                      <svg aria-hidden="true" className="Qk3oof xTjuxe mr-1" fill="currentColor" focusable="false" width="16px" height="16px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path></svg> Debe ser @gmail.com
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* Contraseña */}
            <div>
              <div className="relative">
                <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                <input
                  type={showPassword ? 'text' : 'password'} name="password" value={formData.password}
                  onChange={handleInputChange}
                  placeholder="Contraseña"
                  autoComplete="new-password"
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg pl-10 pr-12 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
                  required
                />
                <button type="button" onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300">
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
              {fieldErrors.password && <p className="text-red-400 text-xs mt-1">{fieldErrors.password}</p>}

              {/* ── Requisitos de contraseña — SOLO en registro ── */}
              {view !== 'login' && formData.password.length > 0 && (
                <div className="mt-2 space-y-1">
                  <div className="flex gap-1">
                    {[1, 2, 3, 4].map(i => (
                      <div
                        key={i}
                        className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${
                          passwordStrength >= i ? barColors[passwordStrength] : 'bg-gray-700'
                        }`}
                      />
                    ))}
                  </div>
                  <p className={`text-xs font-medium transition-colors duration-300 ${strengthColor[passwordStrength]}`}>
                    {strengthLabel[passwordStrength]}
                  </p>
                  <div className="mt-2 space-y-0.5">
                    {(() => {
                      const missing = [];
                      if (formData.password.length < 8) missing.push("8 caracteres");
                      if (!/[A-Z]/.test(formData.password)) missing.push("mayúscula");
                      if (!/[a-z]/.test(formData.password)) missing.push("minúscula");
                      if (!/\d/.test(formData.password)) missing.push("número");
                      if (!/[@$!%*?&]/.test(formData.password)) missing.push("carácter especial (@$!%*?&)");
                      
                      if (missing.length > 0) {
                        return (
                          <p className="text-xs text-red-400 flex items-center">
                            <svg aria-hidden="true" className="Qk3oof xTjuxe mr-1" fill="currentColor" focusable="false" width="16px" height="16px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path></svg>
                            Debe contener {missing.join(", ")}
                          </p>
                        );
                      }
                      return null;
                    })()}
                  </div>
                </div>
              )}
            </div>

            {/* Confirmar contraseña — solo registro */}
            {view !== 'login' && (
              <div>
                <div className="relative">
                  <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                  <input
                    type={showConfirmPassword ? 'text' : 'password'} name="confirmPassword" value={formData.confirmPassword}
                    onChange={handleInputChange}
                    placeholder="Confirmar contraseña"
                    autoComplete="new-password"
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg pl-10 pr-12 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
                    required
                  />
                  <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300">
                    {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
                {fieldErrors.confirmPassword && <p className="text-red-400 text-xs mt-1">{fieldErrors.confirmPassword}</p>}
                {formData.confirmPassword && !fieldErrors.confirmPassword && formData.confirmPassword !== formData.password && (
                  <p className="text-xs mt-1 text-red-400 flex items-center">
                    <svg aria-hidden="true" className="Qk3oof xTjuxe mr-1" fill="currentColor" focusable="false" width="16px" height="16px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path></svg> Las contraseñas no coinciden
                  </p>
                )}
              </div>
            )}

            {error && (
              <p className="text-red-400 text-sm">{error}</p>
            )}

            {/* Botón principal */}
            <button
              type="submit" disabled={loading}
              className="w-full bg-gradient-to-r from-purple-600 to-purple-700 hover:from-purple-500 hover:to-purple-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-lg transition-all duration-300 text-sm tracking-widest"
            >
              {loading ? "Cargando..." : (view === 'login' ? "INICIAR SESIÓN" : "REGISTRARSE")}
            </button>

            {/* ¿No has activado tu cuenta? — solo registro */}
            {view !== 'login' && (
              <div className="text-center">
                <p className="text-gray-500 text-xs">
                  ¿No has activado tu cuenta?{" "}
                  <button type="button" onClick={handleResendActivation} className="text-purple-400 hover:text-purple-300 font-medium">
                    Reenviar correo de activación
                  </button>
                </p>
              </div>
            )}

            {/* ¿Olvidaste tu contraseña? — solo login */}
            {view === 'login' && (
              <div className="text-center">
                <p className="text-gray-500 text-xs">
                  ¿No recuerdas tu contraseña?{" "}
                  <button type="button" onClick={() => setView('forgot')} className="text-purple-400 hover:text-purple-300 font-medium">
                    Recupérala
                  </button>
                </p>
              </div>
            )}

            {/* Separador redes sociales */}
            <div className="flex items-center gap-3">
              <div className="flex-1 h-px bg-gray-700" />
              <div className="flex-1 h-px bg-gray-700" />
            </div>

            {/* Botones Google / Facebook */}
            <div className="flex justify-center gap-4">
              <button
                type="button"
                onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/google'}
                className="flex items-center gap-3 bg-white hover:bg-gray-100 text-gray-800 px-5 py-2.5 rounded-lg transition-colors duration-200 shadow-sm"
                title="Iniciar con Google">
                <svg width="18" height="18" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                  <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
                  <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
                  <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
                  <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
                  <path fill="none" d="M0 0h48v48H0z"/>
                </svg>
                <span className="text-sm font-medium leading-none">Google</span>
              </button>

              <button
                type="button"
                onClick={() => window.location.href = 'http://localhost:8080/oauth2/authorization/facebook'}
                className="flex items-center gap-3 bg-[#1877F2] hover:bg-[#166FE5] text-white px-5 py-2.5 rounded-lg transition-colors duration-200 shadow-sm"
                title="Iniciar con Facebook"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path fill="white" d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                </svg>
                <span className="text-sm font-medium leading-none">Facebook</span>
              </button>
            </div>

            {/* ¿Tienes cuenta? + Términos */}
            <div className="text-center space-y-2 pt-1">
              <p className="text-gray-500 text-xs">
                {view === 'login' ? "¿No tienes una cuenta aún?" : "¿Ya tienes una cuenta?"}
                {" "}
                <button type="button"
                  onClick={() => { setView(view === 'login' ? 'register' : 'login'); setError(""); setFieldErrors({}); setPasswordStrength(0); }}
                  className="text-purple-400 hover:text-purple-300 font-medium"
                >
                  {view === 'login' ? "Haz clic aquí" : "Inicia sesión"}
                </button>
              </p>
              <p className="text-gray-600 text-xs">
                {view === 'login' ? "Al iniciar sesión, aceptas nuestros" : "Al registrarte, aceptas nuestros"}{" "}
                <button type="button" onClick={() => setShowTermsModal(true)}
                  className="text-purple-400 hover:text-purple-300 underline">
                  Términos y condiciones
                </button>
              </p>
            </div>

          </form>
        </div>
      </div>

      {/* ══════════════ MODAL TÉRMINOS ══════════════ */}
      {showTermsModal && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full mx-4 overflow-hidden">
            <div className="bg-gradient-to-r from-purple-500 to-purple-700 p-5 flex items-center justify-between">
              <h3 className="text-lg font-bold text-white">Términos y Condiciones de Uso</h3>
              <button onClick={() => setShowTermsModal(false)} className="text-white hover:text-purple-200 text-xl font-bold leading-none">✕</button>
            </div>
            <div className="p-6 max-h-[500px] overflow-y-auto text-sm text-gray-700 space-y-4">
              <p><strong>Última actualización:</strong> 2/10/2025</p>
              <h4 className="font-semibold">1. Aceptación de los términos</h4>
              <p>Al acceder, registrarse o utilizar la aplicación "Tu Evento" (en su versión web o Android), desarrollada por CapySoft, el usuario acepta expresamente los presentes Términos y Condiciones.</p>
              <h4 className="font-semibold">2. Definiciones</h4>
              <ul className="list-disc pl-6 space-y-1">
                <li><strong>Aplicación / Plataforma:</strong> "Tu Evento" en su versión web y móvil.</li>
                <li><strong>Usuario:</strong> Persona que accede y utiliza la aplicación.</li>
                <li><strong>Organizador:</strong> Usuario autorizado para crear y publicar eventos.</li>
                <li><strong>Asistente:</strong> Usuario que reserva o participa en eventos.</li>
                <li><strong>Administrador:</strong> Usuario con permisos especiales de gestión.</li>
              </ul>
              <h4 className="font-semibold">3. Uso de la plataforma</h4>
              <ul className="list-disc pl-6 space-y-1">
                <li>Requiere conexión estable a Internet.</li>
                <li>Los usuarios deben registrarse con datos verídicos.</li>
                <li>El sistema no gestiona pagos en línea.</li>
              </ul>
              <h4 className="font-semibold">4. Registro y cuentas</h4>
              <ul className="list-disc pl-6 space-y-1">
                <li>Cuenta con correo válido y contraseña segura.</li>
                <li>Posible registro mediante Google/Facebook (OAuth).</li>
                <li>El usuario es responsable de sus credenciales.</li>
              </ul>
              <h4 className="font-semibold">5. Reservas y tickets</h4>
              <ul className="list-disc pl-6 space-y-1">
                <li>Reserva en estado pendiente hasta validación de pago.</li>
                <li>Se genera un código QR único e intransferible al confirmar.</li>
                <li>La falsificación de QR implica denegación de acceso.</li>
              </ul>
              <h4 className="font-semibold">6. Responsabilidades del usuario</h4>
              <ul className="list-disc pl-6 space-y-1">
                <li>Uso correcto y lícito de la plataforma.</li>
                <li>No difundir información falsa u ofensiva.</li>
                <li>No vulnerar la seguridad del sistema.</li>
              </ul>
              <h4 className="font-semibold">7. Limitación de responsabilidades</h4>
              <p>CapySoft no se hace responsable de fallas de conexión, información falsa de terceros ni cancelaciones ajenas al control de la plataforma.</p>
              <h4 className="font-semibold">8. Seguridad y privacidad</h4>
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
            <div className="flex justify-end p-4 bg-gray-50">
              <button onClick={() => setShowTermsModal(false)}
                className="px-6 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm font-medium transition-colors">
                Entendido
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ══════════════ NOTIFICACIÓN REGISTRO EXITOSO ══════════════ */}
      {showSuccessNotification && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full mx-4 overflow-hidden">
            <div className="bg-gradient-to-r from-purple-500 to-purple-700 p-6 text-center">
              <div className="flex justify-center mb-4">
                <div className="bg-white rounded-full p-3"><CheckCircle className="w-8 h-8 text-purple-500" /></div>
              </div>
              <h3 className="text-xl font-bold text-white mb-1">¡Registro Exitoso!</h3>
              <p className="text-purple-100 text-sm">Tu cuenta ha sido creada correctamente</p>
            </div>
            <div className="p-6 text-center space-y-4">
              <div className="bg-gray-50 rounded-xl p-4">
                <Mail className="w-6 h-6 text-gray-500 mx-auto mb-2" />
                <p className="text-gray-700 text-sm font-medium">Revisa tu bandeja de entrada</p>
                <p className="text-gray-400 text-xs mt-1">Te enviamos un código de activación a</p>
                <p className="text-purple-600 text-sm font-semibold mt-1">{formData.email}</p>
                <p className="text-gray-400 text-xs mt-2">¿No lo encuentras? Revisa tu carpeta de spam</p>
              </div>
              <button onClick={handleContinueToVerification}
                className="w-full bg-gradient-to-r from-purple-500 to-purple-700 hover:from-purple-600 hover:to-purple-800 text-white font-semibold py-3 rounded-lg transition-all text-sm flex items-center justify-center gap-2">
                <CheckCircle className="w-4 h-4" /> Continuar a Verificación
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ══════════════ NOTIFICACIÓN LOGIN EXITOSO ══════════════ */}
      {showLoginSuccessNotification && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full mx-4 overflow-hidden">
            <div className="bg-gradient-to-r from-purple-500 to-purple-700 p-6 text-center">
              <div className="flex justify-center mb-4">
                <div className="bg-white rounded-full p-3"><CheckCircle className="w-8 h-8 text-purple-500" /></div>
              </div>
              <h3 className="text-xl font-bold text-white mb-1">¡Bienvenido de Vuelta!</h3>
              <p className="text-purple-100 text-sm">Has iniciado sesión exitosamente</p>
            </div>
            <div className="p-6 text-center space-y-4">
              <div className="bg-gray-50 rounded-xl p-4">
                <User className="w-6 h-6 text-purple-500 mx-auto mb-2" />
                <p className="text-gray-700 text-sm font-medium">Sesión iniciada</p>
                <p className="text-gray-400 text-xs mt-1">Accede a todas las funcionalidades de TuEvento</p>
                <p className="text-purple-600 text-sm font-semibold mt-2">👋 ¡Hola, {(() => {
                      if (!userData?.fullName) return userData?.alias || formData.email;
                      
                      const name = userData.fullName;
                      const parts = name.split(' ').filter(part => part.trim().length > 0);
                      
                      if (parts.length === 0) return userData?.alias || formData.email;
                      if (parts.length === 1) return parts[0];
                      
                      const firstName = parts[0];
                      const lastName = parts[1];
                      
                      // Si el nombre es corto (≤3 caracteres)
                      if (firstName.length <= 3) {
                        // Si el apellido es más largo que el nombre, mostrar apellido
                        if (lastName.length > firstName.length) {
                          return lastName;
                        }
                        // Si el apellido también es corto, mostrar solo el nombre
                        return firstName;
                      }
                      
                      // Si el nombre es largo (>3 caracteres), mostrar solo el nombre
                      return firstName;
                    })()}!</p>
              </div>
              <button onClick={handleContinueToHome}
                className="w-full bg-gradient-to-r from-purple-500 to-purple-700 hover:from-purple-600 hover:to-purple-800 text-white font-semibold py-3 rounded-lg transition-all text-sm flex items-center justify-center gap-2">
                <ArrowRight className="w-4 h-4" /> Ir a Inicio
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}