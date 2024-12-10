import {DateTime, Duration} from "luxon";

const defaultMaxAge = Duration.fromISO("PT5M");
const assetsMaxAge = Duration.fromISO("PT1H");

export default defineEventHandler(event => {
    const res = event.node.res;
    console.log("res.statusCode", res.statusCode);
    if (res.statusCode != 200) {
        return;
    }
    const url = event.node.req.url;
    if (url) {
        const isAsset = url.match(/(.+)\.(jpg|jpeg|gif|css|png|js|ico|svg|mjs|webp)/);
        const maxAge = isAsset ? assetsMaxAge : defaultMaxAge;
        const expires = DateTime.now().plus(maxAge).toHTTP();
        res.setHeader('cache-control', `public, max-age=${Math.round(maxAge.toMillis() / 1000)}`);
        res.setHeader('expires', expires);
    }
});
