import React, { useState } from "react";
import { useTranslation } from 'react-i18next';
import { forgotPassword, resetPassword } from "../services/Login.js";
import { Eye, EyeOff, CheckCircle, Lock, ArrowRight, PartyPopper, Sparkles } from "lucide-react";
import BaseModal from "../components/common/BaseModal.jsx";
import LanguageSelector from "../components/common/LanguageSelector.jsx";
import loginHero from "../assets/images/tu-evento-login-hero-left.png";

export default function ForgotPassword({ onBackToLogin }) {
  const { t } = useTranslation();
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

  const strengthLabel = ['', t('forgotPassword.passwordStrength.veryWeak'), t('forgotPassword.passwordStrength.weak'), t('forgotPassword.passwordStrength.good'), t('forgotPassword.passwordStrength.strong')];
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
        setError(result.message || t('forgotPassword.errors.sendError'));
      }
    } catch (err) {
      const errorMsg = err.message || t('forgotPassword.errors.connectionError');
      if (errorMsg === "This email is not registered in the system") {
        setError(t('login.errors.emailNotRegistered'));
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
      errors.token = t('forgotPassword.validation.codeRequired');
    }
    if (!newPassword.trim()) {
      errors.newPassword = t('forgotPassword.validation.passwordRequired');
    } else if (newPassword.length < 8) {
      errors.newPassword = t('forgotPassword.validation.passwordMin');
    } else if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])/.test(newPassword)) {
      errors.newPassword = t('forgotPassword.validation.passwordWeak');
    }
    if (newPassword !== confirmPassword) {
      errors.confirmPassword = t('forgotPassword.validation.passwordsNoMatch');
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
        setError(result.message || t('forgotPassword.errors.resetError'));
      }
    } catch (err) {
      const errorMsg = err.message || t('forgotPassword.errors.connectionError');
      if (errorMsg.includes("tamaño debe estar entre 8 y 8") || errorMsg.includes("size must be between 8 and 8")) {
        setError(t('forgotPassword.validation.codeLength'));
      } else if (errorMsg.includes("Invalid code") || errorMsg.includes("Código inválido") || errorMsg.includes("invalid code")) {
        setError(t('codeVerification.errors.invalidCode'));
      } else if (errorMsg.includes("expired") || errorMsg.includes("expirado")) {
        setError(t('codeVerification.errors.invalidCode'));
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
        <LanguageSelector position="fixed-top-right" />
        <div className="relative w-1/2 overflow-hidden">
          <img
            src={loginHero}
            alt="Tu Evento — plataforma de eventos en vivo"
            className="absolute inset-0 w-full h-full object-cover"
          />
        </div>
        <div className="w-1/2 bg-background flex items-center justify-center p-8">
          <div className="w-full max-w-sm space-y-6">
            <div className="text-center space-y-2">
              <h1 className="text-2xl font-bold text-textPrimary mb-1">{t('forgotPassword.title')}</h1>
              <p className="text-textMuted text-sm">{t('forgotPassword.enterCodeDesc')}</p>
            </div>

            <form onSubmit={handleResetSubmit} className="space-y-4">
              <div>
                <input
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  className="w-full bg-surface border border-surfaceAlt rounded-lg px-4 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-transparent transition-all text-sm"
                  placeholder={t('forgotPassword.verificationCode')}
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
                    placeholder={t('forgotPassword.newPassword')}
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
                        if (newPassword.length < 8) missing.push(`8 ${t('login.passwordStrength.characters')}`);
                        if (!/[A-Z]/.test(newPassword)) missing.push(t('login.passwordStrength.uppercase'));
                        if (!/[a-z]/.test(newPassword)) missing.push(t('login.passwordStrength.lowercase'));
                        if (!/\d/.test(newPassword)) missing.push(t('login.passwordStrength.number'));
                        if (!/[@$!%*?&]/.test(newPassword)) missing.push(t('login.passwordStrength.specialChar'));
                        
                        if (missing.length > 0) {
                          return (
                            <p className="text-xs form-error flex items-center">
                              <svg aria-hidden="true" className="Qk3oof xTjuxe mr-1" fill="currentColor" focusable="false" width="16px" height="16px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path></svg>
                              {t('login.passwordStrength.mustContain')} {missing.join(", ")}
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
                  placeholder={t('forgotPassword.confirmPassword')}
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
                {loading ? t('forgotPassword.resetting') : t('forgotPassword.resetPassword').toUpperCase()}
              </button>
            </form>

            <div className="text-center">
              <button
                type="button"
                onClick={onBackToLogin}
                className="text-accent hover:text-primary"
              >
                {t('forgotPassword.backToLogin')}
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
          title={t('forgotPassword.success.title')}
          subtitle={t('forgotPassword.success.subtitle')}
          decorIcons={
            <>
              <PartyPopper aria-hidden="true" className="absolute top-3 left-3 w-5 h-5 -rotate-12" style={{ color: 'rgba(255,255,255,0.5)' }} />
              <Sparkles    aria-hidden="true" className="absolute top-3 right-3 w-[18px] h-[18px]"  style={{ color: 'rgba(253,224,71,0.75)' }} />
              <PartyPopper aria-hidden="true" className="absolute bottom-3 right-4 w-4 h-4 rotate-12" style={{ color: 'rgba(255,255,255,0.35)' }} />
            </>
          }
          actions={[
            {
              label: t('forgotPassword.success.goToLogin'),
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
              <p className="bm-label font-medium mb-1">{t('changePassword.success.title')}</p>
              <p className="bm-hint">{t('changePassword.success.subtitle')}</p>
            </div>
            <div className="bm-divider" />
            <p className="text-sm text-textSecondary">
              🔐 <span className="font-medium text-textPrimary">{t('forgotPassword.success.message')}</span>
            </p>
            <p className="bm-hint">{t('forgotPassword.success.message')}</p>
          </div>
        </BaseModal>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex">
      <LanguageSelector position="fixed-top-right" />
      <div className="relative w-1/2 overflow-hidden">
        <img
          src={loginHero}
          alt="Tu Evento — plataforma de eventos en vivo"
          className="absolute inset-0 w-full h-full object-cover"
        />
      </div>

      <div className="w-1/2 bg-background flex items-center justify-center p-8">
        <div className="w-full max-w-sm space-y-6">
          <div className="text-center space-y-2">
            <h1 className="text-2xl font-bold text-textPrimary mb-1">{t('forgotPassword.title')}</h1>
            <p className="text-textMuted text-sm">{t('forgotPassword.enterEmailDesc')}</p>
          </div>

          <form onSubmit={handleEmailSubmit} className="space-y-4">
            <div>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-surface border border-surfaceAlt rounded-lg px-4 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-transparent transition-all text-sm"
                placeholder={t('forgotPassword.email')}
                required
              />
            </div>

            {error && <p className="form-error text-sm">{error}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-primary to-primaryDark hover:from-primaryDark hover:to-primaryDark disabled:opacity-50 disabled:cursor-not-allowed text-textPrimary font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
            >
              {loading ? t('forgotPassword.sending') : t('forgotPassword.sendCode').toUpperCase()}
            </button>
          </form>

          <div className="text-center">
            <button
              type="button"
              onClick={onBackToLogin}
              className="text-accent hover:text-primary"
            >
              {t('forgotPassword.backToLogin')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}