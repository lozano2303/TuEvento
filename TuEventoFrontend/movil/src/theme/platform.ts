import { Platform, StyleSheet } from "react-native";

export const isIOS = Platform.OS === "ios";
export const isAndroid = Platform.OS === "android";

export const decorativeStyle = StyleSheet.create({
  noTouch: {
    pointerEvents: "none" as const,
  },
});

export const iosBase = StyleSheet.create({
  safeContainer: {
    flex: 1,
    ...(isIOS && {
      overScrollMode: "never" as any,
    }),
  },
  scroll: {
    ...(isIOS && {
      scrollEventThrottle: 16,
      decelerationRate: "fast" as const,
      showsVerticalScrollIndicator: false,
      showsHorizontalScrollIndicator: false,
    }),
  },
});

export function platformStyle<T extends object>(styles: {
  ios?: T;
  android?: T;
  default?: T;
}): T | undefined {
  if (isIOS && styles.ios) return styles.ios;
  if (isAndroid && styles.android) return styles.android;
  return styles.default;
}

export const platformConfig = {
  animationDuration: isIOS ? 250 : 300,
  cardShadow: isIOS
    ? { shadowColor: "#000", shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.15, shadowRadius: 8 }
    : { elevation: 6 },
  buttonShadow: isIOS
    ? { shadowColor: "#7C3AED", shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8 }
    : { elevation: 8 },
} as const;
