interface HourBalanceProps {
  available: number;
  purchased: number;
  "data-testid"?: string;
}

export default function HourBalance({ available, purchased, "data-testid": dataTestId }: HourBalanceProps) {
  const consumed = purchased - available;
  const pct = purchased > 0 ? (available / purchased) * 100 : 0;

  const barColor =
    pct > 50 ? "bg-green-500" : pct > 20 ? "bg-yellow-500" : "bg-red-500";

  return (
    <div className="flex flex-col gap-1 min-w-[120px]" data-testid={dataTestId}>
      <div className="flex items-center justify-between text-xs text-gray-600">
        <span className="font-medium">{available} h left</span>
        <span className="text-gray-400">{consumed} / {purchased} used</span>
      </div>
      <div className="h-1.5 w-full rounded-full bg-gray-200 overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${barColor}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}
