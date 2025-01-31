const format = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

export function formatMoney(val: number): string  {
    let v = val;
    let s = "";
    if (v >= 1e12) {
        v = val / 1e12;
        s = "t";
    }
    else if (v >= 1e9) {
        v = val / 1e9;
        s = "b";
    }
    else if (v >= 1e6) {
        v = val / 1e6;
        s = "m";
    }
    else if (v >= 1e3) {
        v = val / 1e3;
        s = "k";
    }
    return `${format.format(v)}${s} ISK`;
}
