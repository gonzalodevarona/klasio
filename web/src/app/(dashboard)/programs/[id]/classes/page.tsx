import Link from "next/link";
import ClassList from "@/components/classes/ClassList";

export const metadata = {
  title: "Classes - Klasio",
};

export default async function ClassesPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return (
    <div>
      <nav className="mb-6 text-sm text-gray-500">
        <Link href="/programs" className="hover:text-gray-700 hover:underline">
          Programs
        </Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Classes</span>
      </nav>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Classes</h1>
        <Link
          href={`/programs/${id}/classes/new`}
          className="inline-flex items-center rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          Add Class
        </Link>
      </div>

      <ClassList programId={id} />
    </div>
  );
}
