import React, { useState } from "react";
import { forgotPassword, resetPassword } from "../services/Login.js";
import { Eye, EyeOff, CheckCircle, Lock, ArrowRight } from "lucide-react";

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
  const strengthColor = ['', 'text-red-400', 'text-yellow-400', 'text-blue-400', 'text-green-400'];
  const barColors = ['', 'bg-red-500', 'bg-yellow-500', 'bg-blue-500', 'bg-green-500'];

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
      setError("Error de conexión");
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
              <h1 className="text-2xl font-bold text-white mb-1">Restablecer contraseña</h1>
              <p className="text-gray-400 text-sm">Ingresa el código recibido y tu nueva contraseña.</p>
            </div>

            <form onSubmit={handleResetSubmit} className="space-y-4">
              <div>
                <input
                  type="text"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                  placeholder="Código de recuperación"
                  required
                />
                {fieldErrors.token && <p className="text-red-500 text-xs mt-1">{fieldErrors.token}</p>}
              </div>

              <div>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => handlePasswordChange(e.target.value)}
                    className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 pr-12 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                    placeholder="Nueva contraseña"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {fieldErrors.newPassword && <p className="text-red-500 text-xs mt-1">{fieldErrors.newPassword}</p>}
                
                {newPassword.length > 0 && (
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
                  </div>
                )}
              </div>

              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 pr-12 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                  placeholder="Confirmar nueva contraseña"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
                {fieldErrors.confirmPassword && <p className="text-red-500 text-xs mt-1">{fieldErrors.confirmPassword}</p>}
              </div>

              {error && <p className="text-red-500 text-sm">{error}</p>}

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-gradient-to-r from-purple-600 to-purple-700 hover:from-purple-700 hover:to-purple-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
              >
                {loading ? "Restableciendo..." : "RESTABLECER"}
              </button>
            </form>

            <div className="text-center">
              <button
                type="button"
                onClick={onBackToLogin}
                className="text-purple-400 hover:text-purple-300"
              >
                Volver al inicio de sesión
              </button>
            </div>
          </div>
        </div>

        {/* Notificación de contraseña restablecida exitosamente */}
        {showSuccessNotification && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full mx-4 overflow-hidden">
              {/* Header con gradiente morado */}
              <div className="bg-gradient-to-r from-purple-500 to-purple-600 p-6 text-center">
                <div className="flex justify-center mb-4">
                  <div className="bg-white rounded-full p-3">
                    <CheckCircle className="w-8 h-8 text-purple-500" />
                  </div>
                </div>
                <h3 className="text-xl font-bold text-white mb-2">¡Bienvenido de Vuelta!</h3>
                <p className="text-purple-100 text-sm">Has iniciado sesión exitosamente</p>
              </div>

              {/* Contenido */}
              <div className="p-6 text-center">
                <div className="mb-6">
                  <div className="bg-gray-50 rounded-lg p-4 mb-4">
                    <Lock className="w-6 h-6 text-purple-600 mx-auto mb-2" />
                    <p className="text-gray-700 text-sm font-medium mb-1">Nueva contraseña configurada</p>
                    <p className="text-gray-500 text-xs">Tu cuenta está ahora más segura</p>
                  </div>

                  <div className="text-gray-600 text-sm">
                    <p className="mb-2">🔐 <span className="font-medium">Contraseña actualizada correctamente</span></p>
                    <p className="text-xs text-gray-500">Ya puedes iniciar sesión con tu nueva contraseña</p>
                  </div>
                </div>

                {/* Botón para continuar */}
                <button
                  onClick={handleContinueToLogin}
                  className="w-full bg-gradient-to-r from-purple-500 to-purple-600 hover:from-purple-600 hover:to-purple-700 text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm flex items-center justify-center space-x-2"
                >
                  <ArrowRight className="w-4 h-4" />
                  <span>Ir al Login</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="min-h-screen flex">
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
            <h1 className="text-2xl font-bold text-white mb-1">Olvidé mi contraseña</h1>
            <p className="text-gray-400 text-sm">Ingresa tu correo electrónico para recibir instrucciones.</p>
          </div>

          <form onSubmit={handleEmailSubmit} className="space-y-4">
            <div>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                placeholder="Correo electrónico"
                required
              />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-purple-600 to-purple-700 hover:from-purple-700 hover:to-purple-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
            >
              {loading ? "Enviando..." : "ENVIAR"}
            </button>
          </form>

          <div className="text-center">
            <button
              type="button"
              onClick={onBackToLogin}
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