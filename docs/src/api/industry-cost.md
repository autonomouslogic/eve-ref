# Industry Cost

The EVE Ref Industry Cost API aims to be a fully-featured, well-tested, and _fast_ API for calculating manufacturing,
invention, reactions, copying, and blueprint research.

The API still in development. See [this milestone](https://github.com/autonomouslogic/eve-ref/milestone/8) for details
on what's missing and currently not supported.

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

## Motivation
Industry calculations in EVE Online are boring and meticulous to get right.
This API was designed based on [evidence](https://github.com/autonomouslogic/eve-ref/tree/main/src/test/resources/com/autonomouslogic/everef/api/IndustryCostHandlerTest).

## Fair Usage

## References
* https://eve-industry.org/export/IndustryFormulas.pdf
* https://wiki.eveuniversity.org/Manufacturing
* https://wiki.eveuniversity.org/Invention
* https://wiki.eveuniversity.org/Reactions
