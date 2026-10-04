const maxNormal = 1e15;
const minNormal = 1 / maxNormal;

export function formatNumber(val: number | undefined, minDecimals: number = 0, maxDecimals: number = 5): string {
    if (typeof val !== "number") {
        return "?";
    }
    const n = Math.abs(val);
    const notation = n != 0 && (n >= maxNormal || n <= minNormal) ? "engineering" : "standard";
    return new Intl.NumberFormat("en-US", {
        minimumFractionDigits: minDecimals,
        maximumFractionDigits: maxDecimals,
        notation
    }).format(val);
}
