export default function TenantDetailLoading() {
  return (
    <div className="animate-pulse space-y-6">
      <div className="h-4 w-48 bg-gray-200 rounded" />
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="px-6 py-5 border-b border-gray-200 flex items-center gap-4">
          <div className="h-12 w-12 bg-gray-200 rounded-full" />
          <div className="space-y-2">
            <div className="h-6 w-40 bg-gray-200 rounded" />
            <div className="h-4 w-24 bg-gray-200 rounded" />
          </div>
        </div>
        <div className="px-6 py-5 grid grid-cols-2 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="space-y-2">
              <div className="h-4 w-24 bg-gray-200 rounded" />
              <div className="h-4 w-32 bg-gray-200 rounded" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
