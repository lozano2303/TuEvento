import React, { useState } from "react";

export default function CodeVerification({ userID, onVerificationSuccess, onBackToLogin }) {
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleVerify = async (e) => {
    e.preventDefault();
    if (!code || code.length < 6) {
      setError("Por favor ingresa el código completo");
      return;
    }
    setLoading(true);
    setError("");

    try {
      onVerificationSuccess();
    } catch (err) {
      setError("Código inválido o expirado");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full mx-4 p-8 bg-white rounded-2xl shadow-xl">
        <div className="text-center mb-8">
          <h2 className="text-2xl font-bold text-gray-800">Verificación</h2>
          <p className="text-gray-600 mt-2">Ingresa el código enviado a tu correo</p>
        </div>

        <form onSubmit={handleVerify} className="space-y-6">
          <div>
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="Código de 6 dígitos"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent text-center text-lg tracking-widest"
              maxLength={6}
            />
          </div>

          {error && (
            <p className="text-red-500 text-sm text-center">{error}</p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 px-4 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50"
          >
            {loading ? "Verificando..." : "Verificar"}
          </button>
        </form>

        <button
          onClick={onBackToLogin}
          className="w-full mt-4 py-2 text-gray-600 hover:text-gray-800"
        >
          Volver al login
        </button>
      </div>
    </div>
  );
}
