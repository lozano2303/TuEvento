import { View, Text, StyleSheet, Animated, Dimensions } from 'react-native';
import { useEffect, useRef } from 'react';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../context/ThemeContext';

const { width } = Dimensions.get('window');

/**
 * Componente de Toast/Snackbar para mostrar alertas temporales.
 * 
 * @param {Object} props
 * @param {Object} props.toast - { key, message, type }
 * @param {Function} props.onHide - Callback cuando se oculta el toast
 */
export default function Toast({ toast, onHide }) {
  const { colors } = useTheme();
  const translateY = useRef(new Animated.Value(100)).current;

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
        toValue: 100,
        duration: 200,
        useNativeDriver: true,
      }).start();
    }
  }, [toast, translateY]);

  if (!toast) return null;

  const getIcon = () => {
    switch (toast.type) {
      case 'success': return 'checkmark-circle';
      case 'warning': return 'warning';
      case 'info': return 'information-circle';
      default: return 'alert-circle';
    }
  };

  const getColor = () => {
    switch (toast.type) {
      case 'success': return colors.success || '#10b981';
      case 'warning': return colors.warning || '#f59e0b';
      case 'info': return colors.info || '#3b82f6';
      default: return colors.error || '#ef4444';
    }
  };

  return (
    <Animated.View
      style={[
        styles.container,
        {
          transform: [{ translateY }],
          backgroundColor: colors.surface || '#1e1e2e',
          borderLeftColor: getColor(),
        },
      ]}
    >
      <Ionicons name={getIcon()} size={20} color={getColor()} />
      <Text style={[styles.message, { color: colors.textPrimary }]}>
        {toast.message}
      </Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: 20,
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
