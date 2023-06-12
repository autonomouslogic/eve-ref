<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryTypeTraits} from "~/refdata-openapi";

const {traits} = defineProps<{
  traits: InventoryTypeTraits | undefined
}>();

const {locale} = useI18n();

const typeBonuses = traits?.types;
const roleBonuses = traits?.roleBonuses;
const miscBonuses = traits?.miscBonuses;

for (const typeId in typeBonuses) {
  const type = await refdataApi.getType({typeId});
  console.log(type.name);
}

</script>

<template>
  <div>
    <h3>Type Bonuses</h3>
    <ul>
      <li v-for="(trait, index) in typeBonuses" :key="index">
        {{ index }}
      </li>
    </ul>

    <h3>Role Bonuses</h3>
    <ul>
      <li v-for="(trait, index) in roleBonuses" :key="index">
        {{ trait.bonus }} {{ trait.unitId }} {{ trait.bonusText[locale] }}
      </li>
    </ul>

  </div>
</template>

<style scoped>

</style>