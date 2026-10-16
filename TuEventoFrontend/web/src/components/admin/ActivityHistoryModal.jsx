/**
 * ActivityHistoryModal.jsx
 * Full activity history modal for the admin dashboard.
 * Fetches GET /admin/activity and renders a scrollable timeline.
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

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// ── Icon / colour config per activity type ────────────────────────────────────
function resolveStyle(item) {
  const { type, title } = item;

  if (type === 'USER_REGISTERED') {
    return { Icon: UserPlus, iconBg: 'bg-violet-500/15', iconColor: 'text-violet-400' };
  }

  if (type === 'PAYMENT') {
    return { Icon: CreditCard, iconBg: 'bg-amber-500/15', iconColor: 'text-amber-400' };
  }

  // EVENT_STATUS_CHANGE — derive from title keyword
  const t = title.toLowerCase();
  if (t.includes('approved') || t.includes('published')) {
    return { Icon: CalendarCheck, iconBg: 'bg-emerald-500/15', iconColor: 'text-emerald-400' };
  }
  if (t.includes('rejected') || t.includes('cancelled')) {
    return { Icon: AlertCircle,   iconBg: 'bg-rose-500/15',    iconColor: 'text-rose-400'    };
  }
  if (t.includes('review')) {
    return { Icon: Eye,           iconBg: 'bg-yellow-500/15',  iconColor: 'text-yellow-400'  };
  }
  if (t.includes('completed')) {
    return { Icon: CalendarCheck, iconBg: 'bg-primary/15',     iconColor: 'text-accent'      };
  }
  return   { Icon: Calendar,      iconBg: 'bg-slate-500/15',   iconColor: 'text-slate-400'   };
}

function fmtTime(iso) {
  if (!iso) return '';
  try {
    return formatDistanceToNow(new Date(iso), { addSuffix: true, locale: es });
  } catch {
    return '';
  }
}

// ── Single activity row ───────────────────────────────────────────────────────
function ActivityRow({ item }) {
  const { Icon, iconBg, iconColor } = resolveStyle(item);
  return (
    <div className="flex items-start gap-4 px-6 py-4 border-b border-white/[0.05] last:border-0 hover:bg-white/[0.02] transition-colors">
      <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 mt-0.5 ${iconBg}`}>
        <Icon className={`w-4 h-4 ${iconColor}`} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-textPrimary leading-snug">{item.title}</p>
        {item.description && (
          <p className="text-xs text-textSecondary mt-0.5 leading-snug truncate">{item.description}</p>
        )}
        {item.actor && (
          <p className="text-[11px] text-textMuted mt-0.5 flex items-center gap-1">
            <span className="inline-block w-3 h-3 opacity-60">
              {/* tiny person icon via CSS */}
              <svg viewBox="0 0 12 12" fill="currentColor"><circle cx="6" cy="3.5" r="2"/><path d="M1 11c0-2.76 2.24-5 5-5s5 2.24 5 5"/></svg>
            </span>
            {item.actor}
          </p>
        )}
      </div>
      <span className="text-[11px] text-textMuted flex-shrink-0 mt-0.5 whitespace-nowrap">
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

  // Close on Escape + lock body scroll
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
        backgroundColor: 'rgba(0,0,0,0.75)',
        backdropFilter: 'blur(10px)',
        WebkitBackdropFilter: 'blur(10px)',
      }}
      onClick={onClose}
    >
      {/* Card */}
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="activity-modal-title"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-[560px] max-h-[80vh] flex flex-col rounded-2xl border border-white/[0.08] shadow-2xl overflow-hidden"
        style={{ background: 'rgb(18,18,24)' }}
      >
        {/* Header */}
        <div className="flex items-start justify-between px-6 py-5 border-b border-white/[0.07] flex-shrink-0">
          <div>
            <h2
              id="activity-modal-title"
              className="text-lg font-black text-textPrimary"
            >
              Historial completo de actividad
            </h2>
            <p className="text-xs text-textMuted mt-0.5">
              Registro de las acciones recientes realizadas dentro de la plataforma.
            </p>
          </div>
          <div className="flex items-center gap-2 flex-shrink-0 ml-4">
            {/* Refresh */}
            <button
              onClick={fetchActivity}
              disabled={loading}
              className="w-8 h-8 flex items-center justify-center rounded-lg text-textMuted hover:text-textPrimary hover:bg-surfaceAlt/50 transition-colors disabled:opacity-40"
              aria-label="Refrescar actividad"
            >
              <RefreshCcw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            {/* Close */}
            <button
              onClick={onClose}
              className="w-8 h-8 flex items-center justify-center rounded-lg text-textMuted hover:text-textPrimary hover:bg-surfaceAlt/50 transition-colors"
              aria-label="Cerrar historial"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Body — scrollable */}
        <div className="flex-1 overflow-y-auto">
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

        {/* Footer count */}
        {!loading && items.length > 0 && (
          <div className="flex-shrink-0 px-6 py-3 border-t border-white/[0.06]">
            <p className="text-[11px] text-textMuted">
              {items.length} {items.length === 1 ? 'registro' : 'registros'} encontrados
            </p>
          </div>
        )}
      </div>
    </div>
  );

  return createPortal(modal, document.body);
}
