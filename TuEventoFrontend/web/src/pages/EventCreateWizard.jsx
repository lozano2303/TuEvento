import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createEvent } from '../services/EventService';
import WizardStepper     from '../components/event-wizard/WizardStepper';
import StepGeneralInfo   from '../components/event-wizard/StepGeneralInfo';
import StepSiteSelection from '../components/event-wizard/StepSiteSelection';
import BackButton        from '../components/common/BackButton';

export default function EventCreateWizard() {
  const navigate = useNavigate();

  const [step,               setStep]               = useState(1);
  const [formData,           setFormData]           = useState({
    eventName:      '',
    description:    '',
    startDate:      '',
    finishDate:     '',
    isPublic:       true,
    categoryId:     null,
    departmentId:   null,
    cityId:         null,
    siteId:         null,
    availableSeats: '',
  });
  const [selectedSite,       setSelectedSite]       = useState(null);
  const [selectedCity,       setSelectedCity]       = useState(null);
  const [selectedDepartment, setSelectedDepartment] = useState(null);
  const [isSubmitting,       setIsSubmitting]       = useState(false);
  const [submitError,        setSubmitError]        = useState(null);

  const handleChange = (patch) => setFormData(prev => ({ ...prev, ...patch }));

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setSubmitError(null);
    try {
      const payload = {
        eventName:      formData.eventName,
        description:    formData.description,
        siteId:         formData.siteId,
        startDate:      formData.startDate,
        finishDate:     formData.finishDate,
        isPublic:       formData.isPublic,
        availableSeats: Number(formData.availableSeats),
        categoryId:     formData.categoryId,
      };

      const response = await createEvent(payload);
      const eventId  = response?.data?.eventId ?? response?.data?.id;

      if (!eventId) throw new Error(response?.message ?? 'Error al crear el evento');

      navigate(`/events/${eventId}/layout`);
    } catch (err) {
      setSubmitError(err.message ?? 'Error inesperado. Inténtalo de nuevo.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4 py-12"
      style={{ background: 'var(--color-background)' }}
    >
      <div
        className="w-full max-w-lg rounded-2xl border border-surfaceAlt shadow-2xl p-8"
        style={{ background: 'var(--color-surface)' }}
      >
        {/* Encabezado con botón cancelar */}
        <div className="mb-6">
          <div className="mb-3">
            <BackButton fallback="/events/manage" label="Cancelar" />
          </div>
          <h1 className="text-xl font-bold text-textPrimary">Crear evento</h1>
          <p className="text-sm text-textMuted mt-1">
            Completa los datos para publicar tu evento en TuEvento.
          </p>
        </div>

        {/* Indicador de pasos */}
        <WizardStepper currentStep={step} />

        {/* Paso 1 */}
        {step === 1 && (
          <StepGeneralInfo
            formData={formData}
            onChange={handleChange}
            onNext={() => setStep(2)}
            onCityChange={setSelectedCity}
            onDepartmentChange={setSelectedDepartment}
          />
        )}

        {/* Paso 2 */}
        {step === 2 && (
          <StepSiteSelection
            formData={formData}
            onChange={handleChange}
            selectedSite={selectedSite}
            onSiteSelect={setSelectedSite}
            selectedCity={selectedCity}
            selectedDepartment={selectedDepartment}
            onBack={() => setStep(1)}
            onSubmit={handleSubmit}
            isSubmitting={isSubmitting}
            submitError={submitError}
          />
        )}
      </div>
    </div>
  );
}
