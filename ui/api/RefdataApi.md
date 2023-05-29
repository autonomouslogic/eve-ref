# .RefdataApi

All URIs are relative to *https://ref-data.everef.net*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAllDogmaAttributes**](RefdataApi.md#getAllDogmaAttributes) | **GET** /dogma_attributes | 
[**getAllTypes**](RefdataApi.md#getAllTypes) | **GET** /types | 
[**getDogmaAttribute**](RefdataApi.md#getDogmaAttribute) | **GET** /dogma_attributes/{attribute_id} | 
[**getType**](RefdataApi.md#getType) | **GET** /types/{type_id} | 


# **getAllDogmaAttributes**
> Array<number> getAllDogmaAttributes()

Get all dogma attribute IDs.

### Example


```typescript
import {  } from '';
import * as fs from 'fs';

const configuration = .createConfiguration();
const apiInstance = new .RefdataApi(configuration);

let body:any = {};

apiInstance.getAllDogmaAttributes(body).then((data:any) => {
  console.log('API called successfully. Returned data: ' + data);
}).catch((error:any) => console.error(error));
```


### Parameters
This endpoint does not need any parameter.


### Return type

**Array<number>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Dogma attribute IDs. |  -  |

[[Back to top]](#) [[Back to API list]](README.md#documentation-for-api-endpoints) [[Back to Model list]](README.md#documentation-for-models) [[Back to README]](README.md)

# **getAllTypes**
> Array<number> getAllTypes()

Get all type IDs.

### Example


```typescript
import {  } from '';
import * as fs from 'fs';

const configuration = .createConfiguration();
const apiInstance = new .RefdataApi(configuration);

let body:any = {};

apiInstance.getAllTypes(body).then((data:any) => {
  console.log('API called successfully. Returned data: ' + data);
}).catch((error:any) => console.error(error));
```


### Parameters
This endpoint does not need any parameter.


### Return type

**Array<number>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Type IDs. |  -  |

[[Back to top]](#) [[Back to API list]](README.md#documentation-for-api-endpoints) [[Back to Model list]](README.md#documentation-for-models) [[Back to README]](README.md)

# **getDogmaAttribute**
> DogmaAttribute getDogmaAttribute()


### Example


```typescript
import {  } from '';
import * as fs from 'fs';

const configuration = .createConfiguration();
const apiInstance = new .RefdataApi(configuration);

let body:.RefdataApiGetDogmaAttributeRequest = {
  // number
  attributeId: 1,
};

apiInstance.getDogmaAttribute(body).then((data:any) => {
  console.log('API called successfully. Returned data: ' + data);
}).catch((error:any) => console.error(error));
```


### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **attributeId** | [**number**] |  | defaults to undefined


### Return type

**DogmaAttribute**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The dogma attribute. |  -  |

[[Back to top]](#) [[Back to API list]](README.md#documentation-for-api-endpoints) [[Back to Model list]](README.md#documentation-for-models) [[Back to README]](README.md)

# **getType**
> InventoryType getType()


### Example


```typescript
import {  } from '';
import * as fs from 'fs';

const configuration = .createConfiguration();
const apiInstance = new .RefdataApi(configuration);

let body:.RefdataApiGetTypeRequest = {
  // number
  typeId: 1,
};

apiInstance.getType(body).then((data:any) => {
  console.log('API called successfully. Returned data: ' + data);
}).catch((error:any) => console.error(error));
```


### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **typeId** | [**number**] |  | defaults to undefined


### Return type

**InventoryType**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | The types. |  -  |

[[Back to top]](#) [[Back to API list]](README.md#documentation-for-api-endpoints) [[Back to Model list]](README.md#documentation-for-models) [[Back to README]](README.md)


