// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
    //ssr: false,
    modules: [
        '@nuxtjs/i18n',
        '@nuxtjs/tailwindcss',
    ],
    i18n: { // @todo i18n beta-12 requires this to be in a separate file i18n.config.ts, but beta-12 break Cloudflare Pages, so leaving it here temporarily.
        defaultLocale: 'en',
        vueI18n: {
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
        }
    },
    css: [
        '@fortawesome/fontawesome-svg-core/styles.css'
    ],
    typescript: {
        typeCheck: true,
    }
})
