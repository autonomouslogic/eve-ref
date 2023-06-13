<script setup lang="ts">
import {InventoryTypeTraits, TraitBonus} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import TraitGroup from "~/components/types/traits/TraitGroup.vue";
import TypeTraits from "~/components/types/traits/TypeTraits.vue";

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
    <TypeTraits :bonuses="typeBonuses"/>
    <TraitGroup title="Role Bonuses" :bonuses="roleBonuses"/>
    <TraitGroup title="Misc Bonuses" :bonuses="miscBonuses"/>
  </div>
</template>

<style scoped>

</style>