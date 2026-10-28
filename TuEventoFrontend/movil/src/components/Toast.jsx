import { Text, StyleSheet, Animated } from 'react-native';
import { useEffect, useRef } from 'react';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTheme } from '../context/ThemeContext';

// Distancia desde la que entra el aviso (por encima de la pantalla)
const HIDDEN_OFFSET = -160;

/**
 * Aviso temporal arriba de la pantalla, bajo la barra de estado. Arriba porque abajo quedaba
 * tapado por el carrito y el mapa de sillas del detalle del evento.
 *
 * @param {Object} props
 * @param {Object} props.toast - { key, message, type }
 * @param {Function} props.onHide - Callback cuando se oculta el toast
 */
export default function Toast({ toast, onHide }) {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const translateY = useRef(new Animated.Value(HIDDEN_OFFSET)).current;

  useEffect(() => {
    if (toast) {
      // Animar entrada
      Animated.spring(translateY, {
        toValue: 0,
        useNativeDriver: true,
        friction: 8,
      }).start();
    } else {
      // Animar salida
      Animated.timing(translateY, {
        toValue: HIDDEN_OFFSET,
        duration: 200,
        useNativeDriver: true,
      }).start();
    }
  }, [toast, translateY]);

  if (!toast) return null;

  const getIcon = () => {
    switch (toast.type) {
      case 'success': return 'checkmark-circle';
      case 'warning': return 'bulb'; // Ícono de bombilla para tip/sugerencia
      case 'info': return 'information-circle';
      default: return 'alert-circle';
    }
  };

  const getColor = () => {
    switch (toast.type) {
      case 'success': return colors.success || '#10b981';
      case 'warning': return colors.accent || '#A78BFA'; // Usar accent del theme en lugar de warning
      case 'info': return colors.primary || '#7C3AED'; // Usar primary para info
      default: return colors.error || '#ef4444';
    }
  };

  return (
    <Animated.View
      accessible
      accessibilityRole="alert"
      accessibilityLiveRegion="polite"
      style={[
        styles.container,
        {
          top: insets.top + 8,
          transform: [{ translateY }],
          // Fondo opaco: encima del mapa de sillas un fondo translúcido no se leía
          backgroundColor: colors.surface || '#1e1e2e',
          borderLeftColor: getColor(),
        },
      ]}
    >
      <Ionicons name={getIcon()} size={20} color={getColor()} />
      <Text style={[styles.message, { color: colors.textPrimary || '#FFFFFF' }]}>
        {toast.message}
      </Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    left: 16,
    right: 16,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 12,
    borderLeftWidth: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
    zIndex: 9999,
  },
  message: {
    flex: 1,
    fontSize: 14,
    fontWeight: '500',
    lineHeight: 20,
  },
});
