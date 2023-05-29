import { ResponseContext, RequestContext, HttpFile } from '../http/http';
import { Configuration} from '../configuration'

import { DogmaAttribute } from '../models/DogmaAttribute';
import { DogmaTypeAttribute } from '../models/DogmaTypeAttribute';
import { DogmaTypeEffect } from '../models/DogmaTypeEffect';
import { InventoryType } from '../models/InventoryType';
import { InventoryTypeTraits } from '../models/InventoryTypeTraits';
import { TraitBonus } from '../models/TraitBonus';

import { ObservableRefdataApi } from "./ObservableAPI";
import { RefdataApiRequestFactory, RefdataApiResponseProcessor} from "../apis/RefdataApi";

export interface RefdataApiGetAllDogmaAttributesRequest {
}

export interface RefdataApiGetAllTypesRequest {
}

export interface RefdataApiGetDogmaAttributeRequest {
    /**
     * 
     * @type number
     * @memberof RefdataApigetDogmaAttribute
     */
    attributeId: number
}

export interface RefdataApiGetTypeRequest {
    /**
     * 
     * @type number
     * @memberof RefdataApigetType
     */
    typeId: number
}

export class ObjectRefdataApi {
    private api: ObservableRefdataApi

    public constructor(configuration: Configuration, requestFactory?: RefdataApiRequestFactory, responseProcessor?: RefdataApiResponseProcessor) {
        this.api = new ObservableRefdataApi(configuration, requestFactory, responseProcessor);
    }

    /**
     * Get all dogma attribute IDs.
     * @param param the request object
     */
    public getAllDogmaAttributes(param: RefdataApiGetAllDogmaAttributesRequest = {}, options?: Configuration): Promise<Array<number>> {
        return this.api.getAllDogmaAttributes( options).toPromise();
    }

    /**
     * Get all type IDs.
     * @param param the request object
     */
    public getAllTypes(param: RefdataApiGetAllTypesRequest = {}, options?: Configuration): Promise<Array<number>> {
        return this.api.getAllTypes( options).toPromise();
    }

    /**
     * @param param the request object
     */
    public getDogmaAttribute(param: RefdataApiGetDogmaAttributeRequest, options?: Configuration): Promise<DogmaAttribute> {
        return this.api.getDogmaAttribute(param.attributeId,  options).toPromise();
    }

    /**
     * @param param the request object
     */
    public getType(param: RefdataApiGetTypeRequest, options?: Configuration): Promise<InventoryType> {
        return this.api.getType(param.typeId,  options).toPromise();
    }

}
