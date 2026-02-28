// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  //ssr: false,

  // https://nuxt.com/docs/getting-started/seo-meta
  app: {
      head: {
          charset: 'utf-8',
          viewport: 'width=device-width, initial-scale=1',

          scripts: {
              globals: {
                  ucLoader: {
                      src: "https://web.cmp.usercentrics.eu/ui/loader.js",
                      type: "application/javascript",
                      id: "usercentrics-cmp",
                      "data-settings-id": "YcvKrzeCLM-3xB",
                      async: true,
                  },
              },
          },
      },
  },

  modules: [
      '@nuxtjs/i18n',
      '@nuxtjs/tailwindcss',
      '@nuxtjs/google-adsense',
  ],

  build: {
      transpile: [
          '@fortawesome/vue-fontawesome'
      ]
  },

  i18n: {
      defaultLocale: "en",
      locales: ["de","en","es", "fr","ja","ru","zh"],
      strategy: "prefix_and_default",
      vueI18n: './i18n.config.ts',
  },

  css: [
      '@fortawesome/fontawesome-svg-core/styles.css'
  ],

  typescript: {
      typeCheck: true,
  },

  googleAdsense: {
      id: "ca-pub-2850842519709578",
  },

  compatibilityDate: "2024-09-24"
})
