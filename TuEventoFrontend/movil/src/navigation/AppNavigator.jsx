import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { Ionicons } from "@expo/vector-icons";
import { View } from "react-native";
import LandingScreen from "../screens/LandingScreen";
import LoginScreen from "../screens/LoginScreen";
import RegisterScreen from "../screens/RegisterScreen";
import ActivateScreen from "../screens/ActivateScreen";
import TermsScreen from "../screens/TermsScreen";
import HomeScreen from "../screens/HomeScreen";

const Stack = createNativeStackNavigator();
const Tab = createBottomTabNavigator();

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
          tabBarIcon: ({ focused }) => (
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
  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        <Stack.Screen name="Landing" component={LandingScreen} />
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Register" component={RegisterScreen} />
        <Stack.Screen name="Terms" component={TermsScreen} />
        <Stack.Screen name="Activate" component={ActivateScreen} />
        <Stack.Screen name="Main" component={MainTabs} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
