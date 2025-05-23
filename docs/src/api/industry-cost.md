# Industry Cost

The EVE Ref Industry Cost API aims to be a fully-featured, well-tested, and _fast_ API for calculating manufacturing,
invention, reactions, copying, and blueprint research.

**The API still in development.** See [this milestone](https://github.com/autonomouslogic/eve-ref/milestone/8) for details
on what's missing and currently not supported.
You can subscribe to individual tickets to be notified when they're updated.
Join us on [Discord](https://everef.net/discord) to discuss things as they're being put together.
Your constructive feedback is always appreciated.

The following activities are supported:
* Manufacture
* Invention

These activities are _not_ supported yet:
* Reaction
* Copying
* Research ME
* Research TE

## Motivation
Industry calculations in EVE Online are boring and meticulous to get right.
On the forums and various Discord servers, I've seen so many people asking questions about industry calculations.
Everyone is writing their own code to do it.
While there's nothing wrong with that, one of the missions of EVE Ref is to provide tools to make EVE developers' lives easier.
This API aims to do exactly that: to have a single place where this is implemented for everyone to use.
To be well-tested, the development is based on [evidence](https://github.com/autonomouslogic/eve-ref/tree/main/src/test/resources/com/autonomouslogic/everef/api/IndustryCostHandlerTest) collected from the game and carefully written tests to ensure correctness.
It's implemented as an API using HTTP and JSON, so it can be used from any language.
It's designed to be fast and can run locally, so it can be used for bulk lookups.
The output is designed to be intuitive and extensible.
You can generate a client from the spec or use it directly and be on your way, getting on with what you really want to build without
having to worry if your material counts match.

## Usage
The API lives at `https://api.everef.net/v1/industry/cost` and accepts only GET requests.

* For manufacture, supply the `product_id` parameter of an item.
* For invention, supply the `product_id` parameter of a the invented blueprint.
* For manufacture and invention of something which can come from multiple blueprints, set the `blueprint_id` as the source blueprint type ID.

See the spec for full details.

## Example
The API call below will return the cost for making 8 [Sins](https://everef.net/types/22430) with ME/TE of 4/4 at a
[Sotiyo](https://everef.net/types/35827) in low-sec with [Ship Manufacturing Efficiency](https://everef.net/types/37180)
and [Laboratory Optimization](https://everef.net/types/37183) rigs, 1.29% system cost, 2% tax, and -50% faction warfare bonus:
```
https://api.everef.net/v1/industry/cost?product_id=22430&runs=8&me=4&te=4&structure_type_id=35827&security=LOW_SEC&rig_id=37180&rig_id=37183&system_cost_bonus=-0.5&manufacturing_cost=0.0129&facility_tax=0.02
```

An this is an example output, with some material omitted for brevity:
```json
{
  "manufacturing" : {
    "22430" : {
      "product_id" : 22430,
      "runs" : 8,
      "time" : "PT194H19M33S",
      "materials" : {
        "645" : {
          "type_id" : 645,
          "quantity" : 8,
          "cost" : 1912274057.68
        },
        "21025" : {
          "type_id" : 21025,
          "quantity" : 22,
          "cost" : 624650221.14
        },
        [...]
        "11545" : {
          "type_id" : 11545,
          "quantity" : 137143,
          "cost" : 1089598392.14
        }
      },
      "estimated_item_value" : 6147769967,
      "system_cost_index" : 79306233,
      "system_cost_bonuses" : -41635772,
      "facility_tax" : 122955399,
      "scc_surcharge" : 245910799,
      "alpha_clone_tax" : 0,
      "total_job_cost" : 406536659,
      "total_material_cost" : 8487211612.94,
      "total_cost" : 8893748271.94,
      "blueprint_id" : 22431,
      "units" : 8,
      "units_per_run" : 1,
      "time_per_run" : "PT24H17M26.625S",
      "time_per_unit" : "PT24H17M26.625S",
      "total_cost_per_run" : 1111718533.99,
      "total_cost_per_unit" : 1111718533.99
    }
  },
```

## Running Locally
All data is loaded upon start.
There is no external database or other dependencies.
Simply start it on Docker and you have it available.
See [API](index.md) for instructions.

## Performance
The API has been tested on my local machine to do over 6,000 req.s at 16 ms per request.

## References
* [https://eve-industry.org/export/IndustryFormulas.pdf](https://eve-industry.org/export/IndustryFormulas.pdf)
* [https://wiki.eveuniversity.org/Manufacturing](https://wiki.eveuniversity.org/Manufacturing)
* [https://wiki.eveuniversity.org/Invention](https://wiki.eveuniversity.org/Invention)
* [https://wiki.eveuniversity.org/Reactions](https://wiki.eveuniversity.org/Reactions)
