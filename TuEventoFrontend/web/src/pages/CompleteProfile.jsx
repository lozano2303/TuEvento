import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createProfile, updateProfile } from '../services/ProfileService';

/**
 * Onboarding page shown after a Google OAuth login when:
 *   (a) New user — Google did not return a usable display name, so no profile
 *       row was created during registration.  Action: POST /profiles.
 *   (b) Existing user — their profile was created before the fix and stores an
 *       invalid name (e.g. "crislozanoshark2006").  Action: PUT /profiles/{id}.
 *
 * The backend signals both cases with needsOnboarding=true in LoginResponse.
 * When a profile already exists the backend also returns profileId so this
 * page knows which path to take.
 */

// Same character set as the backend ValidationUtils.FULL_NAME_PATTERN.
// Rules mirror login.jsx validateName: ≥2 words, ≥3 letters each, letters only.
const FULL_NAME_RE =
  /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]{3,}(\s[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]{3,})+$/;

function validateFullName(value) {
  if (!value || !value.trim()) return 'El nombre completo es obligatorio.';
  const words = value.trim().split(/\s+/);
  if (words.length < 2) return 'Ingresa nombre y apellido.';
  const shortWord = words.find(w => w.length < 3);
  if (shortWord) return 'Cada nombre y apellido debe tener al menos 3 caracteres.';
  if (!FULL_NAME_RE.test(value.trim()))
    return 'Solo letras y espacios — sin números ni símbolos.';
  if (value.trim().length > 100)
    return 'El nombre no puede superar los 100 caracteres.';
  return '';
}

export default function CompleteProfile() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);

  const userId    = localStorage.getItem('userID');
  // profileId is present when the user already has a profile row that needs
  // to be updated (existing user with invalid name).  Absent for new users.
  const profileId = localStorage.getItem('profileId');

  // Guard: no token means the user isn't logged in at all.
  if (!localStorage.getItem('token')) {
    navigate('/login', { replace: true });
    return null;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    const nameError = validateFullName(fullName);
    if (nameError) { setError(nameError); return; }

    setError('');
    setLoading(true);
    try {
      if (profileId) {
        // Existing profile with an invalid name — update it.
        await updateProfile(Number(profileId), { fullName: fullName.trim() });
      } else {
        // New user — no profile row exists yet — create it.
        await createProfile({ userId: Number(userId), fullName: fullName.trim() });
      }

      localStorage.setItem('name', fullName.trim());
      // Clean up the temporary onboarding hint — no longer needed.
      localStorage.removeItem('profileId');
      navigate('/', { replace: true });
    } catch (err) {
      setError(err.message || 'No se pudo guardar el nombre. Intenta de nuevo.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="w-full max-w-md bg-card rounded-2xl shadow-lg p-8">
        <h1 className="text-2xl font-bold text-foreground mb-2">
          {profileId ? 'Actualiza tu nombre' : '¡Bienvenido/a!'}
        </h1>
        <p className="text-muted-foreground mb-6 text-sm">
          {profileId
            ? 'Tu nombre actual no tiene el formato correcto. Ingresa tu nombre completo real para continuar.'
            : 'Para completar tu registro, ingresa tu nombre completo tal como quieres que aparezca en la plataforma.'}
        </p>

        <form onSubmit={handleSubmit} noValidate>
          <label
            htmlFor="fullName"
            className="block text-sm font-medium text-foreground mb-1"
          >
            Nombre completo
          </label>
          <input
            id="fullName"
            type="text"
            autoComplete="name"
            placeholder="Ej. María García"
            value={fullName}
            onChange={(e) => { setFullName(e.target.value); setError(''); }}
            className={`w-full px-4 py-2 rounded-lg border text-foreground bg-background
              focus:outline-none focus:ring-2 focus:ring-primary transition
              ${error ? 'border-red-500' : 'border-border'}`}
            disabled={loading}
            aria-describedby={error ? 'name-error' : undefined}
            aria-invalid={!!error}
          />
          {error && (
            <p id="name-error" role="alert" className="text-red-500 text-xs mt-1">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="mt-6 w-full py-2 px-4 rounded-lg bg-primary text-primary-foreground
              font-semibold hover:opacity-90 transition disabled:opacity-50"
          >
            {loading ? 'Guardando…' : 'Continuar'}
          </button>
        </form>
      </div>
    </div>
  );
}
