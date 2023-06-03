export default defineI18nConfig(() => ({
    locales: ['de', 'en', 'es', 'fr', 'ja', 'ru', 'zh'],
    locale: 'en',
    fallbackLocale: 'en',
    messages: {
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
    }
}))
