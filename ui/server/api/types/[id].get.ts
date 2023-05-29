import {Type} from "../../../types/type";
import {H3Event} from "h3";

export default defineEventHandler(async (event: H3Event): Type => {
    useFetch(`https://ref-data.everef.net/types/${event.context.params.id}`);
});