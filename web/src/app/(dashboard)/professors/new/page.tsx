import Link from "next/link";
import ProfessorForm from "@/components/professors/ProfessorForm";

export const metadata = {
  title: "Add Professor - Klasio",
};

export default function NewProfessorPage() {
  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/professors" className="hover:text-gray-700 hover:underline">
          Professors
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Add Professor</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">
        Add Professor
      </h1>

      <ProfessorForm />
    </div>
  );
}
