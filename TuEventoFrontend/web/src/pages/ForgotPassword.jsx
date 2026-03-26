import React, { useState } from "react";
import { Mail, ArrowLeft } from "lucide-react";

export default function ForgotPassword({ onBackToLogin }) {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email) {
      setError("Por favor ingresa tu correo electrónico");
      return;
    }
    setLoading(true);
    setError("");
    
    try {
      // Aquí iría la lógica de recuperación de contraseña con el backend
      // Por ahora simulamos éxito
      setSuccess(true);
    } catch (err) {
      setError("Error al procesar la solicitud");
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full mx-4 p-8 bg-white rounded-2xl shadow-xl">
          <div className="text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Mail className="w-8 h-8 text-green-600" />
            </div>
            <h2 className="text-2xl font-bold text-gray-800">Correo enviado</h2>
            <p className="text-gray-600 mt-2">
              Hemos enviado un enlace de recuperación a tu correo electrónico
            </p>
            <button
              onClick={onBackToLogin}
              className="mt-6 w-full py-3 px-4 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
            >
              Volver al login
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full mx-4 p-8 bg-white rounded-2xl shadow-xl">
        <div className="text-center mb-8">
          <h2 className="text-2xl font-bold text-gray-800">Recuperar contraseña</h2>
          <p className="text-gray-600 mt-2">Ingresa tu correo electrónico</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="correo@ejemplo.com"
                className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              />
            </div>
          </div>

          {error && (
            <p className="text-red-500 text-sm text-center">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 px-4 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50"
          >
            {loading ? "Enviando..." : "Enviar enlace de recuperación"}
          </button>
        </form>

        <button
          onClick={onBackToLogin}
          className="w-full mt-4 py-2 flex items-center justify-center gap-2 text-gray-600 hover:text-gray-800"
        >
          <ArrowLeft className="w-4 h-4" />
          Volver al login
        </button>
      </div>
    </div>
  );
}
