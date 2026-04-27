import React, { useState, useEffect } from "react";
import { useNavigate } from 'react-router-dom';
import { CheckCircle, Mail, ArrowRight, RefreshCw } from "lucide-react";
import { verifyActivationCode, resendActivationCode, resendActivationCodeByEmail } from "../services/Login.js";

export default function CodeVerification({ userID: propUserID, userEmail: propUserEmail, onVerificationSuccess, onBackToLogin }) {
  const navigate = useNavigate();
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
    console.log('CodeVerification mounted');
    console.log('propUserEmail:', propUserEmail);
    console.log('propUserID:', propUserID);
    
    if (propUserEmail) {
      setActivationEmail(propUserEmail);
      setNoUserID(false);
    } else if (!propUserID) {
      const storedUserID = localStorage.getItem('pendingActivationUserID');
      const storedEmail = localStorage.getItem('pendingActivationEmail');
      
      // Leer email de la URL
      const urlParams = new URLSearchParams(window.location.search);
      const urlEmail = urlParams.get('email');
      
      console.log('URL email:', urlEmail);
      console.log('Stored email:', storedEmail);
      console.log('Stored userID:', storedUserID);
      
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
        {/* Columna izquierda - Ilustración con gradiente púrpura */}
        <div className="w-1/2 bg-gradient-to-br from-purple-600 via-purple-700 to-purple-800 flex items-center justify-center p-8">
          <div className="text-center space-y-6 max-w-sm">
            <Mail className="w-16 h-16 text-white mx-auto" />
            <div className="text-white">
              <h2 className="text-2xl font-bold mb-2">Reenviar Código</h2>
              <p className="text-sm opacity-90">Ingresa tu email para recibir un nuevo código de activación.</p>
            </div>
          </div>
        </div>

        <div className="w-1/2 bg-gray-900 flex items-center justify-center p-8">
          <div className="w-full max-w-sm space-y-6">
            <div className="text-center space-y-2">
              <h1 className="text-xl md:text-2xl font-bold text-white mb-1">Activar Cuenta</h1>
              <p className="text-gray-400 text-xs md:text-sm">Ingresa tu email para reenviar el código de activación</p>
            </div>

            <form onSubmit={async (e) => {
              e.preventDefault();
              setResendLoading(true);
              setResendMessage("");
              try {
                const result = await resendActivationCodeByEmail(activationEmail);
                if (result.success) {
                  setResendMessage("Código de activación enviado exitosamente. Revisa tu correo.");
                } else {
                  setResendMessage("Error al enviar el código. Intenta nuevamente.");
                }
              } catch (err) {
                setResendMessage("Error de conexión. Intenta nuevamente.");
              } finally {
                setResendLoading(false);
              }
            }} className="space-y-4">
              <div>
                <input
                  type="email"
                  value={activationEmail}
                  onChange={(e) => setActivationEmail(e.target.value)}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                  placeholder="Correo electrónico"
                  required
                />
              </div>

              {resendMessage && <p className="text-purple-400 text-sm">{resendMessage}</p>}

              <button
                type="submit"
                disabled={resendLoading}
                className="w-full bg-gradient-to-r from-purple-600 to-purple-700 hover:from-purple-700 hover:to-purple-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm flex items-center justify-center space-x-2"
              >
                {resendLoading ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <Mail className="w-4 h-4" />
                )}
                <span>{resendLoading ? "Enviando..." : "Enviar Código"}</span>
              </button>
            </form>

            <div className="text-center">
              <button
                type="button"
                onClick={() => onBackToLogin ? onBackToLogin() : navigate('/login')}
                className="text-purple-400 hover:text-purple-300"
              >
                Volver al inicio de sesión
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
    console.log('=== FRONTEND SUBMIT ===');
    console.log('Email:', activationEmail || userID);
    console.log('Code:', code);
    console.log('Code length:', code.length);
    try {
      const result = await verifyActivationCode(activationEmail || userID, code);
      if (result.success) {
        setShowSuccessNotification(true);
      } else {
        const errorMessage = result.message || "Código inválido";
        setError(errorMessage);

        if (errorMessage.toLowerCase().includes('expir') ||
            errorMessage.toLowerCase().includes('inválido') ||
            errorMessage.toLowerCase().includes('invalid')) {
          setResendMessage("¿Código expirado? Haz clic en 'Reenviar código' para recibir uno nuevo.");
        }
      }
    } catch (err) {
      const errorMsg = err.message || "";
      if (errorMsg.includes("Invalid activation code") || errorMsg.includes("ACTIVATION_CODE_NOT_FOUND")) {
        setError("Código de activación inválido");
      } else if (errorMsg.includes("expired") || errorMsg.includes("EXPIRED")) {
        setError("El código ha expirado. Solicita uno nuevo.");
      } else if (errorMsg.includes("USER_NOT_FOUND")) {
        setError("Usuario no encontrado");
      } else {
        setError("Error al verificar el código");
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
        setResendMessage("Código reenviado exitosamente. Revisa tu correo.");
      } else {
        setError("Error al reenviar el código");
      }
    } catch (err) {
      setError("Error de conexión al reenviar código");
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* Columna izquierda - Ilustración con gradiente púrpura */}
      <div className="w-1/2 bg-gradient-to-br from-purple-600 via-purple-700 to-purple-800 flex items-center justify-center p-8">
        <div className="text-center space-y-6 max-w-sm">
          <img
            src="/src/assets/images/fondologin.png"
            alt="Ilustración escritorio"
            className="w-full max-w-xs drop-shadow-2xl"
          />
        </div>
      </div>

      <div className="w-1/2 bg-gray-900 flex items-center justify-center p-8">
        <div className="w-full max-w-sm space-y-6">
          <div className="text-center space-y-2">
            <h1 className="text-xl md:text-2xl font-bold text-white mb-1">Verificar Cuenta</h1>
            <p className="text-gray-400 text-xs md:text-sm">Ingresa el código de activación enviado a tu correo</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <input
                type="text"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                placeholder="Código de activación"
                required
              />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            {resendMessage && <p className="text-green-500 text-sm">{resendMessage}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-purple-600 to-purple-700 hover:from-purple-700 hover:to-purple-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
            >
              {loading ? "Verificando..." : "VERIFICAR"}
            </button>

            <div className="text-center">
              <button
                type="button"
                onClick={handleResendCode}
                disabled={resendLoading}
                className="text-purple-400 hover:text-purple-300 disabled:opacity-50 disabled:cursor-not-allowed text-sm flex items-center justify-center space-x-1 mx-auto"
              >
                {resendLoading ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <Mail className="w-4 h-4" />
                )}
                <span>{resendLoading ? "Enviando..." : "Reenviar código"}</span>
              </button>
            </div>
          </form>

          <div className="text-center">
            <button
              type="button"
              onClick={() => onBackToLogin ? onBackToLogin() : navigate('/login')}
              className="text-purple-400 hover:text-purple-300"
            >
              Volver al inicio de sesión
            </button>
          </div>
        </div>
      </div>

      {/* Notificación de activación exitosa */}
      {showSuccessNotification && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full mx-4 overflow-hidden">
            {/* Header con gradiente */}
            <div className="bg-gradient-to-r from-purple-500 to-purple-600 p-6 text-center">
              <div className="flex justify-center mb-4">
                <div className="bg-white rounded-full p-3">
                  <CheckCircle className="w-8 h-8 text-purple-500" />
                </div>
              </div>
              <h3 className="text-xl font-bold text-white mb-2">¡Cuenta Activada!</h3>
              <p className="text-purple-100 text-sm">Tu cuenta ha sido verificada exitosamente</p>
            </div>

            {/* Contenido */}
            <div className="p-6 text-center">
              <div className="mb-6">
                <div className="bg-gray-50 rounded-lg p-4 mb-4">
                  <CheckCircle className="w-6 h-6 text-purple-600 mx-auto mb-2" />
                  <p className="text-gray-700 text-sm font-medium mb-1">Verificación completada</p>
                  <p className="text-gray-500 text-xs">Ya puedes acceder a todas las funcionalidades</p>
                </div>
                
                <div className="text-gray-600 text-sm">
                  <p className="mb-2">🎉 <span className="font-medium">¡Bienvenido a TuEvento!</span></p>
                  <p className="text-xs text-gray-500">Ahora puedes iniciar sesión con tu cuenta</p>
                </div>
              </div>

              {/* Botón para continuar */}
              <button
                onClick={handleContinueToLogin}
                className="w-full bg-gradient-to-r from-purple-500 to-purple-600 hover:from-purple-600 hover:to-purple-700 text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm flex items-center justify-center space-x-2"
              >
                <ArrowRight className="w-4 h-4" />
                <span>Continuar al Login</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
