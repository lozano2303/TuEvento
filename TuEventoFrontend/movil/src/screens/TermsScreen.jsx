import { View, Text, ScrollView, TouchableOpacity, StatusBar } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";

export default function TermsScreen() {
  const navigation = useNavigation();

  const sections = [
    {
      title: "1. Aceptación de los Términos",
      content: "Al acceder al sitio web TuEvento, gestionado por CapiSoft, aceptas cumplir con los siguientes términos y condiciones de uso. Si no estás de acuerdo con estos Términos, te pedimos que no utilices el sistema.",
    },
    {
      title: "2. Descripción del Servicio",
      content: "El sistema permite a los usuarios comprar boletos para eventos organizados por terceros o por nosotros, publicar, promocionar y gestionar sus propios eventos, y comprar productos o servicios relacionados con los eventos.",
    },
    {
      title: "3. Registro de Usuario",
      content: "Para utilizar ciertas funciones del sistema es necesario que te registres como usuario. Eres responsable de mantener la confidencialidad de tu cuenta y contraseña.",
    },
    {
      title: "4. Compra de Boletos",
      content: "Los boletos son personales e intransferibles. El precio será claramente especificado antes de la compra. Las compras son finales salvo cancelación del evento.",
    },
    {
      title: "5. Publicación de Eventos",
      content: "Si decides publicar un evento, aceptas ser responsable del contenido, garantizar que sea legal y cumplir con todas las leyes aplicables.",
    },
    {
      title: "6. Políticas de Cancelación",
      content: "En general, las compras de boletos no son reembolsables, salvo en casos específicos determinados por el organizador o ante cancelación del evento.",
    },
    {
      title: "7. Derechos de Propiedad Intelectual",
      content: "Todo el contenido presente en el sistema está protegido por derechos de autor. El uso no autorizado está estrictamente prohibido sin el consentimiento explícito de CapiSoft.",
    },
    {
      title: "8. Responsabilidades del Usuario",
      content: "No utilizar el sistema para fines ilegales, no realizar actividades que puedan dañar su funcionamiento, no intentar obtener información restringida.",
    },
    {
      title: "9. Modificaciones",
      content: "Nos reservamos el derecho de modificar los Términos en cualquier momento. Se recomienda revisarlos periódicamente.",
    },
    {
      title: "10. Privacidad",
      content: "La privacidad de los usuarios es una prioridad. Toda la información personal será tratada conforme a nuestra Política de Privacidad.",
    },
    {
      title: "11. Ley Aplicable",
      content: "Estos Términos se regirán por las leyes de Colombia. Cualquier disputa será resuelta por los tribunales competentes de Bogotá, D.C., Colombia.",
    },
    {
      title: "12. Contacto",
      content: "Si tienes alguna duda sobre estos Términos, por favor contáctanos a través de nuestro formulario disponible en el sistema.",
    },
  ];

  return (
    <View style={{ flex: 1, backgroundColor: "#1E0A3C" }}>
      <StatusBar barStyle="light-content" backgroundColor="#1E0A3C" />
      <LinearGradient colors={["#1E0A3C", "#2D1B4E", "#1E0A3C"]} style={{ flex: 1 }}>
        {/* Header */}
        <View style={{
          flexDirection: "row", alignItems: "center",
          paddingTop: 56, paddingHorizontal: 24, paddingBottom: 20,
          borderBottomWidth: 1, borderBottomColor: "#3D2B5E",
        }}>
          <TouchableOpacity
            onPress={() => navigation.goBack()}
            style={{
              width: 40, height: 40, borderRadius: 20,
              backgroundColor: "#2D1B4E", alignItems: "center",
              justifyContent: "center", borderWidth: 1, borderColor: "#3D2B5E",
            }}
          >
            <Ionicons name="arrow-back" size={20} color="#FFFFFF" />
          </TouchableOpacity>
          <Text style={{ color: "#FFFFFF", fontSize: 20, fontWeight: "700", marginLeft: 16 }}>
            Términos y Condiciones
          </Text>
        </View>

        <ScrollView contentContainerStyle={{ padding: 24 }}>
          <Text style={{ color: "#A78BFA", fontSize: 13, marginBottom: 4 }}>Tu Evento — CapiSoft</Text>
          <Text style={{ color: "#6B7280", fontSize: 12, marginBottom: 28 }}>
            Última actualización: 9 de abril de 2025
          </Text>

          {sections.map((section, index) => (
            <View key={index} style={{ marginBottom: 24 }}>
              <Text style={{ color: "#FFFFFF", fontSize: 15, fontWeight: "700", marginBottom: 8 }}>
                {section.title}
              </Text>
              <Text style={{ color: "#9CA3AF", fontSize: 14, lineHeight: 22 }}>
                {section.content}
              </Text>
            </View>
          ))}

          <View style={{ height: 40 }} />
        </ScrollView>
      </LinearGradient>
    </View>
  );
}
