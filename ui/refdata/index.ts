import {Configuration, FetchAPI, InventoryType, RefdataApi} from "~/refdata-openapi";

const customFetch: FetchAPI = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    var url = (input as URL).toString();
    var response = await useFetch(url);
    return new Response(JSON.stringify(response.data.value) as any);
}

const config = new Configuration({
    fetchApi: customFetch
})

const api = new RefdataApi(config);

export async function getInventoryType(typeId: number): Promise<InventoryType> {
    return api.getType({typeId})
}
