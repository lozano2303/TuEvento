import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import CourtesySeatsModal from '../../src/components/layout-editor/CourtesySeatsModal';

// BaseModal renderiza en un portal: sin cleanup los modales se acumulan entre tests
afterEach(cleanup);

const GROUPS = [
  { eventSectionId: 'sec-a', label: 'Palco Oro', color: '#CA8A04', price: 100000, totalSeats: 100, courtesySeats: 0 },
  { eventSectionId: 'sec-b', label: 'General',   color: '#16A34A', price: 30000,  totalSeats: 200, courtesySeats: 10 },
];

const renderModal = () => {
  const onApply = vi.fn();
  render(<CourtesySeatsModal isOpen groups={GROUPS} onClose={vi.fn()} onApply={onApply} />);
  return { onApply };
};

const digits = (text) => text.replace(/\D/g, '');

describe('CourtesySeatsModal', () => {
  it('pregunta cuántas cortesías tendrá cada sección', () => {
    renderModal();
    expect(screen.getByText('¿Cuántas sillas de cortesía agregarás en cada sección?')).toBeInTheDocument();
    expect(screen.getByLabelText('Cortesías de Palco Oro')).toHaveValue(0);
    expect(screen.getByLabelText('Cortesías de General')).toHaveValue(10);
  });

  it('recalcula la ganancia estimada al cambiar las cortesías', () => {
    renderModal();
    const estimate = () => digits(screen.getByText('Ganancia estimada').nextSibling.textContent);
    // 100.000 × 100 + 30.000 × 190
    expect(estimate()).toBe('15700000');
    fireEvent.change(screen.getByLabelText('Cortesías de Palco Oro'), { target: { value: '20' } });
    expect(estimate()).toBe('13700000');
    expect(screen.getByText(/30 cortesías/)).toBeInTheDocument();
  });

  it('aplica las cortesías de todas las secciones', () => {
    const { onApply } = renderModal();
    fireEvent.change(screen.getByLabelText('Cortesías de Palco Oro'), { target: { value: '4' } });
    fireEvent.click(screen.getByText('Aplicar'));
    expect(onApply).toHaveBeenCalledWith({ 'sec-a': 4, 'sec-b': 10 });
  });

  it('no deja aplicar más cortesías que sillas', () => {
    const { onApply } = renderModal();
    fireEvent.change(screen.getByLabelText('Cortesías de General'), { target: { value: '201' } });
    expect(screen.getByText('Entre 0 y 200')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Aplicar'));
    expect(onApply).not.toHaveBeenCalled();
  });
});
