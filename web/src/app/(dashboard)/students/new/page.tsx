import Link from "next/link";
import StudentForm from "@/components/students/StudentForm";

export const metadata = {
  title: "Add Student - Klasio",
};

export default function NewStudentPage() {
  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/students" className="hover:text-gray-700 hover:underline">
          Students
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Add Student</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-8">
        Add Student
      </h1>

      <StudentForm />
    </div>
  );
}
