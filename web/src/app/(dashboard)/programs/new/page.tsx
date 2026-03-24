import Link from "next/link";
import ProgramForm from "@/components/programs/ProgramForm";

export const metadata = {
  title: "Create New Program - Klasio",
};

export default function NewProgramPage() {
  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/programs" className="hover:text-gray-700 hover:underline">
          Programs
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">New</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">
        Create New Program
      </h1>

      <ProgramForm />
    </div>
  );
}
