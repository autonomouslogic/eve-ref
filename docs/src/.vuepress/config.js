const { description } = require('../../package')

module.exports = {
  /**
   * Ref：https://v1.vuepress.vuejs.org/config/#title
   */
  title: 'EVE Ref Docs',
  /**
   * Ref：https://v1.vuepress.vuejs.org/config/#description
   */
  description: description,

  /**
   * Extra tags to be injected to the page HTML `<head>`
   *
   * ref：https://v1.vuepress.vuejs.org/config/#head
   */
  head: [
    ['meta', { name: 'theme-color', content: '#3eaf7c' }],
    ['meta', { name: 'apple-mobile-web-app-capable', content: 'yes' }],
    ['meta', { name: 'apple-mobile-web-app-status-bar-style', content: 'black' }]
  ],

  /**
   * Theme configuration, here is the default theme configuration for VuePress.
   *
   * ref：https://v1.vuepress.vuejs.org/theme/default-theme-config.html
   */
  themeConfig: {
    repo: 'autonomouslogic/eve-ref',
    editLinks: false,
    docsDir: 'docs/src',
    docsBranch: 'main',
    editLinks: true,
    lastUpdated: true,
    nav: [
      {
        text: 'EVE Ref',
        link: 'https://everef.net'
      },
      {
        text: 'Patreon',
        link: 'https://www.patreon.com/everef'
      },
      {
        text: 'Discord',
        link: 'https://discord.gg/fZYPAxFyXG'
      }
    ],
    sidebar: [
        "/",
        {
          title: "Datasets",
          collapsable: false,
          children: [
            'datasets/',
            'datasets/downloading-datasets',
            'datasets/fuzzwork-ordersets',
            'datasets/hoboleaks-sde',
            'datasets/incursions',
            'datasets/market-history',
            'datasets/market-orders',
            'datasets/public-contracts',
            'datasets/reference-data',
          ]
        },
        {
          title: "Commands",
          collapsable: false,
          children: [
            'commands/docker.md',
            'commands/field-replication.md',
            'commands/build-ref-data.md',
            'commands/import-market-history.md',
            'commands/scrape-market-history.md',
            'commands/scrape-market-orders.md',
            'commands/scrape-public-contracts.md',
            'commands/sync-fuzzwork-ordersets.md',
          ]
        },
        {
          title: "Development",
          collapsable: false,
          children: [
            'development/esi-code-generation.md',
            'development/logging.md'
          ]
        },
        {
          title: "Meta",
          collapsable: false,
          children: [
            'privacy.md'
          ]
        }
    ]
  },

  /**
   * Apply plugins，ref：https://v1.vuepress.vuejs.org/zh/plugin/
   */
  plugins: [
    '@vuepress/plugin-back-to-top',
    '@vuepress/plugin-medium-zoom',
  ]
}
