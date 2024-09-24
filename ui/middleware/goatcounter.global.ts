export default defineNuxtRouteMiddleware((to, from) => {
    if (process.server) {
        return;
    }
    const w = window as any;
    if (!w.goatcounter || !w.goatcounter.count) {
        return;
    }
    let path = to.fullPath;
    const pos = path.indexOf("#");
    if (pos >= 0) {
        path = path.substring(0, pos);
    }
    console.log("path", path);
    w.goatcounter.count({
        path,
        referrer: "",
        title: "",
    })
})
