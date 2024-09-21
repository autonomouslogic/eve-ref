import type {LocaleObject} from "@nuxtjs/i18n";

export const locales: string[] = ["de","en","es", "fr","ja","ru","zh"];

export default defineI18nConfig(() => ({
    legacy: false,
    //locale: "en",
    fallbackLocale: "en",
    //locales: locales,
    messages: {
        de: {
            languageName: "Deutsch",
        },
        en: {
            languageName: "English",
        },
        es: {
            languageName: "Español",
        },
        fr: {
            languageName: "Français",
        },
        ja: {
            languageName: "日本語",
        },
        ru: {
            languageName: "Русский",
        },
        zh: {
            languageName: "中文",
        },
    }
}));
