import { X, CheckCircle2, DollarSign, Wallet, CreditCard, RefreshCw, AlertCircle, Gift, MessageSquare, Reply, Calendar } from 'lucide-react';
import { format } from 'date-fns';
import { es, enUS, fr, pt } from 'date-fns/locale';
import { useTranslation } from 'react-i18next';

/**
 * Obtiene el locale de date-fns según el idioma actual
 */
function getDateLocale(lang) {
  const locales = { es, en: enUS, fr, pt };
  return locales[lang] || es;
}

/**
 * Configuración de iconos por tipo de notificación
 */
const NOTIFICATION_ICONS = {
  PAYMENT_APPROVED: CheckCircle2,
  PAYMENT_REFUNDED: RefreshCw,
  WALLET_CREDITED:  Wallet,
  WELCOME:          Gift,
  EVENT_PUBLISHED:  Calendar,
  EVENT_REJECTED:   AlertCircle,
  NEW_COMMENT:      MessageSquare,
  NEW_REPLY:        Reply,
  DEFAULT:          AlertCircle,
};

function getNotificationIcon(type) {
  return NOTIFICATION_ICONS[type] || NOTIFICATION_ICONS.DEFAULT;
}

/**
 * Formatea una fecha a formato legible
 */
function formatDate(dateString, locale) {
  if (!dateString) return '';
  try {
    return format(new Date(dateString), "d 'de' MMMM 'de' yyyy 'a las' HH:mm", { locale });
  } catch {
    return dateString;
  }
}

/**
 * Extrae información específica del body HTML
 */
function parseNotificationBody(body, t) {
  if (!body) return { text: '', details: [] };
  
  // Remover tags HTML
  const text = body.replace(/<[^>]*>/g, '');
  
  // Extraer montos y detalles
  const details = [];
  const walletMatch = text.match(/Cartera:\s*\$?([\d,.]+)/i);
  const gatewayMatch = text.match(/(?:Tarjeta|QR|Gateway):\s*\$?([\d,.]+)/i);
  const totalMatch = text.match(/Total:\s*\$?([\d,.]+)/i);
  
  if (walletMatch) details.push({ label: t('notifications.details.wallet'), value: `$${walletMatch[1]}`, icon: Wallet });
  if (gatewayMatch) details.push({ label: t('notifications.details.card'), value: `$${gatewayMatch[1]}`, icon: CreditCard });
  if (totalMatch) details.push({ label: t('notifications.details.total'), value: `$${totalMatch[1]}`, icon: DollarSign });
  
  return { text, details };
}

/**
 * Modal de detalle de notificación (simplificado y alineado al tema)
 * FIX BUG #3: Ahora muestra traducciones cuando están disponibles
 */
export default function NotificationDetailModal({ notification, onClose }) {
  const { t, i18n } = useTranslation();
  const locale = getDateLocale(i18n.language);
  const Icon = getNotificationIcon(notification.type);
  const isUnread = !notification.readAt;
  
  // FIX BUG #3: Obtener traducción si existe para el idioma actual
  const userLanguage = i18n.language || 'es';
  const translation = userLanguage !== 'es' 
    ? notification.translations?.find(t => t.languageCode === userLanguage && t.status === 'PUBLISHED')
    : null;
  
  // Usar traducción si existe, sino mostrar original
  const displaySubject = translation?.translatedSubject || notification.subject;
  const displayBody = translation?.translatedBody || notification.body;
  
  // Parsear el body traducido (o original)
  const { text, details } = parseNotificationBody(displayBody, t);
  
  // Detectar si está esperando traducción
  // NO mostrar indicador si la traducción ya está lista (IN_APP envía traducido)
  const isTranslating = false; // Deshabilitado porque IN_APP ahora envía traducido

  return (
    <div 
      className="fixed inset-0 bg-black/70 backdrop-blur-md flex items-center justify-center z-[100] p-4"
      onClick={onClose}
    >
      <div 
        className="bg-background w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden border-2 border-primary/20"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-4 theme-menu-divider flex items-start justify-between">
          <div className="flex items-start gap-4 flex-1">
            {/* Icono */}
            <div className="w-12 h-12 rounded-xl bg-surfaceAlt flex items-center justify-center flex-shrink-0">
              <Icon className="w-6 h-6 text-accent" />
            </div>
            
            {/* Título */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <h2 className="text-lg font-bold text-textPrimary">
                  {displaySubject}
                </h2>
                {isUnread && (
                  <span className="w-2 h-2 rounded-full bg-accent flex-shrink-0"></span>
                )}
                {isTranslating && (
                  <span className="inline-flex items-center gap-1 text-xs text-warning font-normal">
                    <svg className="animate-spin h-3 w-3" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"/>
                    </svg>
                    {t('notifications.translating') || 'Traduciendo...'}
                  </span>
                )}
              </div>
              <p className="text-xs text-textMuted">
                {formatDate(notification.sentAt, locale)}
              </p>
            </div>
          </div>

          {/* Botón cerrar */}
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-surfaceAlt transition-colors flex-shrink-0 ml-2"
            aria-label="Cerrar"
          >
            <X className="w-5 h-5 text-textSecondary" />
          </button>
        </div>

        {/* Contenido */}
        <div className="px-6 py-5 max-h-[60vh] overflow-y-auto">
          {/* Descripción principal */}
          <p className="text-sm text-textSecondary leading-relaxed mb-4">
            {text}
          </p>

          {/* Detalles financieros (si existen) */}
          {details.length > 0 && (
            <div className="space-y-2 mb-4">
              {details.map((detail, index) => {
                const DetailIcon = detail.icon;
                return (
                  <div 
                    key={index}
                    className="flex items-center justify-between p-3 rounded-lg bg-surfaceAlt"
                  >
                    <div className="flex items-center gap-2">
                      <DetailIcon className="w-4 h-4 text-textMuted" />
                      <span className="text-sm font-medium text-textSecondary">
                        {detail.label}
                      </span>
                    </div>
                    <span className="text-sm font-bold text-textPrimary">
                      {detail.value}
                    </span>
                  </div>
                );
              })}
            </div>
          )}

          {/* Tips según tipo (sin fondos de colores, más discreto) */}
          {notification.type === 'PAYMENT_APPROVED' && (
            <div className="p-3 rounded-lg bg-surfaceAlt border border-surfaceAlt">
              <p className="text-xs text-textMuted">
                💡 <strong>Tip:</strong> {t('notifications.details.tips.paymentApproved')}
              </p>
            </div>
          )}

          {notification.type === 'PAYMENT_REFUNDED' && (
            <div className="p-3 rounded-lg bg-surfaceAlt border border-surfaceAlt">
              <p className="text-xs text-textMuted">
                💡 <strong>Tip:</strong> {t('notifications.details.tips.paymentRefunded')}
              </p>
            </div>
          )}

          {notification.type === 'WALLET_CREDITED' && (
            <div className="p-3 rounded-lg bg-surfaceAlt border border-surfaceAlt">
              <p className="text-xs text-textMuted">
                💡 <strong>Tip:</strong> {t('notifications.details.tips.walletCredited')}
              </p>
            </div>
          )}

          {notification.type === 'WELCOME' && (
            <div className="p-4 rounded-lg bg-surfaceAlt">
              <p className="text-xs text-textMuted mb-3">
                <strong>{t('notifications.details.tips.welcomeTitle')}</strong>
              </p>
              <ul className="text-xs text-textMuted space-y-2 ml-4">
                <li className="flex items-start gap-2">
                  <span className="text-accent mt-0.5">•</span>
                  <span>{t('notifications.details.tips.welcomeItem1')}</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-accent mt-0.5">•</span>
                  <span>{t('notifications.details.tips.welcomeItem2')}</span>
                </li>
              </ul>
            </div>
          )}

          {notification.type === 'EVENT_PUBLISHED' && (
            <div className="p-3 rounded-lg bg-surfaceAlt border border-surfaceAlt">
              <p className="text-xs text-textMuted">
                🎉 <strong>¡Enhorabuena!</strong> Tu evento ya es visible para todos los usuarios. Puedes gestionarlo desde <em>Mis eventos</em>.
              </p>
            </div>
          )}

          {notification.type === 'EVENT_REJECTED' && (
            <div className="p-3 rounded-lg bg-surfaceAlt border border-surfaceAlt">
              <p className="text-xs text-textMuted">
                💡 <strong>Tip:</strong> Revisa el motivo de rechazo, corrígelo y vuelve a enviar tu evento a revisión desde <em>Mis eventos</em>.
              </p>
            </div>
          )}

          {notification.type === 'NEW_COMMENT' && (
            <div className="p-3 rounded-lg bg-surfaceAlt border border-surfaceAlt">
              <p className="text-xs text-textMuted">
                💬 <strong>¡Tu evento está generando conversación!</strong> Entra y respóndele — los organizadores que interactúan con su comunidad marcan la diferencia.
              </p>
            </div>
          )}

          {notification.type === 'NEW_REPLY' && (
            <div className="p-3 rounded-lg bg-surfaceAlt border border-surfaceAlt">
              <p className="text-xs text-textMuted">
                🗨️ <strong>¡Te respondieron!</strong> No los dejes hablando solos — entra al evento y continúa la conversación.
              </p>
            </div>
          )}

          {/* Metadata */}
          <div className="mt-4 pt-4 border-t border-surfaceAlt">
            <div className="flex items-center justify-between text-xs text-textMuted">
              <span>ID: #{notification.notificationUserId}</span>
              {notification.readAt && (
                <span>
                  {t('notifications.details.readAt')}: {format(new Date(notification.readAt), 'dd/MM/yy HH:mm')}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 theme-menu-divider flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 bg-accent text-white rounded-lg font-medium hover:bg-accentDark transition-colors"
          >
            {t('notifications.details.close')}
          </button>
        </div>
      </div>
    </div>
  );
}
