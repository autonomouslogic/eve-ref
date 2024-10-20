export default defineNuxtPlugin({
    name: "preload",
    parallel: true,
    setup(nuxtApp) {
        useHead({
            link: [{
                rel: "preload",
                href: "https://static.everef.net/search.json",
                as: "fetch",
                crossorigin: "anonymous",
            }]});
    },
});
