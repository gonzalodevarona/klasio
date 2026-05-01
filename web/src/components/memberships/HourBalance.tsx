interface HourBalanceProps {
  available: number;
  purchased: number;
}

export default function HourBalance({ available, purchased }: HourBalanceProps) {
  const consumed = purchased - available;
  const pct = purchased > 0 ? Math.min(100, Math.round((consumed / purchased) * 100)) : 0;

  const barColor =
    pct <= 33 ? "#CAFF4D" : pct <= 66 ? "#8AE800" : pct <= 85 ? "#FFC107" : "#CC2200";

  return (
    <div className="flex flex-col gap-1.5">
      {/* Big remaining hours */}
      <div className="flex items-end gap-2">
        <span
          className="text-[56px] font-extrabold leading-none tracking-[-0.04em]"
          style={{ color: "#CAFF4D" }}
        >
          {available}
        </span>
        <span className="text-xl text-[#4A4A48] mb-2">/ {purchased}h</span>
        <span className="text-sm text-[#4A4A48] mb-2 ml-1">horas restantes</span>
      </div>
      {/* Progress bar */}
      <div className="h-1.5 w-full rounded-full bg-[#1A1A1A] overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-700"
          style={{ width: `${pct}%`, background: barColor }}
        />
      </div>
      {/* Labels */}
      <div className="flex justify-between">
        <span
          className="text-[10px] uppercase tracking-[0.1em]"
          style={{ fontFamily: "var(--font-mono)", color: "#4A4A48" }}
        >
          Consumido: {consumed}h
        </span>
        <span
          className="text-[10px] uppercase tracking-[0.1em]"
          style={{ fontFamily: "var(--font-mono)", color: "#4A4A48" }}
        >
          {pct}% usado
        </span>
      </div>
    </div>
  );
}
