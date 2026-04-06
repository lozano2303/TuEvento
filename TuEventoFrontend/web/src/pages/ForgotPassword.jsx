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
      setSuccess(true);
    } catch (err) {
      setError("Error al procesar la solicitud");
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen flex">
        <div className="w-full bg-gradient-to-br from-purple-600 via-purple-700 to-purple-800 flex items-center justify-center p-8">
          <div className="text-center space-y-6 max-w-sm">
            <img
              src="/src/assets/images/fondologin.png"
              alt="Ilustración escritorio"
              className="w-full max-w-xs mx-auto drop-shadow-2xl"
            />
          </div>
        </div>

        <div className="w-full bg-gray-900 flex items-center justify-center p-8">
          <div className="w-full max-w-sm space-y-6">
            <div className="text-center space-y-2">
              <h1 className="text-2xl font-bold text-white mb-1">Correo enviado</h1>
              <p className="text-gray-400 text-sm">Hemos enviado un enlace de recuperación a tu correo electrónico</p>
            </div>

            <button
              onClick={onBackToLogin}
              className="w-full bg-gradient-to-r from-purple-600 to-purple-700 hover:from-purple-700 hover:to-purple-800 text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
            >
              Volver al login
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex">

      <div className="w-full bg-gradient-to-br from-purple-600 via-purple-700 to-purple-800 flex items-center justify-center p-8">
        <div className="text-center space-y-6 max-w-sm">

          <img
            src="/src/assets/images/fondologin.png"
            alt="Ilustración escritorio"
            className="w-full max-w-xs mx-auto drop-shadow-2xl"
          />
        </div>
      </div>


      <div className="w-full bg-gray-900 flex items-center justify-center p-8">
        <div className="w-full max-w-sm space-y-6">

          <div className="text-center space-y-2">
            <h1 className="text-2xl font-bold text-white mb-1">Recuperar contraseña</h1>
            <p className="text-gray-400 text-sm">Ingresa tu correo electrónico</p>
          </div>


          <form onSubmit={handleSubmit} className="space-y-5">

            <div>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Correo electronico"
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all text-sm"
                required
              />
            </div>

            {error && <p className="text-red-500 text-sm">{error}</p>}


            <button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-purple-600 to-purple-700 hover:from-purple-700 hover:to-purple-800 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm"
            >
              {loading ? "Enviando..." : "Enviar enlace de recuperación"}
            </button>


            <button
              type="button"
              onClick={onBackToLogin}
              className="w-full bg-gray-800 hover:bg-gray-700 border border-gray-700 text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 text-sm flex items-center justify-center gap-2"
            >
              <ArrowLeft className="w-4 h-4" />
              Volver al login
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
