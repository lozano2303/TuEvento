import { useRef, useState } from "react";
import { View, Text, TouchableOpacity, Modal, Pressable, Dimensions } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useTranslation } from "react-i18next";
import { useTheme } from "../context/ThemeContext";
import { useLanguage } from "../context/LanguageContext";

// ─── Idiomas disponibles (bandera + etiqueta nativa, igual que la web) ───────
export const LANGUAGES = [
  { code: "es", flag: "🇪🇸", label: "Español" },
  { code: "en", flag: "🇺🇸", label: "English" },
  { code: "fr", flag: "🇫🇷", label: "Français" },
  { code: "pt", flag: "🇧🇷", label: "Português" },
];

const MENU_WIDTH = 180;

/**
 * Selector de idioma compacto para pantallas sin sesión (réplica del
 * LanguageSelector de la web). Botón circular con la bandera activa que abre
 * un menú con los 4 idiomas justo debajo del botón.
 */
export default function LanguageSelector({ style }) {
  const { palette } = useTheme();
  const { t } = useTranslation();
  const { language, changeLanguage } = useLanguage();
  const [open, setOpen] = useState(false);
  const [anchor, setAnchor] = useState({ top: 0, right: 0 });
  const buttonRef = useRef(null);

  const current = LANGUAGES.find((l) => l.code === language) ?? LANGUAGES[0];

  const openMenu = () => {
    buttonRef.current?.measureInWindow((x, y, width, height) => {
      const screenWidth = Dimensions.get("window").width;
      setAnchor({ top: y + height + 8, right: Math.max(12, screenWidth - (x + width)) });
      setOpen(true);
    });
  };

  const select = (code) => {
    setOpen(false);
    if (code !== language) changeLanguage(code);
  };

  return (
    <>
      <TouchableOpacity
        ref={buttonRef}
        onPress={openMenu}
        activeOpacity={0.85}
        hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        accessibilityLabel={t("settings.languageTitle")}
        style={[
          {
            width: 40,
            height: 40,
            borderRadius: 20,
            backgroundColor: palette.surface,
            borderWidth: 1,
            borderColor: palette.surfaceAlt,
            alignItems: "center",
            justifyContent: "center",
          },
          style,
        ]}
      >
        <Text style={{ fontSize: 20 }}>{current.flag}</Text>
      </TouchableOpacity>

      <Modal transparent visible={open} animationType="fade" statusBarTranslucent onRequestClose={() => setOpen(false)}>
        <Pressable style={{ flex: 1 }} onPress={() => setOpen(false)}>
          <View
            style={{
              position: "absolute",
              top: anchor.top,
              right: anchor.right,
              width: MENU_WIDTH,
              backgroundColor: palette.surface,
              borderRadius: 14,
              borderWidth: 1,
              borderColor: palette.surfaceAlt,
              paddingVertical: 6,
              shadowColor: "#000",
              shadowOffset: { width: 0, height: 6 },
              shadowOpacity: 0.3,
              shadowRadius: 12,
              elevation: 10,
            }}
          >
            {LANGUAGES.map((lng) => {
              const active = lng.code === language;
              return (
                <TouchableOpacity
                  key={lng.code}
                  onPress={() => select(lng.code)}
                  activeOpacity={0.75}
                  style={{
                    flexDirection: "row",
                    alignItems: "center",
                    gap: 10,
                    paddingHorizontal: 14,
                    paddingVertical: 10,
                    backgroundColor: active ? palette.primary + "22" : "transparent",
                  }}
                >
                  <Text style={{ fontSize: 18 }}>{lng.flag}</Text>
                  <Text style={{ color: palette.textPrimary, fontSize: 14, fontWeight: active ? "700" : "500", flex: 1 }}>
                    {lng.label}
                  </Text>
                  {active && <Ionicons name="checkmark" size={16} color={palette.accent} />}
                </TouchableOpacity>
              );
            })}
          </View>
        </Pressable>
      </Modal>
    </>
  );
}
