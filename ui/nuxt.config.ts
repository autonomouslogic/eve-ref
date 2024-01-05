// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
    //ssr: false,
    modules: [
        '@nuxtjs/i18n',
        '@nuxtjs/tailwindcss',
    ],
    build: {
        transpile: [
            '@fortawesome/vue-fontawesome'
        ]
    },
    i18n: { // @todo i18n beta-12 requires this to be in a separate file i18n.config.ts, but beta-12 break Cloudflare Pages, so leaving it here temporarily.
        defaultLocale: 'en',
        vueI18n: './i18n.config.ts',
    },
    css: [
        '@fortawesome/fontawesome-svg-core/styles.css'
    ],
    typescript: {
        typeCheck: true,
    }
})
