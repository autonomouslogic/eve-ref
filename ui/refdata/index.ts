import {
    type Bundle,
    Configuration,
    DogmaAttributeToJSON,
    type FetchAPI,
    IconToJSON, InventoryCategoryToJSON, InventoryGroupToJSON,
    InventoryTypeToJSON, MarketGroupToJSON, MetaGroupToJSON,
    RefdataApi,
    SkillToJSON,
    UnitToJSON
} from "~/refdata-openapi";

const cache: { [key: string]: string; } = {};

function cacheBundleObj(bundle: Bundle): void {
    for (let typeId in bundle.types) {
        const type = bundle.types[typeId];
        const path = "/types/" + typeId;
        cache[path] = JSON.stringify(InventoryTypeToJSON(type));
    }
    for (let attributeId in bundle.dogmaAttributes) {
        const attribute = bundle.dogmaAttributes[attributeId];
        const path = "/dogma_attributes/" + attributeId;
        cache[path] = JSON.stringify(DogmaAttributeToJSON(attribute));
    }
    for (let skillId in bundle.skills) {
        const skill = bundle.skills[skillId];
        const path = "/skills/" + skillId;
        cache[path] = JSON.stringify(SkillToJSON(skill));
    }
    for (let unitId in bundle.units) {
        const unit = bundle.units[unitId];
        const path = "/units/" + unitId;
        cache[path] = JSON.stringify(UnitToJSON(unit));
    }
    for (let iconId in bundle.icons) {
        const icon = bundle.icons[iconId];
        const path = "/icons/" + iconId;
        cache[path] = JSON.stringify(IconToJSON(icon));
    }
    for (let marketGroupId in bundle.marketGroups) {
        const marketGroup = bundle.marketGroups[marketGroupId];
        const path = "/market_groups/" + marketGroupId;
        cache[path] = JSON.stringify(MarketGroupToJSON(marketGroup));
    }
    for (let categoryId in bundle.categories) {
        const category = bundle.categories[categoryId];
        const path = "/categories/" + categoryId;
        cache[path] = JSON.stringify(InventoryCategoryToJSON(category));
    }
    for (let groupId in bundle.groups) {
        const group = bundle.groups[groupId];
        const path = "/groups/" + groupId;
        cache[path] = JSON.stringify(InventoryGroupToJSON(group));
    }
    for (let groupId in bundle.metaGroups) {
        const group = bundle.metaGroups[groupId];
        const path = "/meta_groups/" + groupId;
        cache[path] = JSON.stringify(MetaGroupToJSON(group));
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
        // console.log("Cache hit for ", path);
        return new Response(cache[path] as any);
    }
    // Request.
    //console.log("Fetching ", path);
    var response = await useFetch(url.toString());
    var json = JSON.stringify(response.data.value);
    // Cache value.
    if (!url.search) {
        cache[path] = json;
    }
    // Return as normal.
    return new Response(json);
}

const config = new Configuration({
    fetchApi: useFetchFetchApi
})

const refdataApi: RefdataApi = new RefdataApi(config);

export default refdataApi;

export async function cacheTypeBundle(typeId: number): Promise<void> {
    const bundle = await refdataApi.getTypeBundle({typeId});
    if (bundle) {
        cacheBundleObj(bundle);
    }
}

export async function cacheGroupBundle(groupId: number): Promise<void> {
    const bundle = await refdataApi.getGroupBundle({groupId});
    if (bundle) {
        cacheBundleObj(bundle);
    }
}
