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

Quick links:
* [OpenAPI spec](https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml)
* [Open issues](https://github.com/autonomouslogic/eve-ref/milestone/8)
* [Industry source code](https://github.com/autonomouslogic/eve-ref/tree/main/src/main/java/com/autonomouslogic/everef/industry)
* [Industry resources](https://github.com/autonomouslogic/eve-ref/tree/main/src/main/resources/industry)
* [Test code](https://github.com/autonomouslogic/eve-ref/blob/main/src/test/java/com/autonomouslogic/everef/api/IndustryCostHandlerTest.java)
* [Test fixtures](https://github.com/autonomouslogic/eve-ref/tree/main/src/test/resources/com/autonomouslogic/everef/api/IndustryCostHandlerTest)

## Motivation
Let's be honest, industry calculations in EVE Online are tedious and error-prone.
I kept seeing the same questions pop up on forums and Discords and it's clear that many developers struggle with them.
Everyone is writing their own solutions from scratch.
That's fine, but it also feels like everyone's reinventing the wheel.

One of the goals of EVE Ref is to make life easier for EVE developers.
This API was built to solve that exact problem: a single, shared place where accurate industry calculations are already done for you.
It's backed by evidence pulled from the game and tested thoroughly to make sure the results are solid.

It's a simple HTTP+JSON API, so you can use it from any language.
It's fast, runs locally if you want it to, and works great for bulk lookups.
The JSON is easy to read and designed to be extensible.
Whether you generate a client from the spec or just hit it directly,
you can stop worrying about whether your materials or EIV add up and get back to building the cool stuff you actually care about.

## Usage
The API lives at `https://api.everef.net/v1/industry/cost` and accepts only GET requests.

* For manufacture, supply the `product_id` parameter of an item.
* For manufacture of items which require an invented blueprint, the invention will be automatically calculated.
* For invention only, supply the `product_id` parameter of the blueprint to be invented.
* For manufacture and invention of something which can come from multiple blueprints, set the `blueprint_id` as the source blueprint type ID.
* To calculate all activities on a specific blueprint, set `blueprint_id`.

See the spec for full details.

## Manufacture Example
The API call below will return the cost for making 8 [Sins](https://everef.net/types/22430) with ME/TE of 4/4 at a
[Sotiyo](https://everef.net/types/35827) in low-sec with [Ship Manufacturing Efficiency](https://everef.net/types/37180)
and [Laboratory Optimization](https://everef.net/types/37183) rigs, 1.29% system cost, 2% tax, and -50% faction warfare bonus:
```
https://api.everef.net/v1/industry/cost?product_id=22430&runs=8&me=4&te=4&structure_type_id=35827&security=LOW_SEC&rig_id=37180&rig_id=37183&system_cost_bonus=-0.5&manufacturing_cost=0.0129&facility_tax=0.02
```

Below is an example output, with some material omitted for brevity.
Because a T2 item was requested, it includes an invention step too, which is scaled to show _on average_ how many
invention runs are needed to provide the requested manufacturing runs.
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
          "cost_per_unit" : 239034257.21,
          "cost" : 1912274057.68
        },
        "21025" : {
          "type_id" : 21025,
          "quantity" : 22,
          "cost_per_unit" : 28393191.87,
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
  "invention" : {
    "22431" : {
      "product_id" : 22431,
      "runs" : 24.935064935064936,
      "time" : "PT490H35M19.48S",
      "materials" : {
        "20410" : {
          "type_id" : 20410,
          "quantity" : 797.922077922078,
          "cost_per_unit" : 99872.85,
          "cost" : 79690752.00
        },
        "20424" : {
          "type_id" : 20424,
          "quantity" : 797.922077922078,
          "cost_per_unit" : 88643.50,
          "cost" : 70730605.71
        }
      },
      "estimated_item_value" : 19161880416.62,
      "system_cost_index" : 0.00,
      "system_cost_bonuses" : 0.00,
      "facility_tax" : 7664752.21,
      "scc_surcharge" : 15329504.42,
      "alpha_clone_tax" : 0.00,
      "total_job_cost" : 22994256.62,
      "total_material_cost" : 150421357.71,
      "total_cost" : 173415614.34,
      "blueprint_id" : 999,
      "probability" : 0.3208333333333333,
      "runs_per_copy" : 1,
      "units_per_run" : 1,
      "expected_copies" : 8.0,
      "expected_runs" : 8.0,
      "expected_units" : 8.0,
      "me" : 2,
      "te" : 4,
      "job_cost_base" : 383237607.27,
      "avg_time_per_copy" : "PT61H19M24.935S",
      "avg_time_per_run" : "PT61H19M24.935S",
      "avg_time_per_unit" : "PT61H19M24.935S",
      "avg_cost_per_copy" : 21676951.79,
      "avg_cost_per_run" : 21676951.79,
      "avg_cost_per_unit" : 21676951.79
    }
  },
  [...]
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

## Query parameters
This is a quick reference for query parameters and may not be fully up-to-date.
See [OpenAPI spec](https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml) for details and formats.

  [//]: # (cat spec/eve-ref-api.yaml | yq -o json | jq -cr '.components.schemas.IndustryCostInput.properties | to_entries | .[] | "* `" + .key + "` - " + .value.description')

* `product_id` - The desired product type ID
* `blueprint_id` - The blueprint ID to calculate
* `me` - The material efficiency of the blueprint
* `te` - The time efficiency of the blueprint
* `runs` - The number of runs
* `decryptor_id` - The decryptor type ID to use

### Structure
* `structure_type_id` - The type ID of the structure where the job is installed. If not set, an NPC station is assumed.
* `rig_id` - The type IDs of the rigs installed on the sture structure where the job is installed

### System
* `system_id` - The ID of the system where the job is installed. This will resolve security class and cost indices
* `security` - The security class of the system where the job is installed
* `manufacturing_cost` - The manufacturing cost index of the system where the job is installed
* `invention_cost` - The invention cost index of the system where the job is installed
* `copying_cost` - The copying cost index of the system where the job is installed
* `reaction_cost` - The reaction cost index of the system where the job is installed
* `researching_me_cost` - The researching material efficiency cost index of the system where the job is installed
* `researching_te_cost` - The researching time efficiency cost index of the system where the job is installed

### Skills
* `advanced_capital_ship_construction` - The Advanced Capital Ship Construction skill level the installing character
* `advanced_industrial_ship_construction` - The Advanced Industrial Ship Construction skill level the installing character
* `advanced_industry` - The Advanced Industry skill level the installing character
* `advanced_large_ship_construction` - The Advanced Large Ship Construction skill level the installing character
* `advanced_medium_ship_construction` - The Advanced Medium Ship Construction skill level the installing character
* `advanced_small_ship_construction` - The Advanced Small Ship Construction skill level the installing character
* `amarr_encryption_methods` - The Amarr Encryption Methods skill level the installing character
* `amarr_starship_engineering` - The Amarr Starship Engineering skill level the installing character
* `caldari_encryption_methods` - The Caldari Encryption Methods skill level the installing character
* `caldari_starship_engineering` - The Caldari Starship Engineering skill level the installing character
* `core_subsystem_technology` - The Core Subsystem Technology skill level the installing character
* `defensive_subsystem_technology` - The Defensive Subsystem Technology skill level the installing character
* `electromagnetic_physics` - The Electromagnetic Physics skill level the installing character
* `electronic_engineering` - The Electronic Engineering skill level the installing character
* `gallente_encryption_methods` - The Gallente Encryption Methods skill level the installing character
* `gallente_starship_engineering` - The Gallente Starship Engineering skill level the installing character
* `graviton_physics` - The Graviton Physics skill level the installing character
* `high_energy_physics` - The High Energy Physics skill level the installing character
* `hydromagnetic_physics` - The Hydromagnetic Physics skill level the installing character
* `industry` - The Industry skill level the installing character
* `laser_physics` - The Laser Physics skill level the installing character
* `mechanical_engineering` - The Mechanical Engineering skill level the installing character
* `metallurgy` - The Metallurgy skill level the installing character
* `minmatar_encryption_methods` - The Minmatar Encryption Methods skill level the installing character
* `minmatar_starship_engineering` - The Minmatar Starship Engineering skill level the installing character
* `molecular_engineering` - The Molecular Engineering skill level the installing character
* `mutagenic_stabilization` - The Mutagenic Stabilization skill level the installing character
* `nanite_engineering` - The Nanite Engineering skill level the installing character
* `nuclear_physics` - The Nuclear Physics skill level the installing character
* `offensive_subsystem_technology` - The Offensive Subsystem Technology skill level the installing character
* `plasma_physics` - The Plasma Physics skill level the installing character
* `propulsion_subsystem_technology` - The Propulsion Subsystem Technology skill level the installing character
* `quantum_physics` - The Quantum Physics skill level the installing character
* `research` - The Research skill level the installing character
* `rocket_science` - The Rocket Science skill level the installing character
* `science` - The Science skill level the installing character
* `sleeper_encryption_methods` - The Sleeper Encryption Methods skill level the installing character
* `triglavian_encryption_methods` - The Triglavian Encryption Methods skill level the installing character
* `triglavian_quantum_engineering` - The Triglavian Quantum Engineering skill level the installing character
* `upwell_encryption_methods` - The Upwell Encryption Methods skill level the installing character
* `upwell_starship_engineering` - The Upwell Starship Engineering skill level the installing character


### Other
* `alpha` - Whether installing character is an alpha clone or not
* `facility_tax` - The facility tax rate of the station or structure where the job is installed
* `material_prices` - Where to get material prices from
* `system_cost_bonus` - Bonus to apply to system cost, such as the faction warfare bonus

## Job Costs
EIV calculations should match pretty closely, but you might notice that the system cost index can be slightly off.
That's not a bug, it's just how the data from CCP works.
The ESI only gives us four decimal places of precision, even though the actual values go deeper.

For example, ESI might say the index is `0.0728`, but behind the scenes it could really be something like `0.0728049422`.
When you're installing a job, that tiny difference could change the cost by around 800 ISK on an 18 million ISK job installation cost.
That's just 0.004%.
When your material costs are over 200 million ISK, that 800 ISK doesn't really matter.

Still, it's good to know this ahead of time, so you don't end up chasing ghosts trying to find out why your numbers are off by a few hundred ISK.



## Running Locally
All data is loaded upon start.
There is no external database or other dependencies.
Simply start it on Docker and you have it available.
```bash
docker run -it --rm autonomouslogic/eve-ref:latest api
```

## Performance
The API has been tested on my local machine to do over 6,000 req/s at 16 ms per request.

## References
* [https://eve-industry.org/export/IndustryFormulas.pdf](https://eve-industry.org/export/IndustryFormulas.pdf)
* [https://wiki.eveuniversity.org/Manufacturing](https://wiki.eveuniversity.org/Manufacturing)
* [https://wiki.eveuniversity.org/Invention](https://wiki.eveuniversity.org/Invention)
* [https://wiki.eveuniversity.org/Reactions](https://wiki.eveuniversity.org/Reactions)
* [https://eve-industry.org/export/CCP_ROUND.pdf](https://eve-industry.org/export/CCP_ROUND.pdf)
