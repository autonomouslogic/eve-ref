import {
    Bundle,
    Configuration,
    DogmaAttributeToJSON,
    FetchAPI,
    InventoryTypeToJSON,
    RefdataApi
} from "~/refdata-openapi";

const cache: { [key: string]: string; } = {};

function cacheBundleObj(bundle: Bundle): void {
    console.log("Bundle types: ", bundle.types);
    for (let typeId in bundle.types) {
        const type = bundle.types[typeId];
        const path = "/types/" + typeId;
        console.log("Caching ", path);
        cache[path] = JSON.stringify(InventoryTypeToJSON(type));
    }
    console.log("Bundle dogmaAttributes: ", bundle.dogmaAttributes);
    for (let attributeId in bundle.dogmaAttributes) {
        const attribute = bundle.dogmaAttributes[attributeId];
        const path = "/dogma_attributes/" + attributeId;
        console.log("Caching ", path);
        cache[path] = JSON.stringify(DogmaAttributeToJSON(attribute));
    }
}

// function detectAndCacheBundle(path: string, body: any): void {
//     if (path.startsWith("/types/")) {
//         if (path.endsWith("/bundle")) {
//             var bundle = JSON.parse(JSON.stringify(body)) as Bundle; // Force unwrap Proxy object.
//             console.log("Detected bundle: ", bundle)
//             cacheBundle(bundle);
//         }
//     }
// }

const useFetchFetchApi: FetchAPI = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    const url = new URL((input as URL).toString()); // Otherwise, it doesn't parse correctly.
    const path = url.pathname;
    // Check cache.
    if (!url.search && cache[path]) {
        console.log("Cache hit for ", path, cache[path]);
        return new Response(cache[path] as any);
    }
    // Request.
    console.log("Fetching ", path);
    var response = await useFetch(url.toString());
    // // Handle bundle.
    // detectAndCacheBundle(path, response.data.value);
    // Return as normal.
    return new Response(JSON.stringify(response.data.value) as any);
}

const config = new Configuration({
    fetchApi: useFetchFetchApi
})

const refdataApi: RefdataApi = new RefdataApi(config);

export default refdataApi;

export async function cacheBundle(typeId: number): Promise<void> {
    const bundle = await refdataApi.getTypeBundle({typeId});
    if (bundle) {
        cacheBundleObj(bundle);
    }
}
