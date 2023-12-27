<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DefensesRow from "~/components/cards/defenses/DefensesRow.vue";

const {locale} = useI18n();

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

</script>

<template>
	<CardWrapper :title="title">
		<div class="grid grid-cols-7">
			<DefensesRow :inventory-type="inventoryType"
				:dogma-attributes="dogmaAttributes"
				hp-attr-name="shieldCapacity"
				uniformity-attr-name="shieldUniformity"
				em-resonance-attr-name="shieldEmDamageResonance"
				thermal-resonance-attr-name="shieldThermalDamageResonance"
				kinetic-resonance-attr-name="shieldKineticDamageResonance"
				explosive-resonance-attr-name="shieldExplosiveDamageResonance"
			/>

			<DefensesRow :inventory-type="inventoryType"
				:dogma-attributes="dogmaAttributes"
				hp-attr-name="armorHP"
				uniformity-attr-name="armorUniformity"
				em-resonance-attr-name="armorEmDamageResonance"
				thermal-resonance-attr-name="armorThermalDamageResonance"
				kinetic-resonance-attr-name="armorKineticDamageResonance"
				explosive-resonance-attr-name="armorExplosiveDamageResonance"
			/>

			<DefensesRow :inventory-type="inventoryType"
				:dogma-attributes="dogmaAttributes"
				hp-attr-name="hp"
				uniformity-attr-name="structureUniformity"
				em-resonance-attr-name="emDamageResonance"
				thermal-resonance-attr-name="thermalDamageResonance"
				kinetic-resonance-attr-name="kineticDamageResonance"
				explosive-resonance-attr-name="explosiveDamageResonance"
			/>
		</div>
	</CardWrapper>
</template>
