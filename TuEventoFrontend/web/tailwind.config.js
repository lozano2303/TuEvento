/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background:    "var(--color-background)",
        surface:       "var(--color-surface)",
        surfaceAlt:    "var(--color-surfaceAlt)",
        primary:       "var(--color-primary)",
        primaryDark:   "var(--color-primaryDark)",
        accent:        "var(--color-accent)",
        textPrimary:   "var(--color-textPrimary)",
        textSecondary: "var(--color-textSecondary)",
        textMuted:     "var(--color-textMuted)",
        error:         "var(--color-error)",
        success:       "var(--color-success)",
      },
      borderRadius: {
        theme: "var(--color-borderRadius)",
      },
      fontFamily: {
        theme: "var(--color-fontFamily)",
      },
    },
  },
  plugins: [],
}
