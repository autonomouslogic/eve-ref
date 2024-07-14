export const i18nConfigGlobalMessages = {
    de: {
        languageName: 'Deutsch',
    },
    en: {
        languageName: 'English',
    },
    es: {
        languageName: 'Español',
    },
    fr: {
        languageName: 'Français',
    },
    ja: {
        languageName: '日本語',
    },
    ru: {
        languageName: 'Русский',
    },
    zh: {
        languageName: '中文',
    },
};

export default defineI18nConfig(() => ({
    locale: 'en',
    fallbackLocale: 'en',
    messages: i18nConfigGlobalMessages
}));
