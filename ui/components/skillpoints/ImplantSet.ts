export class ImplantSet {
    name: string = "";
    bonus: number = 0;
    typeIds: number[] = [];
    totalPrice: number = 0;
    spPerMonthAlpha: number = 0;
    spPerMonthOmega: number = 0;
    iskPerSps: ImplantIskPerSp[] = [];
}

export class ImplantIskPerSp {
    duration: number = 0;
    iskPerSpAlpha: number = 0;
    iskPerSpOmega: number = 0;
}
