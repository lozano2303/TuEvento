import { useState } from 'react';

const ORGANIZER_COLOR = '#fbbf24';

/**
 * Foto de perfil del autor de un comentario. Si no tiene foto, o no carga, muestra su inicial.
 * El organizador del evento se distingue con un borde dorado (el mismo color que su etiqueta).
 *
 * Props:
 *   comment  { authorName, authorAvatarUrl, isOrganizer }
 *   size     lado en px (32 en comentarios, 24 en respuestas)
 */
export default function CommentAvatar({ comment, size = 32 }) {
  const [failed, setFailed] = useState(false);
  const name = comment.authorName || 'Usuario';
  const style = {
    width: size,
    height: size,
    border: `2px solid ${comment.isOrganizer ? ORGANIZER_COLOR : 'color-mix(in srgb, var(--color-accent) 35%, transparent)'}`,
  };

  if (comment.authorAvatarUrl && !failed) {
    return (
      <img
        src={comment.authorAvatarUrl}
        alt={name}
        className="rounded-full object-cover flex-shrink-0"
        style={style}
        loading="lazy"
        onError={() => setFailed(true)}
      />
    );
  }
  return (
    <span
      aria-hidden="true"
      className="rounded-full flex items-center justify-center flex-shrink-0 font-bold"
      style={{ ...style, background: 'var(--color-primary)', color: 'var(--color-onPrimary)', fontSize: Math.round(size * 0.42) }}
    >
      {name.charAt(0).toUpperCase()}
    </span>
  );
}
