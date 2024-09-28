import {useNuxt} from "@nuxt/kit";
import iconPath from "~/assets/logo.png";

const suffix = "EVE Ref";

export default defineNuxtPlugin({
    name: "Head",
    parallel: true,
    setup (nuxtApp) {
        const url = useRequestURL();
        useHead({
            titleTemplate: (title) => {
                return title ? `${title} - ${suffix}` : suffix;
            }
        });
        useSeoMeta({
            ogSiteName: "EVE Ref",
            ogDescription: "Reference site for EVE Online",
            twitterCard: "summary",
            ogType: "article",
            ogImage: iconPath,
            ogUrl: url.toString(),
        });
    }
})
