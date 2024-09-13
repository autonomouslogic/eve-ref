export const locales = [
    {
        code: "de",
        name: "Deutsch",
    },
    {
        code: "en",
        name: "English",
    },
    {
        code: "es",
        name: "Español",
    },
    {
        code: "fr",
        name: "Français",
    },
    {
        code: "ja",
        name: "日本語",
    },
    {
        code: "ru",
        name: "Русский",
    },
    {
        code: "zh",
        name: "中文",
    },
];

export default defineI18nConfig(() => ({
    legacy: false,
    locale: "en",
    // defaultLocale: "en",
    //strategy: "prefix",
    // useCookie: true,
    // locales: [ "de", "en", "es", "fr", "ja", "ru", "zh" ],
    locales,
    messages: {
        de: {
        },
        en: {
        },
        es: {
        },
        fr: {
        },
        ja: {
        },
        ru: {
        },
        zh: {
        },
    }
}));
