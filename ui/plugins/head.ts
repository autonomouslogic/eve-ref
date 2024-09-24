const suffix = "EVE Ref";

export default defineNuxtPlugin({
    name: "Head",
    parallel: true,
    setup (nuxtApp) {
        useHead({
            titleTemplate: (title) => {
                return title ? `${title} - ${suffix}` : suffix;
            }
        });
        useSeoMeta({
            ogSiteName: "EVE Ref",
            twitterCard: "summary",
            ogType: "article"
        });
    }
})
