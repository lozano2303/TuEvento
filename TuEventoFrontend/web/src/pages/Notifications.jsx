import { useState } from 'react';
import { CheckCircle2, DollarSign, Wallet, Info, AlertCircle, RefreshCw, Gift, PartyPopper, Ban } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { es } from 'date-fns/locale';
import { useNotifications } from '../context/NotificationContext';
import NotificationDetailModal from '../components/common/NotificationDetailModal';

/**
 * Iconos según el tipo de notificación
 */
const NOTIFICATION_ICONS = {
  PAYMENT_APPROVED: { Icon: CheckCircle2,  color: 'text-success'  },
  PAYMENT_REFUNDED: { Icon: RefreshCw,     color: 'text-warning'  },
  WALLET_CREDITED:  { Icon: Wallet,        color: 'text-info'     },
  WELCOME:          { Icon: Gift,          color: 'text-accent'   },
  // Event review flow (Phase 4)
  EVENT_PUBLISHED:  { Icon: PartyPopper,   color: 'text-success'  },
  EVENT_REJECTED:   { Icon: Ban,           color: 'text-error'    },
  DEFAULT:          { Icon: Info,          color: 'text-accent'   },
};

function getNotificationIcon(notificationType) {
  return NOTIFICATION_ICONS[notificationType] || NOTIFICATION_ICONS.DEFAULT;
}

function formatRelativeTime(dateString) {
  if (!dateString) return '';
  try {
    return formatDistanceToNow(new Date(dateString), { addSuffix: true, locale: es });
  } catch {
    return '';
  }
}

/**
 * Card de notificación individual
 */
function NotificationCard({ notification, onMarkAsRead }) {
  const [showModal, setShowModal] = useState(false);
  const { Icon, color } = getNotificationIcon(notification.type);
  const isUnread = !notification.readAt;

  const handleClick = () => {
    if (isUnread) {
      onMarkAsRead(notification.notificationUserId);
    }
    setShowModal(true);
  };

  return (
    <>
      <div
        onClick={handleClick}
        className={`
          theme-card p-4 rounded-xl cursor-pointer transition-all hover:shadow-lg
          ${isUnread ? 'bg-primary/5 border-l-4 border-primary' : 'bg-surface'}
        `}
      >
        <div className="flex items-start gap-4">
          {/* Ícono */}
          <div className={`
            w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0
            ${isUnread ? 'bg-primary/10' : 'bg-surfaceAlt'}
          `}>
            <Icon className={`w-6 h-6 ${color}`} />
          </div>

          {/* Contenido */}
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2 mb-1">
              <h3 className={`
                text-base font-semibold
                ${isUnread ? 'text-textPrimary' : 'text-textSecondary'}
              `}>
                {notification.subject}
              </h3>
              {isUnread && (
                <span className="w-2.5 h-2.5 rounded-full bg-accent flex-shrink-0 mt-1.5" />
              )}
            </div>
            
            <p className="text-sm text-textMuted leading-relaxed mb-2">
              {notification.body?.replace(/<[^>]*>/g, '').substring(0, 150)}
              {notification.body?.length > 150 && '...'}
            </p>
            
            <div className="flex items-center gap-3 text-xs text-textMuted">
              <span>{formatRelativeTime(notification.sentAt)}</span>
              <span>•</span>
              <span className="capitalize">{notification.type?.replace('_', ' ').toLowerCase()}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Modal de detalle */}
      {showModal && (
        <NotificationDetailModal
          notification={notification}
          onClose={() => setShowModal(false)}
        />
      )}
    </>
  );
}

/**
 * Página principal de notificaciones
 */
export default function Notifications() {
  const { notifications, loading, unreadCount, markAsRead, markAllAsRead } = useNotifications();
  const [filter, setFilter] = useState('all'); // 'all' | 'unread'

  const filteredNotifications = filter === 'unread'
    ? notifications.filter(n => !n.readAt)
    : notifications;

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="container mx-auto px-4 max-w-4xl">
        
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-textPrimary mb-2">
            Notificaciones
          </h1>
          <p className="text-textSecondary">
            Mantente al día con todas tus notificaciones
          </p>
        </div>

        {/* Filtros y acciones */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
          {/* Filtros */}
          <div className="flex gap-2">
            <button
              onClick={() => setFilter('all')}
              className={`
                px-4 py-2 rounded-lg text-sm font-medium transition-colors
                ${filter === 'all'
                  ? 'bg-primary text-white'
                  : 'bg-surfaceAlt text-textSecondary hover:bg-surface'
                }
              `}
            >
              Todas ({notifications.length})
            </button>
            <button
              onClick={() => setFilter('unread')}
              className={`
                px-4 py-2 rounded-lg text-sm font-medium transition-colors
                ${filter === 'unread'
                  ? 'bg-primary text-white'
                  : 'bg-surfaceAlt text-textSecondary hover:bg-surface'
                }
              `}
            >
              No leídas ({unreadCount})
            </button>
          </div>

          {/* Acciones */}
          {unreadCount > 0 && (
            <button
              onClick={markAllAsRead}
              className="px-4 py-2 rounded-lg text-sm font-medium bg-accent text-white hover:bg-accentDark transition-colors"
            >
              Marcar todas como leídas
            </button>
          )}
        </div>

        {/* Lista de notificaciones */}
        {loading ? (
          <div className="text-center py-12">
            <div className="animate-spin w-8 h-8 border-4 border-primary border-t-transparent rounded-full mx-auto mb-4"></div>
            <p className="text-textMuted">Cargando notificaciones...</p>
          </div>
        ) : filteredNotifications.length === 0 ? (
          <div className="text-center py-12 theme-card rounded-xl">
            <div className="w-16 h-16 bg-surfaceAlt rounded-full flex items-center justify-center mx-auto mb-4">
              <Info className="w-8 h-8 text-textMuted" />
            </div>
            <h3 className="text-lg font-semibold text-textPrimary mb-2">
              {filter === 'unread' ? 'No tienes notificaciones sin leer' : 'No tienes notificaciones'}
            </h3>
            <p className="text-textMuted">
              {filter === 'unread'
                ? 'Todas tus notificaciones están al día'
                : 'Las notificaciones aparecerán aquí cuando las recibas'
              }
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredNotifications.map((notification) => (
              <NotificationCard
                key={notification.notificationUserId}
                notification={notification}
                onMarkAsRead={markAsRead}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
