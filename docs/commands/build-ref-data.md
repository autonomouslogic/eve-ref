# Build Ref Data

Builds and uploads the main [reference data](../refdata.md) file.

* The main coordination is handled by the `BuildRefData` CLI command.
* Data loaded from the SDE is handled by the `SdeLoader`.
* Data loaded from the ESI is handled by the `EsiLoader`.
* These classes construct specific loaders based on the type they're loading (types, groups, dogma, etc.)
* The loaders read the input files and output objects which are stored in an MVStore.
* The MVStore has a map for each type and source, for instance types/SDE and types/ESI, and so on.
* Once all the loaders have created their outputs, the command merges all these objects together to form the final output.
* To keep the final merge as dumb as possible, the loaders output objects in their final form, ready for the reference data.

## Testing

The `resources` dir in the test folder contains a few different things:

* A `refdata` dir at the root, with:
  * An `esi` dir which contains samples of actual ESI files.
  * And `sde` dir which contains samples of actual SDE files.
  * A `refdata` dir which contains the final expected format (loaded, transformed, and merged).

These resources are used by `BuildRefDataTest` for full end-to-end testing.

Additionally, in the contextual resource folders for `EsiLoaderTest` and `SdeLoaderTest`, there exists samples
for the intermediate expected output of these loaders.


## Adding more resources

To add more resources, they need to be set up in the ESI and SDE loaders.
Samples need to be added to the relevant resource directories, and tests should be added for both loaders and the main command.

Input samples should come directly from the [ESI scrape](https://data.everef.net/esi-scrape/)
or [SDE dump](https://data.everef.net/ccp/sde/).

Output samples should be constructed by hand.

Add to `PublishRefData` and `PublishRefDataTest`, as well as running `VerifyRefDataModels` to ensure models exist.

Extract all unique keys from a bunch of JSON objects:
```shell
cat dogma-attributes.json | jq '.[] | keys | .[]' | sort -u
```

Previous works:
* https://github.com/autonomouslogic/eve-ref/pull/145
