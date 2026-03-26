import Link from "next/link";
import ClassForm from "@/components/classes/ClassForm";

export const metadata = {
  title: "New Class | Klasio",
};

export default async function NewClassPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href={`/programs/${id}/classes`} className="hover:text-gray-700">
          Classes
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">New Class</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-6">Create New Class</h1>

      <ClassForm programId={id} />
    </div>
  );
}
