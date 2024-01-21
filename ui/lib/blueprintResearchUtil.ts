const researchCosts = [105, 250, 595, 1414, 3360, 8000, 19000, 45255, 107700, 256000];

export function researchTimeForLevel(level: number, baseTime: number): number {
    if (level < 1 || level > 10) throw new Error(`Invalid research level: ${level}`);
    return baseTime * researchCosts[level - 1] / 105;
}
