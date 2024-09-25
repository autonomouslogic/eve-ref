<script setup lang="ts">
import refdataApi from "~/refdata";
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import {STRUCTURE_COMPARISON_ENGINEERING_COMPLEX_DOGMA_NAMES} from "~/lib/structureConstants";
import {prepMessages} from "~/lib/translate";
import {STANDARD_ORES} from "~/lib/marketGroupConstants";
import TypeLink from "~/components/helpers/TypeLink.vue";

const {locale} = useI18n();

useHead({
  title: "Mining Overview"
});
useSeoMeta({
  description: "An overview of all the ore, ice, and gas values."
})

const marketGroupId = STANDARD_ORES;
const marketGroup = await refdataApi.getMarketGroup({marketGroupId});
const typeIds = marketGroup.typeIds;
if (!typeIds) {
  throw new Error(`Market group ${marketGroupId} has no type IDs`);
}
</script>

<template>
  <h1>
    <MarketGroupName :market-group-id="marketGroupId" />
  </h1>
  <table>
    <thead>
    <th>Ore</th>
    </thead>
    <tbody>
    <tr v-for="typeId in typeIds" :key="typeId">
      <td><TypeLink :type-id="typeId" /></td>
    </tr>
    </tbody>
  </table>
</template>
