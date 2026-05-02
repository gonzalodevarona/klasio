import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        "k-bg": "#F4F4F2",
        "k-surface": "#FAFAF8",
        "k-dark": "#0A0A0A",
        "k-ink": "#2A2A28",
        "k-muted": "#9A9A98",
        "k-subtle": "#4A4A48",
        "k-border": "#DDDDD8",
        "k-line": "#EBEBEA",
        "k-volt": "#CAFF4D",
        "k-volt-text": "#2A4A00",
        "k-warn-bg": "#FFF0C2",
        "k-warn-text": "#8A5A00",
        "k-info-bg": "#E8F4FF",
        "k-info-text": "#0066BB",
        "k-danger-bg": "#FFE8E8",
        "k-danger-text": "#CC2200",
        "k-sidebar-active": "#1A1A1A",
        "k-volt-hover": "#B8EE3A",
        "k-volt-muted": "rgba(202,255,77,0.1)",
      },
      fontFamily: {
        "k-mono": ["var(--font-mono)"],
      },
      borderRadius: {
        "k-sm": "8px",
        "k-md": "12px",
        "k-lg": "16px",
        "k-xl": "20px",
      },
      boxShadow: {
        "k-card": "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)",
        "k-modal": "0 24px 80px rgba(0,0,0,0.25)",
        "k-dropdown": "0 8px 24px rgba(0,0,0,0.12)",
      },
      transitionDuration: {
        k: "150ms",
      },
    },
  },
  plugins: [],
};

export default config;
