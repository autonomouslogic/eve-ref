<script setup lang="ts">
import {TraitBonus} from "~/refdata-openapi";
import LinkParser from "~/components/helpers/LinkParser.vue";
import UnitValue from "~/components/dogma/UnitValue.vue";

const {locale} = useI18n();

const {title, bonuses} = defineProps<{
  title: string;
  bonuses: { [key: string]: TraitBonus; } | undefined;
}>();

const hasBonuses: boolean = bonuses !== undefined && Object.keys(bonuses).length > 0;

</script>

<template>
  <div v-if="hasBonuses">
    <h3>{{ title }}</h3>
    <ul>
      <li v-for="trait in bonuses">
        <UnitValue :value="trait.bonus" :unit-id="trait.unitId" /> <LinkParser :content="trait.bonusText[locale]"/>
      </li>
    </ul>
  </div>
</template>
