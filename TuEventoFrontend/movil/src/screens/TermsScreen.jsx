import { View, Text, ScrollView, StatusBar } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import BackButton from "../components/BackButton";
import { useTheme } from "../context/ThemeContext";

export default function TermsScreen() {
  const insets = useSafeAreaInsets();
  const { palette } = useTheme();

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
    <View style={{ flex: 1, backgroundColor: palette.background }}>
      <StatusBar barStyle="light-content" backgroundColor={palette.background} />
      <LinearGradient colors={[palette.background, palette.surface, palette.background]} style={{ flex: 1 }}>
        {/* Header */}
        <View style={{
          flexDirection: "row", alignItems: "center",
          paddingTop: insets.top + 16, paddingHorizontal: 24, paddingBottom: 20,
          borderBottomWidth: 1, borderBottomColor: palette.surfaceAlt,
        }}>
          <BackButton style={{ marginBottom: 0, backgroundColor: palette.surface, borderColor: palette.surfaceAlt }} />
          <View style={{ flex: 1, alignItems: "center" }}>
            <Text style={{ color: palette.textPrimary, fontSize: 20, fontWeight: "700" }}>
              Términos y Condiciones
            </Text>
          </View>
          <View style={{ width: 40 }} />
        </View>

        <ScrollView contentContainerStyle={{ paddingHorizontal: 24, paddingTop: insets.top + 16, paddingBottom: insets.bottom + 24 }}>
          <Text style={{ color: palette.accent, fontSize: 13, marginBottom: 4 }}>Tu Evento — CapySoft</Text>
          <Text style={{ color: palette.textMuted, fontSize: 12, marginBottom: 28 }}>
            Última actualización: 9 de abril de 2025
          </Text>

          {sections.map((section, index) => (
            <View key={index} style={{ marginBottom: 24 }}>
              <Text style={{ color: palette.textPrimary, fontSize: 15, fontWeight: "700", marginBottom: 8 }}>
                {section.title}
              </Text>
              <Text style={{ color: palette.textSecondary, fontSize: 14, lineHeight: 22 }}>
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
