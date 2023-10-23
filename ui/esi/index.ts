import {Configuration, FetchAPI, InsuranceApi, MarketApi, UniverseApi} from "~/esi-openapi";

const useFetchFetchApi: FetchAPI = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    var url = (input as URL).toString();
    var response = await useFetch(url);
    return new Response(JSON.stringify(response.data.value) as any);
}

const config = new Configuration({
    fetchApi: useFetchFetchApi
})

export const marketApi = new MarketApi(config);
export const universeApi = new UniverseApi(config);
export const insuranceApi = new InsuranceApi(config);
