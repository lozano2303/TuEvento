import React, { useState } from "react";
import { changePassword } from "../services/Login.js";
import { Eye, EyeOff, CheckCircle, Lock } from "lucide-react";
import { PartyPopper, Sparkles } from "lucide-react";
import BaseModal from "../components/common/BaseModal.jsx";

export default function ChangePassword({ onClose }) {
  const [formData, setFormData] = useState({
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [showSuccessNotification, setShowSuccessNotification] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value,
    });
    if (fieldErrors[name]) {
      setFieldErrors({
        ...fieldErrors,
        [name]: "",
      });
    }
  };

  const validatePassword = (password) => {
    if (!password || password.trim() === "") {
      return "La contraseña es obligatoria";
    }
    if (password.length < 8) {
      return "La contraseña debe tener al menos 8 caracteres";
    }
    if (password.length > 100) {
      return "La contraseña no puede tener más de 100 caracteres";
    }
    const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!passwordPattern.test(password)) {
      return "La contraseña debe contener al menos una letra minúscula, una mayúscula, un número y un carácter especial (@$!%*?&)";
    }
    return "";
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setFieldErrors({});

    const errors = {};

    const oldError = validatePassword(formData.oldPassword);
    if (oldError) errors.oldPassword = oldError;

    const newError = validatePassword(formData.newPassword);
    if (newError) errors.newPassword = newError;

    if (formData.newPassword !== formData.confirmPassword) {
      errors.confirmPassword = "Las contraseñas no coinciden";
    }

    if (formData.oldPassword === formData.newPassword) {
      errors.newPassword = "La nueva contraseña no puede ser igual a la anterior";
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setLoading(true);
    try {
      const result = await changePassword(formData.oldPassword, formData.newPassword);
      if (result.success) {
        setShowSuccessNotification(true);
      } else {
        setError(result.message || "Error al cambiar contraseña");
      }
    } catch (err) {
      setError("Error de conexión");
    } finally {
      setLoading(false);
    }
  };

  const handleContinueAndClose = () => {
    setShowSuccessNotification(false);
    onClose();
  };

  return (
    /* Overlay del formulario — mismo backdrop estándar que .theme-overlay
       (rgba 0,0,0,0.45) para mantener consistencia con el resto de modales. */
    <div className="fixed inset-0 flex items-center justify-center z-50 bg-black/45">
      <div className="theme-modal-card rounded-lg p-6 w-full max-w-md">
        <h2 className="text-xl font-bold text-textPrimary mb-4">Cambiar Contraseña</h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <input
              type={showPassword ? 'text' : 'password'}
              name="oldPassword"
              value={formData.oldPassword}
              onChange={handleInputChange}
              className="w-full bg-surfaceAlt border border-surfaceAlt rounded-lg px-4 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all text-sm"
              placeholder="Contraseña actual"
              required
            />
            {fieldErrors.oldPassword && <p className="text-error text-xs mt-1">{fieldErrors.oldPassword}</p>}
          </div>

          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              name="newPassword"
              value={formData.newPassword}
              onChange={handleInputChange}
              className="w-full bg-surfaceAlt border border-surfaceAlt rounded-lg px-4 pr-12 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all text-sm"
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
            {fieldErrors.newPassword && <p className="text-error text-xs mt-1">{fieldErrors.newPassword}</p>}
          </div>

          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleInputChange}
              className="w-full bg-surfaceAlt border border-surfaceAlt rounded-lg px-4 pr-12 py-3 text-textPrimary placeholder-textMuted focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all text-sm"
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
            {fieldErrors.confirmPassword && <p className="text-error text-xs mt-1">{fieldErrors.confirmPassword}</p>}
          </div>

          {error && <p className="text-error text-sm">{error}</p>}

          <div className="flex space-x-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 bg-surfaceAlt hover:brightness-110 text-textPrimary py-2 px-4 rounded-lg transition-all"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 bg-primary hover:bg-primaryDark disabled:opacity-50 disabled:cursor-not-allowed text-textPrimary py-2 px-4 rounded-lg transition-colors"
            >
              {loading ? "Cambiando..." : "Cambiar"}
            </button>
          </div>
        </form>
      </div>

      {/* ── BaseModal: contraseña actualizada ───────────────────────── */}
      <BaseModal
        isOpen={showSuccessNotification}
        onClose={() => {}}
        hideOverlayClose
        variant="success"
        icon={<Lock className="w-8 h-8" />}
        title="¡Contraseña Actualizada!"
        subtitle="Tu contraseña ha sido cambiada exitosamente"
        decorIcons={
          <>
            <PartyPopper aria-hidden="true" className="absolute top-3 left-3 w-5 h-5 -rotate-12" style={{ color: 'rgba(255,255,255,0.5)' }} />
            <Sparkles    aria-hidden="true" className="absolute top-3 right-3 w-[18px] h-[18px]"  style={{ color: 'rgba(253,224,71,0.75)' }} />
            <PartyPopper aria-hidden="true" className="absolute bottom-3 right-4 w-4 h-4 rotate-12" style={{ color: 'rgba(255,255,255,0.35)' }} />
          </>
        }
        actions={[
          {
            label: 'Entendido',
            icon: <CheckCircle className="w-4 h-4" />,
            variant: 'primary',
            onClick: handleContinueAndClose,
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
            <p className="bm-label font-medium mb-1">Seguridad mejorada</p>
            <p className="bm-hint">Tu cuenta ahora está más protegida</p>
          </div>
          <div className="bm-divider" />
          <p className="text-sm text-textSecondary">
            🔐 <span className="font-medium text-textPrimary">Nueva contraseña configurada</span>
          </p>
          <p className="bm-hint">Recuerda usar esta nueva contraseña en futuros inicios de sesión</p>
        </div>
      </BaseModal>
    </div>
  );
}