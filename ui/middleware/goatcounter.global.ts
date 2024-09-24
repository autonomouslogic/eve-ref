export default defineNuxtRouteMiddleware((to, from) => {
    if (process.server) {
        return;
    }
    const w = window as any;
    if (!w.goatcounter || !w.goatcounter.count) {
        return;
    }
    const path = to.fullPath;
    w.goatcounter.count({path})
})
