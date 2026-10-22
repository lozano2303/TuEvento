import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { createProfile, updateProfile } from '../services/ProfileService';
import LanguageSelector from '../components/common/LanguageSelector.jsx';

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

function validateFullName(value, t) {
  if (!value || !value.trim()) return t('completeProfile.validation.nameRequired');
  const words = value.trim().split(/\s+/);
  if (words.length < 2) return t('completeProfile.validation.nameMin');
  const shortWord = words.find(w => w.length < 3);
  if (shortWord) return t('completeProfile.validation.nameMinPerWord');
  if (!FULL_NAME_RE.test(value.trim()))
    return t('completeProfile.validation.nameLettersOnly');
  if (value.trim().length > 100)
    return t('completeProfile.validation.nameMax');
  return '';
}

export default function CompleteProfile() {
  const navigate = useNavigate();
  const { t } = useTranslation();
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
    const nameError = validateFullName(fullName, t);
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
      setError(err.message || t('completeProfile.errors.updateError'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <LanguageSelector position="fixed-top-right" />
      <div className="w-full max-w-md bg-card rounded-2xl shadow-lg p-8">
        <h1 className="text-2xl font-bold text-foreground mb-2">
          {profileId ? t('completeProfile.updateName') : t('completeProfile.welcome')}
        </h1>
        <p className="text-muted-foreground mb-6 text-sm">
          {profileId
            ? t('completeProfile.invalidName')
            : t('completeProfile.message')}
        </p>

        <form onSubmit={handleSubmit} noValidate>
          <label
            htmlFor="fullName"
            className="block text-sm font-medium text-foreground mb-1"
          >
            {t('completeProfile.fullName')}
          </label>
          <input
            id="fullName"
            type="text"
            autoComplete="name"
            placeholder={t('completeProfile.fullNamePlaceholder')}
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
            {loading ? t('completeProfile.continuing') : t('completeProfile.continue')}
          </button>
        </form>
      </div>
    </div>
  );
}
