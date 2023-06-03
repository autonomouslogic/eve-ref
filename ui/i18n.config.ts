import type { I18nOptions } from 'vue-i18n'

export default defineI18nConfig(() => ({
    locales: ['en'],
    locale: 'en',
    fallbackLocale: 'en',
    messages: {
        en: {
            languageName: 'English',
        }
    }
} as I18nOptions))
