export default defineNuxtPlugin({
    name: "goatcounter",
    parallel: true,
    setup(nuxtApp) {
        const url = useRequestURL();
        let code = "everef-dev";
        if (url.host == "everef.net" || url.host == "www.everef.net") {
            code = "everef";
        }
        useHead({
            script: [
                {
                    src: "https://gc.zgo.at/count.js",
                    async: true,
                    "data-goatcounter": `https://${code}.goatcounter.com/count`,
                    "data-goatcounter-settings": JSON.stringify({
                        allow_local: false,
                    })
                },
            ],
        });
    },
});
