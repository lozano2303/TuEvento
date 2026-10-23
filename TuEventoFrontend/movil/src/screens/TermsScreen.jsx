import { View, Text, ScrollView, StatusBar } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { useTranslation } from "react-i18next";
import BackButton from "../components/BackButton";
import { darkColors } from "../theme/colors";

export default function TermsScreen() {
  const insets = useSafeAreaInsets();
  const palette = darkColors;
  const { t } = useTranslation();

  // Mismo contenido y orden que la página web (TerminosDeUso.jsx).
  // Cada sección es una secuencia de párrafos (string) y listas (array de claves).
  const sections = [
    ["p1", "p2"],
    ["intro", ["item1", "item2"]],
    ["p1", "intro", ["item1", "item2", "item3"], "p2"],
    ["intro", ["item1", "item2", "item3"], "p1"],
    [["item1", "item2", "item3", "item4"]],
    ["intro", ["item1", "item2", "item3"]],
    ["intro", ["item1", "item2", "item3", "item4"], "p1"],
    ["p1"],
    ["p1"],
    ["p1"],
    ["p1"],
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
              {t("terms.title")}
            </Text>
          </View>
          <View style={{ width: 40 }} />
        </View>

        <ScrollView contentContainerStyle={{ paddingHorizontal: 24, paddingTop: insets.top + 16, paddingBottom: insets.bottom + 24 }}>
          <Text style={{ color: palette.accent, fontSize: 13, marginBottom: 4 }}>{t("terms.subtitle")}</Text>
          <Text style={{ color: palette.textMuted, fontSize: 12, marginBottom: 28 }}>
            {t("terms.lastUpdate")}
          </Text>

          {sections.map((blocks, index) => {
            const ns = `terms.section${index + 1}`;
            return (
              <View key={index} style={{ marginBottom: 24 }}>
                <Text style={{ color: palette.textPrimary, fontSize: 15, fontWeight: "700", marginBottom: 8 }}>
                  {t(`${ns}.title`)}
                </Text>
                {blocks.map((block, i) =>
                  Array.isArray(block) ? (
                    <View key={i} style={{ marginBottom: 8 }}>
                      {block.map((item) => (
                        <View key={item} style={{ flexDirection: "row", gap: 8, marginBottom: 6, paddingLeft: 4 }}>
                          <Text style={{ color: palette.accent, fontSize: 14, lineHeight: 22 }}>•</Text>
                          <Text style={{ color: palette.textSecondary, fontSize: 14, lineHeight: 22, flex: 1 }}>
                            {t(`${ns}.${item}`)}
                          </Text>
                        </View>
                      ))}
                    </View>
                  ) : (
                    <Text key={i} style={{ color: palette.textSecondary, fontSize: 14, lineHeight: 22, marginBottom: 8 }}>
                      {t(`${ns}.${block}`)}
                    </Text>
                  )
                )}
                {index === sections.length - 1 && (
                  <Text style={{ color: palette.accent, fontSize: 14, fontWeight: "600" }}>
                    {t(`${ns}.email`)}
                  </Text>
                )}
              </View>
            );
          })}

          <View style={{ height: 1, backgroundColor: palette.surfaceAlt, marginVertical: 16 }} />
          <Text style={{ color: palette.textMuted, fontSize: 12, lineHeight: 18, fontStyle: "italic" }}>
            {t("terms.disclaimer")}
          </Text>

          <View style={{ height: 40 }} />
        </ScrollView>
      </LinearGradient>
    </View>
  );
}
