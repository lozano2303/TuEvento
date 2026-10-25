import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import SectionWizardModal from '../../src/components/layout-editor/SectionWizardModal';

// BaseModal renderiza en un portal: sin cleanup los modales se acumulan entre tests
afterEach(cleanup);

const TEMPLATE = { label: 'Palco Diamante', color: '#7C3AED' };

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

  it('hace las tres preguntas y explica dónde cambiar las sillas', () => {
    renderWizard();
    expect(screen.getByText('Nueva sección: Palco Diamante')).toBeInTheDocument();
    expect(screen.getByText(/Podrás cambiarlo después en el panel Propiedades/)).toBeInTheDocument();
    expect(screen.getByText(/Quedan 500 sillas disponibles/)).toBeInTheDocument();
    expect(screen.getByText('¿Qué forma quieres como base?')).toBeInTheDocument();
    expect(screen.getByText(/Todas las sub-secciones de esta sección tendrán el mismo precio/)).toBeInTheDocument();
  });

  it('envía las sillas, la forma y el precio elegidos', () => {
    const { onConfirm } = renderWizard();
    fill('¿Cuántas sillas quieres que tenga esta sección?', '120');
    fireEvent.click(screen.getByTitle('Herradura'));
    fill('¿Cuál es el precio por silla de esta sección?', '85000');
    fireEvent.click(screen.getByText('Crear sección'));
    expect(onConfirm).toHaveBeenCalledWith({ targetSeats: 120, shapeId: 'horseshoe', price: 85000 });
  });

  it('no deja crear la sección sin precio o con sillas inválidas', () => {
    const { onConfirm } = renderWizard();
    fireEvent.click(screen.getByText('Crear sección'));
    expect(onConfirm).not.toHaveBeenCalled();

    fill('¿Cuál es el precio por silla de esta sección?', '1000');
    fill('¿Cuántas sillas quieres que tenga esta sección?', '2.5');
    expect(screen.getByText(/Ingresa un número entero/)).toBeInTheDocument();
    fireEvent.click(screen.getByText('Crear sección'));
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('avisa cuando supera el aforo restante', () => {
    renderWizard();
    fill('¿Cuántas sillas quieres que tenga esta sección?', '800');
    expect(screen.getByText(/Supera el aforo restante del evento/)).toBeInTheDocument();
  });

  it('cancela sin crear nada', () => {
    const { onConfirm, onCancel } = renderWizard();
    fireEvent.click(screen.getByText('Cancelar'));
    expect(onCancel).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
