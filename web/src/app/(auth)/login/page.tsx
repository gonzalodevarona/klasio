import { getTranslations } from "next-intl/server";
import LoginForm from "@/components/auth/LoginForm";

export default async function LoginPage() {
  const t = await getTranslations("loginPage");

  return (
    <div style={{ display: "flex", minHeight: "100vh", fontFamily: "var(--font-main)" }}>

      {/* LEFT PANEL — hidden on mobile */}
      <div
        className="hidden md:flex"
        style={{
          width: "45%",
          background: "#0A0A0A",
          flexDirection: "column",
          justifyContent: "space-between",
          padding: "48px 56px",
        }}
      >
        {/* TOP: logo lockup */}
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <svg width="28" height="28" viewBox="0 0 40 40" fill="none">
            <rect x="6" y="6" width="6" height="28" fill="#CAFF4D" />
            <polygon points="14,6 40,6 40,14 20,22 14,22" fill="#CAFF4D" opacity="0.9" />
            <polygon points="20,22 40,30 40,38 14,22" fill="#CAFF4D" opacity="0.6" />
          </svg>
          <span style={{ fontSize: 20, fontWeight: 800, color: "#FAFAF8", letterSpacing: "-0.03em" }}>
            klasio
          </span>
        </div>

        {/* MIDDLE: hero copy */}
        <div>
          <p style={{
            fontFamily: "var(--font-mono)",
            fontSize: 10,
            letterSpacing: "0.12em",
            textTransform: "uppercase",
            color: "#4A4A48",
            marginBottom: 24,
          }}>
            {t("tagline")}
          </p>
          <h2 style={{
            fontSize: 40,
            fontWeight: 800,
            color: "#FAFAF8",
            letterSpacing: "-0.03em",
            lineHeight: 1.1,
            marginBottom: 20,
          }}>
            {t("heroLine1")}<br />
            <span style={{ color: "#CAFF4D" }}>{t("heroAccent")}</span>{t("heroLine2")}<br />
            {t("heroLine3")}
          </h2>
          <p style={{ fontSize: 14, color: "#4A4A48", lineHeight: 1.7, maxWidth: 280 }}>
            {t("heroBody")}
          </p>
        </div>

        {/* BOTTOM: tag pills */}
        <div style={{ display: "flex", gap: 16 }}>
          {[t("tag1"), t("tag2"), t("tag3")].map((tag) => (
            <span
              key={tag}
              style={{
                fontFamily: "var(--font-mono)",
                fontSize: 10,
                letterSpacing: "0.08em",
                color: "#4A4A48",
                border: "1px solid #2A2A2A",
                borderRadius: 6,
                padding: "4px 10px",
              }}
            >
              {tag}
            </span>
          ))}
        </div>
      </div>

      {/* RIGHT PANEL */}
      <div
        className="flex-1 flex items-center justify-center"
        style={{ background: "#F4F4F2", padding: "40px 24px" }}
      >
        <div style={{ width: "100%", maxWidth: 360 }}>
          <h1 style={{
            fontSize: 26,
            fontWeight: 800,
            color: "#0A0A0A",
            letterSpacing: "-0.02em",
            marginBottom: 6,
          }}>
            {t("title")}
          </h1>
          <p style={{ fontSize: 13, color: "#9A9A98", marginBottom: 36 }}>
            {t("subtitle")}
          </p>
          <LoginForm />
        </div>
      </div>

    </div>
  );
}
