import React, { useState, useEffect } from "react";
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CheckCircle, Mail, ArrowRight, RefreshCw, PartyPopper, Sparkles } from "lucide-react";
import { verifyActivationCode, resendActivationCode, resendActivationCodeByEmail } from "../services/Login.js";
import LanguageSelector from "../components/common/LanguageSelector.jsx";
import loginHero from "../assets/images/tu-evento-login-hero-left.png";

export default function CodeVerification({ userID: propUserID, userEmail: propUserEmail, onVerificationSuccess, onBackToLogin }) {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showSuccessNotification, setShowSuccessNotification] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);
  const [resendMessage, setResendMessage] = useState("");
  const [userID, setUserID] = useState(propUserID || null);
  const [noUserID, setNoUserID] = useState(false);
  const [activationEmail, setActivationEmail] = useState(propUserEmail || "");

  useEffect(() => {
    if (propUserEmail) {
      setActivationEmail(propUserEmail);
      setNoUserID(false);
    } else if (!propUserID) {
      const storedUserID = localStorage.getItem('pendingActivationUserID');
      const storedEmail = localStorage.getItem('pendingActivationEmail');
      
      // Leer email de la URL
      const urlParams = new URLSearchParams(window.location.search);
      const urlEmail = urlParams.get('email');
      
      if (urlEmail) {
        setActivationEmail(urlEmail);
        localStorage.setItem('pendingActivationEmail', urlEmail);
        setNoUserID(false);
      } else if (storedEmail) {
        setActivationEmail(storedEmail);
        setNoUserID(false);
      } else if (storedUserID) {
        setUserID(parseInt(storedUserID));
      } else {
        setNoUserID(true);
      }
    }
  }, [propUserID, propUserEmail]);

  // Si no hay userID disponible, mostrar pantalla para reenviar código
  if (noUserID) {
    return (
      <div className="min-h-screen flex">
      <LanguageSelector position="fixed-top-right" />
      {/* Columna izquierda - Ilustración con gradiente púrpura */}
      <div className="theme-auth-hero w-1/2 flex items-center justify-center p-8">
        <div className="text-center space-y-6 max-w-sm">
          <Mail className="w-16 h-16 text-textPrimary mx-auto" />
          <div className="text-textPrimary">
            <h2 className="text-2xl font-bold mb-2">{t('codeVerification.title')}</h2>
            <p className="text-sm opacity-90">{t('codeVerification.enterCode')}</p>
          </div>
        </div>
      </div>

        <div className="w-1/2 bg-background flex items-center justify-center p-8">
          <div className="w-full max-w-sm space-y-6">
            <div className="text-center space-y-2">
              <h1 className="text-xl md:text-2xl font-bold text-textPrimary mb-1">{t('codeVerification.title')}</h1>
              <p className="text-textMuted text-xs md:text-sm">{t('codeVerification.enterCode')}</p>
            </div>

            <form onSubmit={async (e) => {
              e.preventDefault();
              setResendLoading(true);
              setResendMessage("");
              try {
                const result = await resendActivationCodeByEmail(activationEmail);
                if (result.success) {
                  setResendMessage(t('codeVerification.success.message'));
                } else {
                  setResendMessage(t('codeVerification.errors.resendError'));
                }
              } catch (err) {
                setResendMessage(t('codeVerification.errors.connectionError'));
              } finally {
                setResendLoading(false);
              }
            }} className="space-y-4">
              <div>
                <input
                  type="email"
                  value={activationEmail}
                  onChange={(e) => setActivationEmail(e.target.value)}
                  className="w-full bg-surface border border-surfaceAlt rounded-lg px-4 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-transparent transition-all text-sm"
                  placeholder={t('codeVerification.code')}
                  required
                />
              </div>

              {resendMessage && <p className="form-success text-sm">{resendMessage}</p>}

              <button
                type="submit"
                disabled={resendLoading}
                className="w-full bg-gradient-to-r from-primary to-primaryDark hover:from-primaryDark hover:to-primaryDark disabled:opacity-50 disabled:cursor-not-allowed text-textPrimary font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm flex items-center justify-center space-x-2"
              >
                {resendLoading ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <Mail className="w-4 h-4" />
                )}
                <span>{resendLoading ? t('codeVerification.resending') : t('codeVerification.resend')}</span>
              </button>
            </form>

            <div className="text-center">
              <button
                type="button"
                onClick={() => onBackToLogin ? onBackToLogin() : navigate('/login')}
                className="text-accent hover:text-primary"
              >
                {t('codeVerification.backToLogin')}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const result = await verifyActivationCode(activationEmail || userID, code);
      if (result.success) {
        setShowSuccessNotification(true);
      } else {
        const errorMessage = result.message || t('codeVerification.errors.invalidCode');
        setError(errorMessage);

        if (errorMessage.toLowerCase().includes('expir') ||
            errorMessage.toLowerCase().includes('inválido') ||
            errorMessage.toLowerCase().includes('invalid')) {
          setResendMessage(t('codeVerification.checkSpam'));
        }
      }
    } catch (err) {
      const errorMsg = err.message || "";
      if (errorMsg.includes("Invalid activation code") || errorMsg.includes("ACTIVATION_CODE_NOT_FOUND")) {
        setError(t('codeVerification.errors.invalidCode'));
      } else if (errorMsg.includes("expired") || errorMsg.includes("EXPIRED")) {
        setError(t('codeVerification.errors.invalidCode'));
      } else if (errorMsg.includes("USER_NOT_FOUND")) {
        setError(t('login.errors.emailNotRegistered'));
      } else if (errorMsg.toLowerCase().includes("cuenta ya está activada")) {
        setError(t('login.errors.emailAlreadyRegistered'));
      } else {
        setError(t('codeVerification.errors.verificationError'));
      }
    } finally {
      setLoading(false);
    }
  };

  const handleContinueToLogin = () => {
    setShowSuccessNotification(false);
    localStorage.removeItem('pendingActivationUserID');
    localStorage.removeItem('pendingActivationEmail');
    if (onVerificationSuccess) {
      onVerificationSuccess();
    } else {
      navigate('/login');
    }
  };

  const handleResendCode = async () => {
    const emailToUse = activationEmail || userID;
    setResendLoading(true);
    setResendMessage("");
    setError("");
    try {
      const result = await resendActivationCode(emailToUse);
      if (result.success) {
        setResendMessage(t('codeVerification.success.message'));
      } else {
        setError(t('codeVerification.errors.resendError'));
      }
    } catch (err) {
      setError(t('codeVerification.errors.connectionError'));
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      <LanguageSelector position="fixed-top-right" />
      {/* Columna izquierda — hero a pantalla completa */}
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
            <h1 className="text-xl md:text-2xl font-bold text-textPrimary mb-1">{t('codeVerification.title')}</h1>
            <p className="text-textMuted text-xs md:text-sm">{t('codeVerification.subtitle')}</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <input
                type="text"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="w-full bg-surface border border-surfaceAlt rounded-lg px-4 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-transparent transition-all text-sm"
                placeholder={t('codeVerification.code')}
                required
              />
            </div>

            {error && <p className="form-error text-sm">{error}</p>}

            {resendMessage && <p className="form-success text-sm">{resendMessage}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-primary to-primaryDark hover:from-primaryDark hover:to-primaryDark disabled:opacity-50 disabled:cursor-not-allowed text-textPrimary font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
            >
              {loading ? t('codeVerification.verifying') : t('codeVerification.verify').toUpperCase()}
            </button>

            <div className="text-center">
              <button
                type="button"
                onClick={handleResendCode}
                disabled={resendLoading}
                className="text-accent hover:text-primary disabled:opacity-50 disabled:cursor-not-allowed text-sm flex items-center justify-center space-x-1 mx-auto"
              >
                {resendLoading ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <Mail className="w-4 h-4" />
                )}
                <span>{resendLoading ? t('codeVerification.resending') : t('codeVerification.resend')}</span>
              </button>
            </div>
          </form>

          <div className="text-center">
            <button
              type="button"
              onClick={() => onBackToLogin ? onBackToLogin() : navigate('/login')}
              className="text-accent hover:text-primary"
            >
              {t('codeVerification.backToLogin')}
            </button>
          </div>
        </div>
      </div>

      {/* Notificación de activación exitosa */}
      {showSuccessNotification && (
        <div className="theme-overlay fixed inset-0 flex items-center justify-center z-50 p-4">
          <div className="theme-modal-card rounded-2xl max-w-md w-full mx-4 overflow-hidden">
            <div className="modal-header-gradient p-6 text-center">
              <PartyPopper aria-hidden="true" className="absolute top-3 left-3 w-5 h-5 -rotate-12" style={{ color: 'rgba(255,255,255,0.5)' }} />
              <Sparkles aria-hidden="true" className="absolute top-3 right-3 w-[18px] h-[18px]" style={{ color: 'rgba(253,224,71,0.75)' }} />
              <PartyPopper aria-hidden="true" className="absolute bottom-3 right-4 w-4 h-4 rotate-12" style={{ color: 'rgba(255,255,255,0.35)' }} />
              <div className="flex justify-center mb-4 relative z-10">
                <div className="modal-icon-ring">
                  <CheckCircle className="w-8 h-8" style={{ color: '#16a34a' }} />
                </div>
              </div>
              <h3 className="text-xl font-bold mb-2 relative z-10" style={{ color: 'var(--color-onPrimary, #ffffff)' }}>{t('codeVerification.success.title')}</h3>
              <p className="modal-header-gradient-sub text-sm relative z-10">{t('codeVerification.success.subtitle')}</p>
            </div>
            <div className="p-6 text-center">
              <div className="mb-6">
                <div className="theme-info-card mb-4" style={{ background: 'rgba(0,0,0,0.15)' }}>
                  <div className="flex items-center justify-center mb-2">
                    <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'color-mix(in srgb, var(--color-accent) 15%, transparent)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <CheckCircle className="w-5 h-5 text-success" />
                    </div>
                  </div>
                  <p className="theme-label text-sm font-medium mb-1">{t('codeVerification.success.subtitle')}</p>
                  <p className="theme-hint text-xs">{t('codeVerification.success.message')}</p>
                </div>
                <div className="text-textSecondary text-sm">
                  <p className="mb-2">🎉 <span className="font-medium text-textPrimary">{t('login.modals.loginSuccessSubtitle')}</span></p>
                  <p className="theme-hint text-xs">{t('codeVerification.success.message')}</p>
                </div>
              </div>
              <button onClick={handleContinueToLogin} className="modal-btn-cta">
                <ArrowRight className="w-4 h-4" />
                <span>{t('codeVerification.success.goToLogin')}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
