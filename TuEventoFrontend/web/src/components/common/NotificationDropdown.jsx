import { CheckCircle2, DollarSign, Wallet, Info, AlertCircle, RefreshCw, Gift } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { es } from 'date-fns/locale';
import { useState } from 'react';
import NotificationDetailModal from './NotificationDetailModal';

/**
 * Iconos según el tipo de notificación
 */
const NOTIFICATION_ICONS = {
  PAYMENT_APPROVED: { Icon: CheckCircle2, color: 'text-success' },
  PAYMENT_REFUNDED: { Icon: RefreshCw, color: 'text-warning' },
  WALLET_CREDITED: { Icon: Wallet, color: 'text-info' },
  WELCOME: { Icon: Gift, color: 'text-accent' },
  DEFAULT: { Icon: Info, color: 'text-accent' },
};

/**
 * Obtiene el ícono y color según el tipo de notificación
 */
function getNotificationIcon(notificationType) {
  return NOTIFICATION_ICONS[notificationType] || NOTIFICATION_ICONS.DEFAULT;
}

/**
 * Formatea la fecha relativa (ej: "hace 5 minutos")
 */
function formatRelativeTime(dateString) {
  if (!dateString) return '';
  try {
    return formatDistanceToNow(new Date(dateString), { addSuffix: true, locale: es });
  } catch {
    return '';
  }
}

/**
 * Item individual de notificación
 */
function NotificationItem({ notification, onMarkAsRead, onClose }) {
  const [showModal, setShowModal] = useState(false);
  const { Icon, color } = getNotificationIcon(notification.type);
  const isUnread = !notification.readAt;

  const handleClick = () => {
    if (isUnread) {
      onMarkAsRead(notification.notificationUserId);
    }
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    // No cerramos el dropdown cuando se cierra el modal
  };

  return (
    <>
      <button
        onClick={handleClick}
        className={`
          w-full text-left px-4 py-3 transition-colors theme-menu-divider
          hover:bg-surfaceAlt
          ${isUnread ? 'bg-surface/50' : 'bg-transparent'}
        `}
      >
        <div className="flex items-start gap-3">
          {/* Ícono */}
          <div className={`
            w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0
            ${isUnread ? 'bg-primary/10' : 'bg-surfaceAlt'}
          `}>
            <Icon className={`w-5 h-5 ${color}`} />
          </div>

          {/* Contenido */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2">
              <p className={`
                text-sm font-medium leading-tight
                ${isUnread ? 'text-textPrimary' : 'text-textSecondary'}
              `}>
                {notification.subject}
              </p>
              {isUnread && (
                <span className="w-2 h-2 rounded-full bg-accent flex-shrink-0 mt-1" />
              )}
            </div>
            
            {/* Body (HTML stripping simplificado) */}
            <p className="text-xs text-textMuted leading-snug mt-1 line-clamp-2">
              {notification.body?.replace(/<[^>]*>/g, '') || ''}
            </p>
            
            {/* Timestamp */}
            <p className="text-[10px] text-textMuted mt-1.5">
              {formatRelativeTime(notification.createdAt)}
            </p>
          </div>
        </div>
      </button>

      {/* Modal de detalle */}
      {showModal && (
        <NotificationDetailModal
          notification={notification}
          onClose={handleCloseModal}
        />
      )}
    </>
  );
}

/**
 * Dropdown con la lista de notificaciones
 */
export default function NotificationDropdown({
  notifications,
  loading,
  unreadCount,
  onMarkAsRead,
  onMarkAllAsRead,
  onClose,
}) {
  // Mostrar solo las últimas 5 notificaciones
  const displayedNotifications = notifications.slice(0, 5);
  const hasMore = notifications.length > 5;

  return (
    <div className="theme-dropdown absolute right-0 mt-2 w-[380px] max-w-[calc(100vw-2rem)] rounded-2xl shadow-2xl z-50 overflow-hidden">
      
      {/* Header */}
      <div className="px-4 py-3 theme-menu-divider flex items-center justify-between">
        <h3 className="text-sm font-bold text-textPrimary">
          Notificaciones {unreadCount > 0 && `(${unreadCount})`}
        </h3>
        {unreadCount > 0 && (
          <button
            onClick={onMarkAllAsRead}
            className="text-xs font-medium text-accent hover:text-accentDark transition-colors"
          >
            Marcar todas como leídas
          </button>
        )}
      </div>

      {/* Lista de notificaciones */}
      <div className="max-h-[400px] overflow-y-auto">
        {loading ? (
          <div className="px-4 py-8 text-center text-textMuted text-sm">
            Cargando notificaciones...
          </div>
        ) : notifications.length === 0 ? (
          <div className="px-4 py-8 text-center">
            <Bell className="w-12 h-12 text-textMuted mx-auto mb-2 opacity-30" />
            <p className="text-sm text-textMuted">No tienes notificaciones</p>
          </div>
        ) : (
          <>
            {displayedNotifications.map((notification) => (
              <NotificationItem
                key={notification.notificationUserId}
                notification={notification}
                onMarkAsRead={onMarkAsRead}
                onClose={onClose}
              />
            ))}
            
            {/* Botón "Ver todas" */}
            {hasMore && (
              <div className="px-4 py-3 theme-menu-divider border-t border-surfaceAlt">
                <a
                  href="/notifications"
                  className="block text-center text-sm font-medium text-accent hover:text-accentDark transition-colors"
                  onClick={onClose}
                >
                  Ver todas las notificaciones ({notifications.length})
                </a>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

// Placeholder para el ícono cuando no hay notificaciones
function Bell({ className }) {
  return (
    <svg className={className} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
      <path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
    </svg>
  );
}
