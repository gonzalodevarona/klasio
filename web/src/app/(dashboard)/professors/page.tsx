import ProfessorList from "@/components/professors/ProfessorList";

export const metadata = {
  title: "Professors - Klasio",
};

export default function ProfessorsPage() {
  return (
    <div>
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Professors</h1>
      </div>

      <ProfessorList />
    </div>
  );
}
