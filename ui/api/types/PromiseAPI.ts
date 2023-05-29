import { ResponseContext, RequestContext, HttpFile } from '../http/http';
import { Configuration} from '../configuration'

import { DogmaAttribute } from '../models/DogmaAttribute';
import { DogmaTypeAttribute } from '../models/DogmaTypeAttribute';
import { DogmaTypeEffect } from '../models/DogmaTypeEffect';
import { InventoryType } from '../models/InventoryType';
import { InventoryTypeTraits } from '../models/InventoryTypeTraits';
import { TraitBonus } from '../models/TraitBonus';
import { ObservableRefdataApi } from './ObservableAPI';

import { RefdataApiRequestFactory, RefdataApiResponseProcessor} from "../apis/RefdataApi";
export class PromiseRefdataApi {
    private api: ObservableRefdataApi

    public constructor(
        configuration: Configuration,
        requestFactory?: RefdataApiRequestFactory,
        responseProcessor?: RefdataApiResponseProcessor
    ) {
        this.api = new ObservableRefdataApi(configuration, requestFactory, responseProcessor);
    }

    /**
     * Get all dogma attribute IDs.
     */
    public getAllDogmaAttributes(_options?: Configuration): Promise<Array<number>> {
        const result = this.api.getAllDogmaAttributes(_options);
        return result.toPromise();
    }

    /**
     * Get all type IDs.
     */
    public getAllTypes(_options?: Configuration): Promise<Array<number>> {
        const result = this.api.getAllTypes(_options);
        return result.toPromise();
    }

    /**
     * @param attributeId 
     */
    public getDogmaAttribute(attributeId: number, _options?: Configuration): Promise<DogmaAttribute> {
        const result = this.api.getDogmaAttribute(attributeId, _options);
        return result.toPromise();
    }

    /**
     * @param typeId 
     */
    public getType(typeId: number, _options?: Configuration): Promise<InventoryType> {
        const result = this.api.getType(typeId, _options);
        return result.toPromise();
    }


}



