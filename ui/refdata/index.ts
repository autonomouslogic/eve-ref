import {Configuration, FetchAPI, RefdataApi} from "~/refdata-openapi";

const useFetchFetchApi: FetchAPI = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
    var url = (input as URL).toString();
    var response = await useFetch(url);
    return new Response(JSON.stringify(response.data.value) as any);
}

const config = new Configuration({
    fetchApi: useFetchFetchApi
})

const refdataApi: RefdataApi = new RefdataApi(config);

export default refdataApi;
