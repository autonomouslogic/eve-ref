import {Duration} from "luxon";

export const SECOND = 1000;
export const MINUTE = 60 * SECOND;
export const HOUR = 60 * MINUTE;
export const DAY = 24 * HOUR;

export function formatDuration(milliseconds: number): string {
    let formatted = Duration.fromMillis(milliseconds).toFormat("d'd' h'h' m'm' s.SSS's'");
    // Remove extra fractional milliseconds.
    formatted = formatted.replaceAll(/0+s/g, "s");
    formatted = formatted.replaceAll(".s", "s");
    // Remove zero parts.
    formatted = formatted.replaceAll(/(^| )0[dhms]/g, " ");
    // Remove extra spaces.
    formatted = formatted.replaceAll(/ +/g, " ");
    formatted = formatted.trim();
    // Fallback to 0s.
    if (formatted == "") {
        return "0s";
    }
    return formatted;
}

export function secondsToMilliseconds(seconds: number | undefined): number {
    return seconds === undefined ? 0 : seconds * 1000;
}
