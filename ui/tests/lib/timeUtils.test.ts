import {describe, expect, it} from 'vitest'
import {formatDuration} from "../../lib/timeUtils";

describe('timeUtils', () => {
    describe('formatTime', () => {
        it.each([
            [0, '0 s'],
            [123, '0.123 s'],
            [1123, '1.123 s'],
            [1100, '1.100 s'],

            [(60 - 1) * 1000, '59 s'],
            [60 * 1000, '1 m 0 s'],
            [(60 + 1) * 1000, '1 m 1 s'],

            [(60 * 60 - 1) * 1000, '59 m 59 s'],
            [(60 * 60) * 1000, '1 h 0 m 0 s'],
            [(60 * 60 + 1) * 1000, '1 h 0 m 1 s'],

            [(24 * 60 * 60 - 1) * 1000, '23 h 59 m 59 s'],
            [(24 * 60 * 60) * 1000, '1 d 0 h 0 m 0 s'],
            [(24 * 60 * 60 + 1) * 1000, '1 d 0 h 0 m 1 s'],

            [9843615645, '113 d 22 h 20 m 15.645 s'],
        ])('should format time', (input: number, expected: string) => {
            const actual = formatDuration(input);
            expect(actual).toEqual(expected);
        });
    });
});
