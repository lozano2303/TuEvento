import { TouchableOpacity } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useNavigation } from "@react-navigation/native";
import { colors } from "../theme";

export default function BackButton({ onPress, style }) {
  const navigation = useNavigation();

  const handlePress = () => {
    if (onPress) {
      onPress();
      return;
    }
    navigation.goBack();
  };

  return (
    <TouchableOpacity
      onPress={handlePress}
      activeOpacity={0.85}
      hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
      style={[
        {
          width: 40,
          height: 40,
          borderRadius: 20,
          backgroundColor: colors.surface,
          alignItems: "center",
          justifyContent: "center",
          alignSelf: "flex-start",
          marginBottom: 24,
          borderWidth: 1,
          borderColor: colors.surfaceAlt,
        },
        style,
      ]}
    >
      <Ionicons name="arrow-back" size={20} color={colors.textPrimary} />
    </TouchableOpacity>
  );
}
