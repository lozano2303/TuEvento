import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import SectionWizardModal, { priceErrorFor, seatsErrorFor } from '../../src/components/layout-editor/SectionWizardModal';

// BaseModal renderiza en un portal: sin cleanup los modales se acumulan entre tests
afterEach(cleanup);

const TEMPLATE = { label: 'Palco Diamante', color: '#7C3AED' };
const SEATS_LABEL = '¿Cuántas sillas quieres que tenga esta sección?';
const PRICE_LABEL = '¿Cuál es el precio por silla de esta sección?';

const renderWizard = (props = {}) => {
  const onConfirm = vi.fn();
  const onCancel  = vi.fn();
  render(<SectionWizardModal template={TEMPLATE} remainingSeats={500}
    onConfirm={onConfirm} onCancel={onCancel} {...props} />);
  return { onConfirm, onCancel };
};

const fill = (label, value) => fireEvent.change(screen.getByLabelText(label), { target: { value } });

describe('SectionWizardModal', () => {
  it('no se muestra sin un tipo de sección', () => {
    render(<SectionWizardModal template={null} remainingSeats={null} onConfirm={vi.fn()} onCancel={vi.fn()} />);
    expect(screen.queryByText(/Nueva sección/)).not.toBeInTheDocument();
  });

  it('hace las preguntas y explica dónde cambiar las sillas', () => {
    renderWizard();
    expect(screen.getByText('Nueva sección: Palco Diamante')).toBeInTheDocument();
    expect(screen.getByText(/Podrás cambiarlo después en el panel Propiedades/)).toBeInTheDocument();
    expect(screen.getByText(/Quedan 500 sillas disponibles/)).toBeInTheDocument();
    expect(screen.getByText('¿Qué forma quieres como base?')).toBeInTheDocument();
    expect(screen.getByText('¿De qué tamaño son las sillas en el mapa?')).toBeInTheDocument();
    expect(screen.getByText(/Todas las sub-secciones de esta sección tendrán el mismo precio/)).toBeInTheDocument();
  });

  it('envía las sillas, la forma, el tamaño y el precio elegidos', () => {
    const { onConfirm } = renderWizard();
    fill(SEATS_LABEL, '120');
    fireEvent.click(screen.getByTitle('Hexágono'));
    fireEvent.click(screen.getByRole('radio', { name: 'Mini' }));
    fill(PRICE_LABEL, '85000');
    fireEvent.click(screen.getByText('Crear sección'));
    expect(onConfirm).toHaveBeenCalledWith({ targetSeats: 120, shapeId: 'hexagon', seatSize: 'mini', price: 85000 });
  });

  it('al intentar crear sin precio muestra por qué no se puede', () => {
    const { onConfirm } = renderWizard();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    fireEvent.click(screen.getByText('Crear sección'));

    expect(onConfirm).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent('Revisa los campos marcados');
    expect(screen.getByText(/Ingresa el precio por silla/)).toBeInTheDocument();
    expect(screen.getByLabelText(PRICE_LABEL)).toHaveAttribute('aria-invalid', 'true');
  });

  it('muestra el error de un campo al salir de él', () => {
    renderWizard();
    fill(SEATS_LABEL, '2.5');
    expect(screen.queryByText(/número entero/)).not.toBeInTheDocument();
    fireEvent.blur(screen.getByLabelText(SEATS_LABEL));
    expect(screen.getByText('La cantidad de sillas debe ser un número entero')).toBeInTheDocument();
  });

  it('no deja escribir más de 200.000 sillas', () => {
    renderWizard();
    fill(SEATS_LABEL, '999999');
    expect(screen.getByLabelText(SEATS_LABEL)).toHaveValue(200000);
  });

  it('sugiere sillas más pequeñas cuando son muchas, salvo que se elija un tamaño', () => {
    renderWizard({ remainingSeats: null });
    fill(SEATS_LABEL, '5000');
    expect(screen.getByRole('radio', { name: 'Compacta' })).toHaveAttribute('aria-checked', 'true');
    fill(SEATS_LABEL, '50000');
    expect(screen.getByRole('radio', { name: 'Mini' })).toHaveAttribute('aria-checked', 'true');

    fireEvent.click(screen.getByRole('radio', { name: 'Normal' }));
    fill(SEATS_LABEL, '60000');
    expect(screen.getByRole('radio', { name: 'Normal' })).toHaveAttribute('aria-checked', 'true');
  });

  it('avisa cuando supera el aforo restante', () => {
    renderWizard();
    fill(SEATS_LABEL, '800');
    expect(screen.getByText(/Supera el aforo restante del evento/)).toBeInTheDocument();
  });

  it('cancela sin crear nada', () => {
    const { onConfirm, onCancel } = renderWizard();
    fireEvent.click(screen.getByText('Cancelar'));
    expect(onCancel).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });
});

describe('validaciones del asistente', () => {
  it.each([
    ['', 'Ingresa cuántas sillas tendrá la sección'],
    ['abc', 'La cantidad de sillas debe ser un número entero'],
    ['0', 'La sección debe tener al menos 1 silla'],
    ['200001', 'Una sección puede tener como máximo 200.000 sillas'],
    ['200000', null],
  ])('sillas %j → %s', (raw, expected) => {
    expect(seatsErrorFor(raw)).toBe(expected);
  });

  it.each([
    ['', 'Ingresa el precio por silla (puede ser 0 si es gratis)'],
    ['-1', 'El precio no puede ser negativo'],
    ['x', 'El precio debe ser un número'],
    ['0', null],
  ])('precio %j → %s', (raw, expected) => {
    expect(priceErrorFor(raw)).toBe(expected);
  });
});
