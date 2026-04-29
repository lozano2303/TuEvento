import { useRef } from "react";
import { NavigationContainer, createNavigationContainerRef } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { Ionicons } from "@expo/vector-icons";
import { View, Text, TouchableOpacity, Modal } from "react-native";
import { useAuth } from "../context/AuthContext";
import { colors } from "../theme";
import LandingScreen      from "../screens/LandingScreen";
import LoginScreen        from "../screens/LoginScreen";
import RegisterScreen     from "../screens/RegisterScreen";
import ActivateScreen     from "../screens/ActivateScreen";
import TermsScreen        from "../screens/TermsScreen";
import ForgotPasswordScreen from "../screens/ForgotPasswordScreen";
import ResetPasswordScreen  from "../screens/ResetPasswordScreen";
import HomeScreen         from "../screens/HomeScreen";
import ProfileScreen      from "../screens/ProfileScreen";
import SettingsScreen     from "../screens/SettingsScreen";

const Stack = createNativeStackNavigator();
const Tab   = createBottomTabNavigator();
export const navigationRef = createNavigationContainerRef();

// ─── Modal de logout ──────────────────────────────────────────────────────────
function LogoutModal({ visible, onConfirm, onCancel }) {
  return (
    <Modal transparent visible={visible} animationType="fade" statusBarTranslucent>
      <View style={{
        flex: 1, backgroundColor: "#00000088",
        justifyContent: "center", alignItems: "center",
        paddingHorizontal: 32,
      }}>
        <View style={{
          backgroundColor: colors.surface, borderRadius: 20,
          padding: 28, width: "100%",
          borderWidth: 1, borderColor: colors.surfaceAlt,
        }}>
          <Text style={{
            color: colors.textPrimary, fontSize: 20, fontWeight: "800",
            textAlign: "center", marginBottom: 10,
          }}>
            ¿Cerrar sesión?
          </Text>
          <Text style={{
            color: colors.textSecondary, fontSize: 14, textAlign: "center",
            lineHeight: 22, marginBottom: 28,
          }}>
            Tu sesión se cerrará y tendrás que volver a iniciar sesión para acceder.
          </Text>
          <TouchableOpacity
            onPress={onConfirm}
            activeOpacity={0.75}
            style={{
              backgroundColor: colors.primary, borderRadius: 14,
              paddingVertical: 14, alignItems: "center", marginBottom: 12,
            }}
          >
            <Text style={{ color: colors.textPrimary, fontSize: 15, fontWeight: "700" }}>
              Sí, cerrar sesión
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={onCancel}
            activeOpacity={0.75}
            style={{
              borderRadius: 14, paddingVertical: 14,
              alignItems: "center", borderWidth: 1,
              borderColor: colors.surfaceAlt,
            }}
          >
            <Text style={{ color: colors.accent, fontSize: 15, fontWeight: "600" }}>
              Cancelar
            </Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

// ─── Tab bar personalizado ────────────────────────────────────────────────────
function CustomTabBar({ state, descriptors, navigation }) {
  return (
    <View style={{
      flexDirection: "row",
      backgroundColor: colors.surface + "F0",
      borderTopWidth: 1,
      borderTopColor: colors.primary + "30",
      paddingVertical: 8,
      paddingHorizontal: 16,
    }}>
      {state.routes.map((route, index) => {
        const { options } = descriptors[route.key];
        const isFocused = state.index === index;
        const isQR = route.name === "QR";

        const iconName = (() => {
          if (route.name === "Eventos") return isFocused ? "home"     : "home-outline";
          if (route.name === "QR")      return isFocused ? "qr-code"  : "qr-code-outline";
          if (route.name === "Perfil")  return isFocused ? "person"   : "person-outline";
          return "ellipse-outline";
        })();

        const onPress = () => {
          const event = navigation.emit({ type: "tabPress", target: route.key, canPreventDefault: true });
          if (!isFocused && !event.defaultPrevented) navigation.navigate(route.name);
        };

        if (isQR) {
          return (
            <View key={route.key} style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
              <TouchableOpacity
                onPress={onPress}
                activeOpacity={0.75}
                style={{
                  width: 52, height: 52, borderRadius: 26,
                  backgroundColor: colors.primary,
                  alignItems: "center", justifyContent: "center",
                  marginBottom: 14,
                  shadowColor: colors.primary,
                  shadowOffset: { width: 0, height: 4 },
                  shadowOpacity: 0.5, shadowRadius: 8, elevation: 8,
                }}
              >
                <Ionicons name={iconName} size={26} color={colors.textPrimary} />
              </TouchableOpacity>
            </View>
          );
        }

        return (
          <TouchableOpacity
            key={route.key}
            onPress={onPress}
            activeOpacity={0.75}
            style={{ flex: 1, alignItems: "center", justifyContent: "center", paddingVertical: 4 }}
          >
            {isFocused ? (
              /* Pill activo */
              <View style={{
                flexDirection: "row", alignItems: "center", gap: 6,
                backgroundColor: colors.primary,
                borderRadius: 20, paddingHorizontal: 14, paddingVertical: 7,
              }}>
                <Ionicons name={iconName} size={18} color={colors.textPrimary} />
                <Text style={{ color: colors.textPrimary, fontSize: 12, fontWeight: "700" }}>
                  {route.name}
                </Text>
              </View>
            ) : (
              /* Ícono inactivo */
              <View style={{ alignItems: "center", paddingVertical: 4 }}>
                <Ionicons name={iconName} size={22} color={colors.textMuted} />
              </View>
            )}
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

// ─── Tabs principales ─────────────────────────────────────────────────────────
function MainTabs() {
  return (
    <Tab.Navigator
      tabBar={(props) => <CustomTabBar {...props} />}
      screenOptions={{ headerShown: false }}
    >
      <Tab.Screen name="Eventos" component={HomeScreen} />
      <Tab.Screen name="QR"      component={HomeScreen} />
      <Tab.Screen name="Perfil"  component={ProfileScreen} />
    </Tab.Navigator>
  );
}

// ─── Navigator raíz ───────────────────────────────────────────────────────────
export default function AppNavigator() {
  const { logout, showLogoutModal, setShowLogoutModal } = useAuth();
  const isLoggingOut = useRef(false);

  const handleLogoutConfirm = async () => {
    setShowLogoutModal(false);
    isLoggingOut.current = true;
    if (navigationRef.isReady()) {
      navigationRef.reset({ index: 0, routes: [{ name: "Login" }] });
    }
    await logout();
    isLoggingOut.current = false;
  };

  return (
    <NavigationContainer ref={navigationRef}>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Landing"       component={LandingScreen} />
        <Stack.Screen name="Login"         component={LoginScreen} />
        <Stack.Screen name="Register"      component={RegisterScreen} />
        <Stack.Screen name="Terms"         component={TermsScreen} />
        <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
        <Stack.Screen name="ResetPassword"  component={ResetPasswordScreen} />
        <Stack.Screen name="Activate"      component={ActivateScreen} />
        <Stack.Screen name="Settings"      component={SettingsScreen} />
        <Stack.Screen
          name="Main"
          component={MainTabs}
          options={{ gestureEnabled: false }}
          listeners={() => ({
            beforeRemove: (e) => {
              if (isLoggingOut.current) return;
              e.preventDefault();
              setShowLogoutModal(true);
            },
          })}
        />
      </Stack.Navigator>
      <LogoutModal
        visible={showLogoutModal}
        onConfirm={handleLogoutConfirm}
        onCancel={() => setShowLogoutModal(false)}
      />
    </NavigationContainer>
  );
}
