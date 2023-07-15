import {describe, it, expect} from 'vitest'
import TypeLinkService from '../../services/typelink.service';

describe('TypeLinkService', () => {
    it.each([
        ['Input <a href=showinfo:11396>Mercoxit</a> string'],
        ['Input <url href=showinfo:11396>Mercoxit</url> string'],
    ])('should extract Type ID from valid links', (input: string) => {
        const expected = ['Input ', 11396, ' string'];
        const actual = new TypeLinkService().parse(input);
        expect(actual).toEqual(expected);
    });

    it.each([
        ['Input <a href=showinfo:5//11396>Mercoxit</a> string'],
        ['Input <url href=showinfo:5//11396>Mercoxit</url> string'],
    ])('should ignore multi-part IDs', (input: string) => {
        const expected = ['Input ', 'Mercoxit', ' string'];
        const actual = new TypeLinkService().parse(input);
        expect(actual).toEqual(expected);
    });
});
