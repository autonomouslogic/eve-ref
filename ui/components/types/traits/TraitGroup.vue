<script setup lang="ts">
import {TraitBonus, TraitBonusFromJSON} from "~/refdata-openapi";

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
        {{ trait.bonus }} {{ trait.unitId }} {{ trait.bonusText[locale] }}
      </li>
    </ul>
  </div>
</template>
