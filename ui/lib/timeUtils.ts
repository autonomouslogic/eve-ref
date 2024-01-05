import {Duration} from "luxon";

export function formatDuration(milliseconds: number): string {
    const ms = Math.floor(milliseconds);
    let format: string[] = [];
    if (ms % 1000 != 0) {
        format.unshift("s.SSS 's'");
    }
    else {
        format.unshift("s 's'");
    }
    if (ms >= 60 * 1000) {
        format.unshift("m 'm'");
    }
    if (ms >= 60 * 60 * 1000) {
        format.unshift("h 'h'");
    }
    if (ms >= 24 * 60 * 60 * 1000) {
        format.unshift("d 'd'");
    }
    return Duration.fromMillis(ms).toFormat(format.join(" "));
}
