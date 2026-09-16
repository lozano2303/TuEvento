import React, { useState } from "react";
import { forgotPassword, resetPassword } from "../services/Login.js";
import { Eye, EyeOff, CheckCircle, Lock, ArrowRight, PartyPopper, Sparkles } from "lucide-react";
import BaseModal from "../components/common/BaseModal.jsx";

export default function ForgotPassword({ onBackToLogin }) {
  const [step, setStep] = useState('email'); // 'email' or 'reset'
  const [email, setEmail] = useState("");
  const [token, setToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [showSuccessNotification, setShowSuccessNotification] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);

  const calcStrength = (pw) => {
    let score = 0;
    if (pw.length >= 8) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/\d/.test(pw)) score++;
    if (/[@$!%*?&]/.test(pw)) score++;
    return score;
  };

  const strengthLabel = ['', 'Muy débil', 'Débil', 'Buena', 'Fuerte'];
  const strengthColor = ['', 'strength-text-1', 'strength-text-2', 'strength-text-3', 'strength-text-4'];
  const barColors = ['', 'strength-bar-1', 'strength-bar-2', 'strength-bar-3', 'strength-bar-4'];

  const handlePasswordChange = (value) => {
    setNewPassword(value);
    setPasswordStrength(calcStrength(value));
  };

  const handleEmailSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const result = await forgotPassword(email);
      if (result.success) {
        setStep('reset');
      } else {
        setError(result.message || "Error al enviar el correo");
      }
    } catch (err) {
      const errorMsg = err.message || "Error de conexión";
      // Traducir mensajes del backend
      if (errorMsg === "This email is not registered in the system") {
        setError("Este correo no está registrado en el sistema");
      } else {
        setError(errorMsg);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResetSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setFieldErrors({});

    const errors = {};
    if (!token.trim()) {
      errors.token = "El código es obligatorio";
    }
    if (!newPassword.trim()) {
      errors.newPassword = "La nueva contraseña es obligatoria";
    } else if (newPassword.length < 8) {
      errors.newPassword = "La contraseña debe tener al menos 8 caracteres";
    } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/.test(newPassword)) {
      errors.newPassword = "Debe contener minúscula, mayúscula, número y carácter especial (@$!%*?&)";
    }
    if (newPassword !== confirmPassword) {
      errors.confirmPassword = "Las contraseñas no coinciden";
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setLoading(true);
    try {
      const result = await resetPassword(token, newPassword, email);
      if (result.success) {
        setShowSuccessNotification(true);
      } else {
        setError(result.message || "Error al restablecer contraseña");
      }
    } catch (err) {
      const errorMsg = err.message || "Error de conexión";
      // Traducir mensajes del backend
      if (errorMsg.includes("tamaño debe estar entre 8 y 8") || errorMsg.includes("size must be between 8 and 8")) {
        setError("El código de recuperación debe tener 8 caracteres");
      } else if (errorMsg.includes("Invalid code") || errorMsg.includes("Código inválido") || errorMsg.includes("invalid code")) {
        setError("El código de recuperación es incorrecto");
      } else if (errorMsg.includes("expired") || errorMsg.includes("expirado")) {
        setError("El código de recuperación ha expirado. Solicita uno nuevo");
      } else {
        setError(errorMsg);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleContinueToLogin = () => {
    setShowSuccessNotification(false);
    onBackToLogin();
  };

  if (step === 'reset') {
    return (
      <div className="min-h-screen flex">
        <div className="theme-auth-hero w-1/2 flex items-center justify-center p-8">
          <div className="text-center space-y-6 max-w-sm">
            <img
              src="/src/assets/images/fondologin.png"
              alt="Ilustración escritorio"
              className="w-full max-w-xs drop-shadow-2xl"
            />
          </div>
        </div>

        <div className="w-1/2 bg-background flex items-center justify-center p-8">
          <div className="w-full max-w-sm space-y-6">
            <div className="text-center space-y-2">
              <h1 className="text-2xl font-bold text-textPrimary mb-1">Restablecer contraseña</h1>
              <p className="text-textMuted text-sm">Ingresa el código recibido y tu nueva contraseña.</p>
            </div>

            <form onSubmit={handleResetSubmit} className="space-y-4">
              <div>
                <input
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  className="w-full bg-surface border border-surfaceAlt rounded-lg px-4 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-transparent transition-all text-sm"
                  placeholder="Código de recuperación"
                  required
                />
                {fieldErrors.token && <p className="form-error text-xs mt-1">{fieldErrors.token}</p>}
              </div>

              <div>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => handlePasswordChange(e.target.value)}
                    className="w-full bg-surface border border-surfaceAlt rounded-lg px-4 pr-12 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-transparent transition-all text-sm"
                    placeholder="Nueva contraseña"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-textMuted hover:text-textSecondary"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {fieldErrors.newPassword && <p className="form-error text-xs mt-1">{fieldErrors.newPassword}</p>}
                
                {newPassword.length > 0 && (
                  <div className="mt-2 space-y-1">
                    <div className="flex gap-1">
                      {[1, 2, 3, 4].map(i => (
                        <div
                          key={i}
                          className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${
                            passwordStrength >= i ? barColors[passwordStrength] : 'bg-surfaceAlt'
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
                        if (newPassword.length < 8) missing.push("8 caracteres");
                        if (!/[A-Z]/.test(newPassword)) missing.push("mayúscula");
                        if (!/[a-z]/.test(newPassword)) missing.push("minúscula");
                        if (!/\d/.test(newPassword)) missing.push("número");
                        if (!/[@$!%*?&]/.test(newPassword)) missing.push("carácter especial (@$!%*?&)");
                        
                        if (missing.length > 0) {
                          return (
                            <p className="text-xs form-error flex items-center">
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

              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full bg-surface border border-surfaceAlt rounded-lg px-4 pr-12 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-transparent transition-all text-sm"
                  placeholder="Confirmar nueva contraseña"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-textMuted hover:text-textSecondary"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
                {fieldErrors.confirmPassword && <p className="form-error text-xs mt-1">{fieldErrors.confirmPassword}</p>}
              </div>

              {error && <p className="form-error text-sm">{error}</p>}

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-gradient-to-r from-primary to-primaryDark hover:from-primaryDark hover:to-primaryDark disabled:opacity-50 disabled:cursor-not-allowed text-textPrimary font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
              >
                {loading ? "Restableciendo..." : "RESTABLECER"}
              </button>
            </form>

            <div className="text-center">
              <button
                type="button"
                onClick={onBackToLogin}
                className="text-accent hover:text-primary"
              >
                Volver al inicio de sesión
              </button>
            </div>
          </div>
        </div>

        {/* ── BaseModal: contraseña restablecida ──────────────────── */}
        <BaseModal
          isOpen={showSuccessNotification}
          onClose={() => {}}
          hideOverlayClose
          variant="success"
          icon={<CheckCircle className="w-8 h-8" />}
          title="¡Contraseña Restablecida!"
          subtitle="Tu contraseña ha sido actualizada exitosamente"
          decorIcons={
            <>
              <PartyPopper aria-hidden="true" className="absolute top-3 left-3 w-5 h-5 -rotate-12" style={{ color: 'rgba(255,255,255,0.5)' }} />
              <Sparkles    aria-hidden="true" className="absolute top-3 right-3 w-[18px] h-[18px]"  style={{ color: 'rgba(253,224,71,0.75)' }} />
              <PartyPopper aria-hidden="true" className="absolute bottom-3 right-4 w-4 h-4 rotate-12" style={{ color: 'rgba(255,255,255,0.35)' }} />
            </>
          }
          actions={[
            {
              label: 'Ir al Login',
              icon: <ArrowRight className="w-4 h-4" />,
              variant: 'primary',
              onClick: handleContinueToLogin,
            },
          ]}
        >
          <div className="space-y-3 text-center">
            <div className="bm-info-card">
              <div className="flex items-center justify-center mb-2">
                <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'color-mix(in srgb, var(--color-accent) 15%, transparent)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Lock className="w-5 h-5 text-accent" />
                </div>
              </div>
              <p className="bm-label font-medium mb-1">Nueva contraseña configurada</p>
              <p className="bm-hint">Tu cuenta está ahora más segura</p>
            </div>
            <div className="bm-divider" />
            <p className="text-sm text-textSecondary">
              🔐 <span className="font-medium text-textPrimary">Contraseña actualizada correctamente</span>
            </p>
            <p className="bm-hint">Ya puedes iniciar sesión con tu nueva contraseña</p>
          </div>
        </BaseModal>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex">
      <div className="theme-auth-hero w-1/2 flex items-center justify-center p-8">
        <div className="text-center space-y-6 max-w-sm">
          <img
            src="/src/assets/images/fondologin.png"
            alt="Ilustración escritorio"
            className="w-full max-w-xs drop-shadow-2xl"
          />
        </div>
      </div>

      <div className="w-1/2 bg-background flex items-center justify-center p-8">
        <div className="w-full max-w-sm space-y-6">
          <div className="text-center space-y-2">
            <h1 className="text-2xl font-bold text-textPrimary mb-1">Olvidé mi contraseña</h1>
            <p className="text-textMuted text-sm">Ingresa tu correo electrónico para recibir instrucciones.</p>
          </div>

          <form onSubmit={handleEmailSubmit} className="space-y-4">
            <div>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-surface border border-surfaceAlt rounded-lg px-4 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-transparent transition-all text-sm"
                placeholder="Correo electrónico"
                required
              />
            </div>

            {error && <p className="form-error text-sm">{error}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-primary to-primaryDark hover:from-primaryDark hover:to-primaryDark disabled:opacity-50 disabled:cursor-not-allowed text-textPrimary font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
            >
              {loading ? "Enviando..." : "ENVIAR"}
            </button>
          </form>

          <div className="text-center">
            <button
              type="button"
              onClick={onBackToLogin}
              className="text-accent hover:text-primary"
            >
              Volver al inicio de sesión
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}