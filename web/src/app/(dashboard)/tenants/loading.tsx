export default function TenantsLoading() {
  return (
    <div className="animate-pulse space-y-4">
      <div className="flex items-center justify-between mb-8">
        <div className="h-8 w-32 bg-k-line rounded" />
        <div className="h-10 w-40 bg-k-line rounded" />
      </div>
      <div className="h-10 w-48 bg-k-line rounded" />
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-14 bg-k-line rounded" />
        ))}
      </div>
    </div>
  );
}
