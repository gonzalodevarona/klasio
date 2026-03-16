import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Sign In - Klasio",
  description: "Sign in to Klasio Sports League Management",
};

export default function LoginLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <>{children}</>;
}
