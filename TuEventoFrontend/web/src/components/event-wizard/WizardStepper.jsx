export default function WizardStepper({ currentStep }) {
  const steps = [
    { number: 1, label: 'Información general' },
    { number: 2, label: 'Sede' },
  ];

  return (
    <div className="flex items-center gap-0 mb-8">
      {steps.map((step, idx) => {
        const isActive    = step.number === currentStep;
        const isCompleted = step.number < currentStep;

        return (
          <div key={step.number} className="flex items-center">
            {/* Círculo + label */}
            <div className="flex flex-col items-center gap-1.5">
              <div
                className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-all"
                style={{
                  background: isActive
                    ? 'var(--color-accent)'
                    : isCompleted
                    ? 'var(--color-success, #16A34A)'
                    : 'var(--color-surfaceAlt)',
                  color: isActive || isCompleted ? '#fff' : 'var(--color-textMuted)',
                  boxShadow: isActive ? '0 0 12px rgba(124,58,237,0.45)' : 'none',
                }}
              >
                {isCompleted ? '✓' : step.number}
              </div>
              <span
                className="text-[11px] font-medium whitespace-nowrap"
                style={{ color: isActive ? 'var(--color-accent)' : 'var(--color-textMuted)' }}
              >
                {step.label}
              </span>
            </div>

            {/* Línea conectora */}
            {idx < steps.length - 1 && (
              <div
                className="h-0.5 w-16 mx-2 mt-[-14px] transition-all"
                style={{
                  background: isCompleted
                    ? 'var(--color-success, #16A34A)'
                    : 'var(--color-surfaceAlt)',
                }}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}
