# Industry Cost

The EVE Ref Industry Cost API aims to be a fully-featured, well-tested, and _fast_ API for calculating manufacturing,
invention, reactions, copying, and blueprint research.
The full [OpenAPI spec](https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml) is available.

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
Let's be honest, industry calculations in EVE Online are tedious and error-prone.
I kept seeing the same questions pop up on forums and Discords and it's clear that many developers struggle with them.
Everyone is writing their own solutions from scratch.
That's fine, but it also feels like everyone's reinventing the wheel.

One of the goals of EVE Ref is to make life easier for EVE developers.
This API was built to solve that exact problem: a single, shared place where accurate industry calculations are already done for you.
It's backed by [evidence](https://github.com/autonomouslogic/eve-ref/tree/main/src/test/resources/com/autonomouslogic/everef/api/IndustryCostHandlerTest)
pulled straight from the game and tested thoroughly to make sure the results are solid.

It's a simple HTTP+JSON API, so you can use it from any language.
It's fast, runs locally if you want it to, and works great for bulk lookups.
The JSON easy to read and designed to be extensible.
Whether you generate a client from the spec or just hit it directly,
you can stop worrying about whether your materials or EIV add up and get back to building the cool stuff you actually care about.

## Usage
The API lives at `https://api.everef.net/v1/industry/cost` and accepts only GET requests.

* For manufacture, supply the `product_id` parameter of an item.
* For invention, supply the `product_id` parameter of a the invented blueprint.
* For manufacture and invention of something which can come from multiple blueprints, set the `blueprint_id` as the source blueprint type ID.

See the spec for full details.

## Manufacture Example
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
  }
}
```

## Invention Example
The example below does 10 invention runs of [Inferno Fury Cruise Missile Blueprint](https://everef.net/types/2622)
with a [Symmetry Decryptor](https://everef.net/types/34206) at a [Raitaru](https://everef.net/types/35825)
with a 0.5% tax in a high-sec  system with an 11.71% system cost index.
```
https://api.everef.net/v1/industry/cost?product_id=2622&runs=10&structure_type_id=35825&decryptor_id=34206&invention_cost=0.1171&facility_tax=0.005
```

Example output:
```json
{
  "invention" : {
    "2622": {
      "product_id": 2622,
      "runs": 10,
      "time": "PT136H40M23S",
      "materials": {
        "20418": {
          "type_id": 20418,
          "quantity": 10,
          "cost": 1043000.50
        },
        "20420": {
          "type_id": 20420,
          "quantity": 10,
          "cost": 1020634.60
        },
        "34206": {
          "type_id": 34206,
          "quantity": 10,
          "cost": 4285722.00
        }
      },
      "estimated_item_value": 16852252,
      "system_cost_index": 39468,
      "system_cost_bonuses": -1184,
      "facility_tax": 1685,
      "scc_surcharge": 13482,
      "alpha_clone_tax": 0,
      "total_job_cost": 53451,
      "total_material_cost": 6349357.10,
      "total_cost": 6402808.10,
      "blueprint_id": 805,
      "probability": 0.49583333333333335,
      "runs_per_copy": 12,
      "units_per_run": 5000,
      "expected_copies": 4.958333333333334,
      "expected_runs": 59.50000000000001,
      "expected_units": 297500.00000000006,
      "me": 3,
      "te": 12,
      "job_cost_base": 337045,
      "avg_time_per_copy": "PT27H33M51.529S",
      "avg_time_per_run": "PT2H17M49.294S",
      "avg_time_per_unit": "PT1.653S",
      "avg_cost_per_copy": 1291322.64,
      "avg_cost_per_run": 107610.22,
      "avg_cost_per_unit": 21.52
    }
  }
}
```

## Running Locally
All data is loaded upon start.
There is no external database or other dependencies.
Simply start it on Docker and you have it available.
```bash
docker run -it --rm autonomouslogic/eve-ref:latest api
```

## Performance
The API has been tested on my local machine to do over 6,000 req.s at 16 ms per request.

## References
* [https://eve-industry.org/export/IndustryFormulas.pdf](https://eve-industry.org/export/IndustryFormulas.pdf)
* [https://wiki.eveuniversity.org/Manufacturing](https://wiki.eveuniversity.org/Manufacturing)
* [https://wiki.eveuniversity.org/Invention](https://wiki.eveuniversity.org/Invention)
* [https://wiki.eveuniversity.org/Reactions](https://wiki.eveuniversity.org/Reactions)
