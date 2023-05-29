import { ResponseContext, RequestContext, HttpFile } from '../http/http';
import { Configuration} from '../configuration'
import { Observable, of, from } from '../rxjsStub';
import {mergeMap, map} from  '../rxjsStub';
import { DogmaAttribute } from '../models/DogmaAttribute';
import { DogmaTypeAttribute } from '../models/DogmaTypeAttribute';
import { DogmaTypeEffect } from '../models/DogmaTypeEffect';
import { InventoryType } from '../models/InventoryType';
import { InventoryTypeTraits } from '../models/InventoryTypeTraits';
import { TraitBonus } from '../models/TraitBonus';

import { RefdataApiRequestFactory, RefdataApiResponseProcessor} from "../apis/RefdataApi";
export class ObservableRefdataApi {
    private requestFactory: RefdataApiRequestFactory;
    private responseProcessor: RefdataApiResponseProcessor;
    private configuration: Configuration;

    public constructor(
        configuration: Configuration,
        requestFactory?: RefdataApiRequestFactory,
        responseProcessor?: RefdataApiResponseProcessor
    ) {
        this.configuration = configuration;
        this.requestFactory = requestFactory || new RefdataApiRequestFactory(configuration);
        this.responseProcessor = responseProcessor || new RefdataApiResponseProcessor();
    }

    /**
     * Get all dogma attribute IDs.
     */
    public getAllDogmaAttributes(_options?: Configuration): Observable<Array<number>> {
        const requestContextPromise = this.requestFactory.getAllDogmaAttributes(_options);

        // build promise chain
        let middlewarePreObservable = from<RequestContext>(requestContextPromise);
        for (let middleware of this.configuration.middleware) {
            middlewarePreObservable = middlewarePreObservable.pipe(mergeMap((ctx: RequestContext) => middleware.pre(ctx)));
        }

        return middlewarePreObservable.pipe(mergeMap((ctx: RequestContext) => this.configuration.httpApi.send(ctx))).
            pipe(mergeMap((response: ResponseContext) => {
                let middlewarePostObservable = of(response);
                for (let middleware of this.configuration.middleware) {
                    middlewarePostObservable = middlewarePostObservable.pipe(mergeMap((rsp: ResponseContext) => middleware.post(rsp)));
                }
                return middlewarePostObservable.pipe(map((rsp: ResponseContext) => this.responseProcessor.getAllDogmaAttributes(rsp)));
            }));
    }

    /**
     * Get all type IDs.
     */
    public getAllTypes(_options?: Configuration): Observable<Array<number>> {
        const requestContextPromise = this.requestFactory.getAllTypes(_options);

        // build promise chain
        let middlewarePreObservable = from<RequestContext>(requestContextPromise);
        for (let middleware of this.configuration.middleware) {
            middlewarePreObservable = middlewarePreObservable.pipe(mergeMap((ctx: RequestContext) => middleware.pre(ctx)));
        }

        return middlewarePreObservable.pipe(mergeMap((ctx: RequestContext) => this.configuration.httpApi.send(ctx))).
            pipe(mergeMap((response: ResponseContext) => {
                let middlewarePostObservable = of(response);
                for (let middleware of this.configuration.middleware) {
                    middlewarePostObservable = middlewarePostObservable.pipe(mergeMap((rsp: ResponseContext) => middleware.post(rsp)));
                }
                return middlewarePostObservable.pipe(map((rsp: ResponseContext) => this.responseProcessor.getAllTypes(rsp)));
            }));
    }

    /**
     * @param attributeId 
     */
    public getDogmaAttribute(attributeId: number, _options?: Configuration): Observable<DogmaAttribute> {
        const requestContextPromise = this.requestFactory.getDogmaAttribute(attributeId, _options);

        // build promise chain
        let middlewarePreObservable = from<RequestContext>(requestContextPromise);
        for (let middleware of this.configuration.middleware) {
            middlewarePreObservable = middlewarePreObservable.pipe(mergeMap((ctx: RequestContext) => middleware.pre(ctx)));
        }

        return middlewarePreObservable.pipe(mergeMap((ctx: RequestContext) => this.configuration.httpApi.send(ctx))).
            pipe(mergeMap((response: ResponseContext) => {
                let middlewarePostObservable = of(response);
                for (let middleware of this.configuration.middleware) {
                    middlewarePostObservable = middlewarePostObservable.pipe(mergeMap((rsp: ResponseContext) => middleware.post(rsp)));
                }
                return middlewarePostObservable.pipe(map((rsp: ResponseContext) => this.responseProcessor.getDogmaAttribute(rsp)));
            }));
    }

    /**
     * @param typeId 
     */
    public getType(typeId: number, _options?: Configuration): Observable<InventoryType> {
        const requestContextPromise = this.requestFactory.getType(typeId, _options);

        // build promise chain
        let middlewarePreObservable = from<RequestContext>(requestContextPromise);
        for (let middleware of this.configuration.middleware) {
            middlewarePreObservable = middlewarePreObservable.pipe(mergeMap((ctx: RequestContext) => middleware.pre(ctx)));
        }

        return middlewarePreObservable.pipe(mergeMap((ctx: RequestContext) => this.configuration.httpApi.send(ctx))).
            pipe(mergeMap((response: ResponseContext) => {
                let middlewarePostObservable = of(response);
                for (let middleware of this.configuration.middleware) {
                    middlewarePostObservable = middlewarePostObservable.pipe(mergeMap((rsp: ResponseContext) => middleware.post(rsp)));
                }
                return middlewarePostObservable.pipe(map((rsp: ResponseContext) => this.responseProcessor.getType(rsp)));
            }));
    }

}
