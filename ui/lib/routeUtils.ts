import type {RouteLocationNormalizedLoaded} from "vue-router";

export function getRouteParams(route: RouteLocationNormalizedLoaded, name: string): string | string[] {
    return route.params[name];
}

export function getStringRouteParam(route: RouteLocationNormalizedLoaded, name: string): string {
    var val = getRouteParams(route, name);
    return Array.isArray(val) ? val[0] : val;
}

export function getIntRouteParam(route: RouteLocationNormalizedLoaded, name: string): number {
    return parseInt(getStringRouteParam(route, name));
}
