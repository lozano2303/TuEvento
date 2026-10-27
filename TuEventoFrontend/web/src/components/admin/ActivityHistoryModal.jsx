/**
 * ActivityHistoryModal.jsx
 * Historial completo de actividad — modal del dashboard de administración.
 * Consume GET /admin/activity y renderiza un timeline con scroll.
 */

import { useEffect, useState, useCallback } from 'react';
import { createPortal } from 'react-dom';
import {
  X, CalendarCheck, AlertCircle, Calendar, CreditCard,
  UserPlus, Eye, Loader2, RefreshCcw,
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { es } from 'date-fns/locale';
import { httpRequest } from '../../services/httpClient';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8090/api/v1';

// ── Traducciones de títulos que llegan en inglés desde el backend ─────────────
const TITLE_TRANSLATIONS = {
  // Eventos — estado
  'event approved and published':     'Evento aprobado y publicado',
  'event approved':                   'Evento aprobado',
  'event published':                  'Evento publicado',
  'event rejected':                   'Evento rechazado',
  'event cancelled':                  'Evento cancelado',
  'event completed':                  'Evento finalizado',
  'event created as draft':           'Evento creado como borrador',
  'event sent to review':             'Evento enviado a revisión',
  'event pending review':             'Evento en revisión',
  'event updated':                    'Evento actualizado',
  // Usuarios
  'new user registered':              'Nuevo usuario registrado',
  'user registered':                  'Usuario registrado',
  // Pagos
  'payment approved':                 'Pago aprobado',
  'payment rejected':                 'Pago rechazado',
  'payment refunded':                 'Reembolso procesado',
  'refund processed':                 'Reembolso procesado',
  // Genérico
  'event status changed':             'Estado del evento actualizado',
};

function translateTitle(raw) {
  if (!raw) return raw;
  const key = raw.trim().toLowerCase();
  return TITLE_TRANSLATIONS[key] ?? raw;
}

// ── Ícono y color según tipo de actividad ─────────────────────────────────────
function resolveStyle(item) {
  const { type, title = '' } = item;

  if (type === 'USER_REGISTERED') {
    return { Icon: UserPlus, iconBg: 'bg-violet-500/20', iconColor: 'text-violet-400' };
  }
  if (type === 'PAYMENT') {
    return { Icon: CreditCard, iconBg: 'bg-amber-500/20', iconColor: 'text-amber-400' };
  }

  // EVENT_STATUS_CHANGE — derivar por palabra clave
  const t = title.toLowerCase();
  if (t.includes('approved') || t.includes('published') || t.includes('aprobado') || t.includes('publicado')) {
    return { Icon: CalendarCheck, iconBg: 'bg-emerald-500/20', iconColor: 'text-emerald-400' };
  }
  if (t.includes('rejected') || t.includes('cancelled') || t.includes('rechazado') || t.includes('cancelado')) {
    return { Icon: AlertCircle, iconBg: 'bg-rose-500/20', iconColor: 'text-rose-400' };
  }
  if (t.includes('review') || t.includes('revisión')) {
    return { Icon: Eye, iconBg: 'bg-yellow-500/20', iconColor: 'text-yellow-400' };
  }
  if (t.includes('completed') || t.includes('finalizado')) {
    return { Icon: CalendarCheck, iconBg: 'bg-primary/20', iconColor: 'text-accent' };
  }
  return { Icon: Calendar, iconBg: 'bg-purple-500/20', iconColor: 'text-purple-400' };
}

function fmtTime(iso) {
  if (!iso) return '';
  try {
    return formatDistanceToNow(new Date(iso), { addSuffix: true, locale: es });
  } catch {
    return '';
  }
}

// ── Fila individual de actividad ──────────────────────────────────────────────
function ActivityRow({ item }) {
  const { Icon, iconBg, iconColor } = resolveStyle(item);
  const title = translateTitle(item.title);

  return (
    <div className="flex items-start gap-4 px-6 py-4 border-b last:border-0 transition-colors hover:bg-white/[0.03]"
      style={{ borderColor: 'rgba(124,58,237,0.12)' }}>
      {/* Ícono */}
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 mt-0.5 ${iconBg}`}>
        <Icon className={`w-4.5 h-4.5 ${iconColor}`} style={{ width: '1.125rem', height: '1.125rem' }} />
      </div>

      {/* Contenido */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-textPrimary leading-snug">{title}</p>

        {item.description && (
          <p className="text-xs text-textSecondary mt-1 leading-snug">
            {item.description}
          </p>
        )}

        {item.actor && (
          <p className="text-xs text-textMuted mt-1 flex items-center gap-1.5">
            <svg className="w-3.5 h-3.5 opacity-70 flex-shrink-0" viewBox="0 0 12 12" fill="currentColor">
              <circle cx="6" cy="3.5" r="2"/>
              <path d="M1 11c0-2.76 2.24-5 5-5s5 2.24 5 5"/>
            </svg>
            {item.actor}
          </p>
        )}
      </div>

      {/* Timestamp */}
      <span className="text-sm flex-shrink-0 mt-0.5 whitespace-nowrap" style={{ color: 'rgba(255,255,255,0.55)' }}>
        {fmtTime(item.occurredAt)}
      </span>
    </div>
  );
}

// ── Modal ─────────────────────────────────────────────────────────────────────
export default function ActivityHistoryModal({ isOpen, onClose }) {
  const [items,   setItems]   = useState([]);
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState(null);

  const fetchActivity = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res  = await httpRequest(`${API_URL}/admin/activity?limit=50`, { method: 'GET' });
      const data = await res.json();
      if (res.ok) setItems(data.data || []);
      else setError(data.message || 'Error al cargar actividad');
    } catch (err) {
      setError('Error de conexión: ' + err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    fetchActivity();
  }, [isOpen, fetchActivity]);

  // Cerrar con Escape + bloquear scroll del body
  useEffect(() => {
    if (!isOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => {
      window.removeEventListener('keydown', handler);
      document.body.style.overflow = prev;
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const modal = (
    <div
      role="presentation"
      style={{
        position: 'fixed', inset: 0, zIndex: 9999,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '1rem',
        backgroundColor: 'rgba(0,0,0,0.80)',
        backdropFilter: 'blur(12px)',
        WebkitBackdropFilter: 'blur(12px)',
      }}
      onClick={onClose}
    >
      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="activity-modal-title"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[560px] max-h-[82vh] flex flex-col rounded-2xl overflow-hidden"
        style={{
          background: 'var(--color-surface)',
          border: '1px solid color-mix(in srgb, var(--color-primary) 30%, transparent)',
          boxShadow: '0 20px 60px color-mix(in srgb, var(--color-primary) 30%, transparent)',
        }}
      >
        {/* Header con gradiente morado igual al BaseModal */}
        <div
          className="flex items-start justify-between px-6 py-5 flex-shrink-0"
          style={{
            background: 'linear-gradient(135deg, var(--color-primary) 0%, var(--color-primaryDark) 100%)',
            borderBottom: '1px solid color-mix(in srgb, var(--color-primary) 20%, transparent)',
          }}
        >
          <div>
            <h2
              id="activity-modal-title"
              className="text-base font-black text-white"
            >
              Historial completo de actividad
            </h2>
            <p className="text-xs mt-0.5" style={{ color: 'rgba(255,255,255,0.65)' }}>
              Registro de las acciones recientes realizadas dentro de la plataforma.
            </p>
          </div>

          <div className="flex items-center gap-2 flex-shrink-0 ml-4">
            {/* Refrescar */}
            <button
              onClick={fetchActivity}
              disabled={loading}
              className="w-8 h-8 flex items-center justify-center rounded-lg transition-colors disabled:opacity-40"
              style={{ color: 'rgba(255,255,255,0.7)', background: 'rgba(255,255,255,0.1)' }}
              onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.18)'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.1)'}
              aria-label="Refrescar actividad"
            >
              <RefreshCcw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            {/* Cerrar */}
            <button
              onClick={onClose}
              className="w-8 h-8 flex items-center justify-center rounded-lg transition-colors"
              style={{ color: 'rgba(255,255,255,0.7)', background: 'rgba(255,255,255,0.1)' }}
              onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.18)'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.1)'}
              aria-label="Cerrar historial"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Cuerpo — con scroll */}
        <div className="flex-1 overflow-y-auto" style={{ background: 'var(--color-surface)' }}>
          {loading && items.length === 0 ? (
            <div className="flex flex-col items-center justify-center gap-3 py-16 text-textMuted">
              <Loader2 className="w-6 h-6 animate-spin" />
              <span className="text-sm">Cargando actividad…</span>
            </div>
          ) : error ? (
            <div className="flex flex-col items-center justify-center gap-3 py-16 text-rose-400">
              <AlertCircle className="w-6 h-6" />
              <span className="text-sm">{error}</span>
              <button
                onClick={fetchActivity}
                className="text-xs underline underline-offset-2 hover:text-rose-300 transition-colors"
              >
                Reintentar
              </button>
            </div>
          ) : items.length === 0 ? (
            <div className="flex flex-col items-center justify-center gap-2 py-16 text-textMuted">
              <Calendar className="w-6 h-6 opacity-40" />
              <span className="text-sm">No hay actividad reciente.</span>
            </div>
          ) : (
            items.map((item, i) => (
              <ActivityRow key={`${item.type}-${i}`} item={item} />
            ))
          )}
        </div>

        {/* Footer con conteo */}
        {!loading && items.length > 0 && (
          <div
            className="flex-shrink-0 px-6 py-3"
            style={{
              borderTop: '1px solid color-mix(in srgb, var(--color-primary) 15%, transparent)',
              background: 'var(--color-surface)',
            }}
          >
            <p className="text-xs text-textMuted">
              {items.length} {items.length === 1 ? 'registro' : 'registros'} encontrados
            </p>
          </div>
        )}
      </div>
    </div>
  );

  return createPortal(modal, document.body);
}
