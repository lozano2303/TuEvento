import { useRef } from "react";
import { NavigationContainer, createNavigationContainerRef } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { Ionicons } from "@expo/vector-icons";
import { View, Text, TouchableOpacity, Modal } from "react-native";
import { useAuth } from "../context/AuthContext";
import LandingScreen from "../screens/LandingScreen";
import LoginScreen from "../screens/LoginScreen";
import RegisterScreen from "../screens/RegisterScreen";
import ActivateScreen from "../screens/ActivateScreen";
import TermsScreen from "../screens/TermsScreen";
import ForgotPasswordScreen from "../screens/ForgotPasswordScreen";
import ResetPasswordScreen from "../screens/ResetPasswordScreen";
import HomeScreen from "../screens/HomeScreen";

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();
export const navigationRef = createNavigationContainerRef();

function LogoutModal({ visible, onConfirm, onCancel }) {
  return (
    <Modal
      transparent
      visible={visible}
      animationType="fade"
      statusBarTranslucent
    >
      <View style={{
        flex: 1, backgroundColor: "#00000088",
        justifyContent: "center", alignItems: "center",
        paddingHorizontal: 32,
      }}>
        <View style={{
          backgroundColor: "#2D1B4E", borderRadius: 20,
          padding: 28, width: "100%",
          borderWidth: 1, borderColor: "#3D2B5E",
        }}>
          <Text style={{
            color: "#FFFFFF", fontSize: 20, fontWeight: "800",
            textAlign: "center", marginBottom: 10,
          }}>
            ¿Cerrar sesión?
          </Text>
          <Text style={{
            color: "#9CA3AF", fontSize: 14, textAlign: "center",
            lineHeight: 22, marginBottom: 28,
          }}>
            Tu sesión se cerrará y tendrás que volver a iniciar sesión para acceder.
          </Text>
          <TouchableOpacity
            onPress={onConfirm}
            activeOpacity={0.85}
            style={{
              backgroundColor: "#7C3AED", borderRadius: 14,
              paddingVertical: 14, alignItems: "center", marginBottom: 12,
            }}
          >
            <Text style={{ color: "#FFFFFF", fontSize: 15, fontWeight: "700" }}>Sí, cerrar sesión</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={onCancel}
            activeOpacity={0.85}
            style={{
              borderRadius: 14, paddingVertical: 14,
              alignItems: "center", borderWidth: 1, borderColor: "#3D2B5E",
            }}
          >
            <Text style={{ color: "#A78BFA", fontSize: 15, fontWeight: "600" }}>Cancelar</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarStyle: {
          backgroundColor: "#2D1B4E",
          borderTopColor: "#3D2B5E",
          borderTopWidth: 1,
          height: 64,
          paddingBottom: 10,
          paddingTop: 8,
        },
        tabBarActiveTintColor: "#7C3AED",
        tabBarInactiveTintColor: "#6B7280",
        tabBarIcon: ({ focused, color }) => {
          let iconName;
          if (route.name === "Eventos") iconName = focused ? "calendar" : "calendar-outline";
          else if (route.name === "QR") iconName = focused ? "qr-code" : "qr-code-outline";
          else if (route.name === "Perfil") iconName = focused ? "person" : "person-outline";
          return <Ionicons name={iconName} size={24} color={color} />;
        },
      })}
    >
      <Tab.Screen name="Eventos" component={HomeScreen} />
      <Tab.Screen
        name="QR"
        component={HomeScreen}
        options={{
          tabBarIcon: () => (
            <View style={{
              width: 52, height: 52, borderRadius: 26,
              backgroundColor: "#7C3AED", alignItems: "center",
              justifyContent: "center", marginBottom: 18,
              shadowColor: "#7C3AED", shadowOffset: { width: 0, height: 4 },
              shadowOpacity: 0.5, shadowRadius: 8, elevation: 8,
            }}>
              <Ionicons name="qr-code-outline" size={26} color="#FFFFFF" />
            </View>
          ),
          tabBarLabel: () => null,
        }}
      />
      <Tab.Screen name="Perfil" component={HomeScreen} />
    </Tab.Navigator>
  );
}

export default function AppNavigator() {
  const { logout, showLogoutModal, setShowLogoutModal } = useAuth();
  const isLoggingOut = useRef(false);

  const handleLogoutConfirm = async () => {
    // 1. Cierra el modal
    setShowLogoutModal(false);
    // 2. Marca que es un logout para que beforeRemove no bloquee
    isLoggingOut.current = true;
    // 3. Navega a Login ANTES de limpiar el estado
    if (navigationRef.isReady()) {
      navigationRef.reset({
        index: 0,
        routes: [{ name: "Login" }],
      });
    }
    // 4. Limpia el estado después de navegar
    await logout();
    // 5. Resetea el flag
    isLoggingOut.current = false;
  };

  return (
    <NavigationContainer ref={navigationRef}>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Landing" component={LandingScreen} />
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Register" component={RegisterScreen} />
        <Stack.Screen name="Terms" component={TermsScreen} />
        <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
        <Stack.Screen name="ResetPassword" component={ResetPasswordScreen} />
        <Stack.Screen name="Activate" component={ActivateScreen} />
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
