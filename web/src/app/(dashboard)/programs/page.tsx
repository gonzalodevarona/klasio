import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button } from "@/components/ui";
import ProgramList from "@/components/programs/ProgramList";

export async function generateMetadata() {
  const t = await getTranslations("programs");
  return { title: `${t("pageTitle")} - Klasio` };
}

export default async function ProgramsPage() {
  const t = await getTranslations("programs");

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em] text-k-dark">{t("pageTitle")}</h1>
        <Button variant="volt" asChild>
          <Link href="/programs/new">+ {t("createButton")}</Link>
        </Button>
      </div>

      <ProgramList />
    </div>
  );
}
