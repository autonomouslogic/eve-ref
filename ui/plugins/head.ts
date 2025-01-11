import iconPath from "~/assets/logo.png";

const suffix = "EVE Ref";

export default defineNuxtPlugin({
    name: "Head",
    parallel: true,
    setup (nuxtApp) {
        const siteName = "EVE Ref"
        const titleTemplate = "%s %separator %siteName";
        const description = "Reference site for EVE Online";
        const url = useRequestURL();
        useHead({
            titleTemplate,
            templateParams: {
                siteName,
                separator: "|",
            }
        });
        const t = this;
        useSeoMeta({
            ogTitle: titleTemplate,
            ogSiteName: siteName,
            ogDescription: description,
            twitterCard: "summary",
            ogType: "website",
            ogImage: iconPath,
            ogUrl: url.toString(),
        });
    }
})
