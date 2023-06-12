<script setup lang="ts">
import refdataApi from "~/refdata";
import {InventoryTypeTraits} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";

const {traits} = defineProps<{
  traits: InventoryTypeTraits | undefined
}>();

const {locale} = useI18n();

const typeBonuses = traits?.types;
const roleBonuses = traits?.roleBonuses;
const miscBonuses = traits?.miscBonuses;

</script>

<template>
  <div>
    <h3>Type Bonuses</h3>
    <div v-for="(traits, index) in typeBonuses" :key="index">
      <TypeLink :type-id="index" /> bonuses per level:
      <ul>
        <li v-for="trait in traits">
          {{ trait.bonus }} {{ trait.unitId }} {{ trait.bonus_text[locale] }}
        </li>
      </ul>
    </div>

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