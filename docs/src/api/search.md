# Search

The EVE Ref Search API lets you search for EVE Online entities by name or numeric ID.
The full [OpenAPI spec](https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml) is available.

Quick links:
* [OpenAPI spec](https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml)
* [Source code](https://github.com/autonomouslogic/eve-ref/blob/main/src/main/java/com/autonomouslogic/everef/api/SearchHandler.java)
* [Test code](https://github.com/autonomouslogic/eve-ref/blob/main/src/test/java/com/autonomouslogic/everef/api/SearchHandlerTest.java)

## Usage

```
GET https://api.everef.net/v1/search?q={query}
```

### Query parameters

* `q` (**required**) — search query, minimum 3 non-whitespace characters. Exactly one `q` parameter must be provided.

### Behaviour
* Matching is case-insensitive and partial
* Multiple words are all required to match
* Numeric queries match by entity ID
* Results are sorted by relevance score (lower is better)
* Responses are cached for 10 minutes

### Searchable entity types
The following entries are searchable:
* Inventory types
* Market groups
* Categories
* Groups

## Example
Search for "Tritanium":
```
https://api.everef.net/v1/search?q=Tritanium
```

Response:
```json
{
  "entries" : [ {
    "id" : 34,
    "language" : "en",
    "relevance" : 9,
    "title" : "Tritanium",
    "type" : "inventory_type",
    "type_name" : "Manufacture & Research",
    "urls" : {
      "everef" : "https://everef.net/types/34",
      "reference_data" : "https://ref-data.everef.net/types/34"
    }
  }, {
    "id" : 82579,
    "language" : "en",
    "relevance" : 29,
    "title" : "Tritanium Prospecting Array 1",
    "type" : "inventory_type",
    "type_name" : "Structures",
    "urls" : {
      "everef" : "https://everef.net/types/82579",
      "reference_data" : "https://ref-data.everef.net/types/82579"
    }
  },
  {
    ...
  }
}
```

See the [OpenAPI spec](https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml) for documentation of individual fields.

Partial search for "Trit":
```
https://api.everef.net/v1/search?q=Trit
```

Multi-word search for "Mjolnir Fury":
```
https://api.everef.net/v1/search?q=Mjolnir+Fury
```

Search by ID:
```
https://api.everef.net/v1/search?q=34
```
