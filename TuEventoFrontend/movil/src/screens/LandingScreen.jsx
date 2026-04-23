import { useEffect, useRef } from "react";
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  Animated,
  useWindowDimensions,
  StatusBar,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { useNavigation } from "@react-navigation/native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { decorativeStyle } from "../theme";

export default function LandingScreen() {
  const { width, height } = useWindowDimensions();
  const navigation = useNavigation();
  const insets = useSafeAreaInsets();
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(40)).current;
  const buttonAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.sequence([
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 800,
          useNativeDriver: true,
        }),
        Animated.timing(slideAnim, {
          toValue: 0,
          duration: 800,
          useNativeDriver: true,
        }),
      ]),
      Animated.timing(buttonAnim, {
        toValue: 1,
        duration: 400,
        useNativeDriver: true,
      }),
    ]).start();
  }, []);

  return (
    <View style={{ flex: 1, backgroundColor: "#1E0A3C" }}>
      <StatusBar barStyle="light-content" backgroundColor="#1E0A3C" />

      {/* Fondo con gradiente */}
      <LinearGradient
        colors={["#1E0A3C", "#2D1B4E", "#1E0A3C"]}
        style={{ position: "absolute", width, height }}
      />

      {/* Ola decorativa superior */}
      <View
        style={[
          decorativeStyle.noTouch,
          {
            position: "absolute",
            top: -60,
            left: -40,
            width: width + 80,
            height: 200,
            borderRadius: 120,
            backgroundColor: "#7C3AED22",
          },
        ]}
      />

      {/* Contenido principal */}
      <View style={{ flex: 1, paddingHorizontal: 28, paddingTop: insets.top, paddingBottom: insets.bottom + 16, justifyContent: "space-between" }}>

        {/* Logo y nombre */}
        <Animated.View
          style={{
              flex: 0.35,
              opacity: fadeAnim,
              transform: [{ translateY: slideAnim }],
              alignItems: "center",
              justifyContent: "center",
            }}
        >
          <View
            style={[
              decorativeStyle.noTouch,
              {
                width: 80,
                height: 80,
                borderRadius: 20,
                backgroundColor: "#7C3AED",
                alignItems: "center",
                justifyContent: "center",
                marginBottom: 16,
                shadowColor: "#7C3AED",
                shadowOffset: { width: 0, height: 8 },
                shadowOpacity: 0.5,
                shadowRadius: 16,
                elevation: 12,
              },
            ]}
          >
            <Image
              source={require("../../assets/logo.png")}
              style={{ width: 80, height: 80, borderRadius: 20 }}
              resizeMode="contain"
            />
          </View>
          <Text
            style={{
              color: "#FFFFFF",
              fontSize: 36,
              fontWeight: "800",
              letterSpacing: 1,
              marginBottom: 8,
            }}
          >
            Tu Evento
          </Text>
          <Text
            style={{
              color: "#A78BFA",
              fontSize: 14,
              textAlign: "center",
              lineHeight: 22,
              paddingHorizontal: 20,
            }}
          >
            Diseña, planifica y vive experiencias{"\n"}
            únicas que marquen la diferencia.
          </Text>
        </Animated.View>

        {/* Tarjeta de características */}
        <Animated.View
          style={{
              flex: 0.4,
              opacity: fadeAnim,
              transform: [{ translateY: slideAnim }],
              justifyContent: "center",
            }}
        >
          {[
            { emoji: "🗓️", title: "Crea eventos", desc: "Organiza cada detalle fácilmente" },
            { emoji: "🎟️", title: "Vende tickets", desc: "Gestiona entradas digitales" },
            { emoji: "👥", title: "Conecta personas", desc: "Haz crecer tu comunidad" },
          ].map((item, index) => (
            <View
              key={index}
              style={[
                decorativeStyle.noTouch,
                {
                  flexDirection: "row",
                  alignItems: "center",
                  backgroundColor: "#2D1B4E",
                  borderRadius: 16,
                  padding: 16,
                  marginBottom: 12,
                  borderWidth: 1,
                  borderColor: "#3D2B5E",
                },
              ]}
            >
              <View
                style={[
                  decorativeStyle.noTouch,
                  {
                    width: 44,
                    height: 44,
                    borderRadius: 12,
                    backgroundColor: "#3D2B5E",
                    alignItems: "center",
                    justifyContent: "center",
                    marginRight: 14,
                  },
                ]}
              >
                <Text style={{ fontSize: 20 }}>{item.emoji}</Text>
              </View>
              <View>
                <Text style={{ color: "#FFFFFF", fontWeight: "700", fontSize: 15 }}>
                  {item.title}
                </Text>
                <Text style={{ color: "#A78BFA", fontSize: 13, marginTop: 2 }}>
                  {item.desc}
                </Text>
              </View>
            </View>
          ))}
        </Animated.View>

        {/* Botones */}
        <Animated.View style={{ flex: 0.25, justifyContent: "flex-end", opacity: buttonAnim }}>
          {/* Botón principal */}
          <TouchableOpacity
            onPress={() => navigation.navigate("Register")}
            activeOpacity={0.85}
            style={{
              borderRadius: 14,
              overflow: "hidden",
              marginBottom: 14,
              shadowColor: "#7C3AED",
              shadowOffset: { width: 0, height: 6 },
              shadowOpacity: 0.5,
              shadowRadius: 12,
              elevation: 8,
            }}
          >
            <LinearGradient
              colors={["#7C3AED", "#6D28D9"]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 0 }}
              style={{ paddingVertical: 16, alignItems: "center" }}
            >
              <Text style={{ color: "#FFFFFF", fontSize: 16, fontWeight: "700" }}>
                Comenzar ahora
              </Text>
            </LinearGradient>
          </TouchableOpacity>

          {/* Botón secundario */}
          <TouchableOpacity
            onPress={() => navigation.navigate("Login")}
            activeOpacity={0.85}
            style={{
              borderRadius: 14,
              borderWidth: 1.5,
              borderColor: "#7C3AED",
              paddingVertical: 15,
              alignItems: "center",
            }}
          >
            <Text style={{ color: "#A78BFA", fontSize: 16, fontWeight: "600" }}>
              Iniciar sesión
            </Text>
          </TouchableOpacity>
        </Animated.View>
      </View>

      {/* Ola decorativa inferior */}
      <View
        style={[
          decorativeStyle.noTouch,
          {
            position: "absolute",
            bottom: -40,
            left: -20,
            width: width + 40,
            height: 140,
            borderRadius: 80,
            backgroundColor: "#7C3AED33",
          },
        ]}
      />
    </View>
  );
}
