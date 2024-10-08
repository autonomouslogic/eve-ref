import {describe, expect, it} from 'vitest'
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
        ['Interstellar Shipcaster in <a=showinfo:5//30002974>Mehatoor</a>.'],
        ['Interstellar Shipcaster in <url=showinfo:5//30002974>Mehatoor</url>.'],
    ])('should ignore multi-part IDs', (input: string) => {
        const expected = ['Interstellar Shipcaster in ', 'Mehatoor', '.'];
        const actual = new TypeLinkService().parse(input);
        expect(actual).toEqual(expected);
    });

    it.each([
        ["a\nb"],
        ["a\r\nb"],
    ])('should render line-breaks', (input: string) => {
        const expected = ['a<br/>b'];
        const actual = new TypeLinkService().parse(input);
        expect(actual).toEqual(expected);
    });
});
