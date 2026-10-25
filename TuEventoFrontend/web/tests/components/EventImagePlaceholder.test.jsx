import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import EventImagePlaceholder from '../../src/components/common/EventImagePlaceholder';

describe('EventImagePlaceholder', () => {
  it('muestra el texto por defecto según el tamaño', () => {
    render(<EventImagePlaceholder size="lg" />);
    expect(screen.getByText('Sin imagen de portada')).toBeInTheDocument();
  });

  it('usa la etiqueta recibida', () => {
    render(<EventImagePlaceholder label="Pendiente" />);
    expect(screen.getByText('Pendiente')).toBeInTheDocument();
  });

  it('en tamaño pequeño solo muestra el ícono', () => {
    render(<EventImagePlaceholder size="sm" label="Oculto" />);
    expect(screen.queryByText('Oculto')).not.toBeInTheDocument();
  });
});
