import {describe, expect, it} from 'vitest'
import {DAY, formatDuration, HOUR, MINUTE, SECOND} from "../../lib/timeUtils";

describe('timeUtils', () => {
    describe('formatTime', () => {
        it.each([
            [0, '0s'],
            [123, '0.123s'],
            [1234, '1.234s'],
            [1230, '1.23s'],
            [1200, '1.2s'],
            [1000, '1s'],
            [2000, '2s'],
            [59 * SECOND, '59s'],

            [MINUTE, '1m'],
            [MINUTE + SECOND, '1m 1s'],

            [59 * MINUTE + 58 * SECOND, '59m 58s'],
            [HOUR, '1h'],
            [2 * HOUR, '2h'],
            [HOUR + 2 * SECOND, '1h 2s'],
            [HOUR + 2 * MINUTE, '1h 2m'],
            [HOUR + 2 * MINUTE + 3 * SECOND, '1h 2m 3s'],

            [23 * HOUR + 59 * MINUTE + 58 * SECOND, '23h 59m 58s'],
            [DAY, '1d'],
            [2 * DAY, '2d'],
            [DAY + 2 * SECOND, '1d 2s'],
            [DAY + 2 * MINUTE, '1d 2m'],
            [DAY + 2 * HOUR, '1d 2h'],

            [9843615645, '113d 22h 20m 15.645s'],
        ])('should format time', (input: number, expected: string) => {
            const actual = formatDuration(input);
            expect(actual).toEqual(expected);
        });
    });
});
