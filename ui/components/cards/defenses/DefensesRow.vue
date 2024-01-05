<script setup lang="ts">
import {type DogmaAttribute, type DogmaTypeAttribute, type InventoryType} from "~/refdata-openapi";
import {getAttributeByName, getTypeAttributeByName} from "~/lib/dogmaUtils";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import DefensesRowResistance from "~/components/cards/defenses/DefensesRowResistance.vue";

const {locale} = useI18n();

const props = defineProps<{
	hpAttrName: string,
	uniformityAttrName: string,
	emResonanceAttrName: string,
	thermalResonanceAttrName: string,
	kineticResonanceAttrName: string,
	explosiveResonanceAttrName: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const hpAttr = getAttributeByName(props.hpAttrName, props.dogmaAttributes);
const hp = getTypeAttributeByName(props.hpAttrName, props.inventoryType, props.dogmaAttributes);

const uniformityAttr = getAttributeByName(props.uniformityAttrName, props.dogmaAttributes);
const uniformity = getTypeAttributeByName(props.uniformityAttrName, props.inventoryType, props.dogmaAttributes);

const emResonanceAttr = getAttributeByName(props.emResonanceAttrName, props.dogmaAttributes);
const emResonance = getTypeAttributeByName(props.emResonanceAttrName, props.inventoryType, props.dogmaAttributes);

const thermalResonanceAttr = getAttributeByName(props.thermalResonanceAttrName, props.dogmaAttributes);
const thermalResonance = getTypeAttributeByName(props.thermalResonanceAttrName, props.inventoryType, props.dogmaAttributes);

const kineticResonanceAttr = getAttributeByName(props.kineticResonanceAttrName, props.dogmaAttributes);
const kineticResonance = getTypeAttributeByName(props.kineticResonanceAttrName, props.inventoryType, props.dogmaAttributes);

const explosiveResonanceAttr = getAttributeByName(props.explosiveResonanceAttrName, props.dogmaAttributes);
const explosiveResonance = getTypeAttributeByName(props.explosiveResonanceAttrName, props.inventoryType, props.dogmaAttributes);

function uniformityDisplay(value: number) {
	return Math.round((1 - value) * 100);
}

function attributeValueCalc(dogmaTypeAttribute: DogmaTypeAttribute | undefined, round: boolean = false): number | undefined {
	if (!dogmaTypeAttribute || !dogmaTypeAttribute.value) {
		return undefined;
	}
	const value = (1 - dogmaTypeAttribute.value) * 100;
	return round ? Math.round(value) : value;
}
</script>

<template>
	<div v-if="hpAttr && hp">
		<DogmaAttributeLink :attribute="hpAttr">
			<AttributeTypeIcon :dogma-attribute="hpAttr" />
			<span v-if="hpAttr.displayName">{{ hpAttr.displayName[locale] }}</span>
		</DogmaAttributeLink>
	</div>
	<div v-else />

	<div v-if="hpAttr && hp && hp.value !== undefined" class="text-right">
		<DogmaValue :attribute="hpAttr" :value="hp.value" />
	</div>
	<div v-else />

	<div v-if="uniformityAttr && uniformity && uniformity.value !== undefined" class="text-right w-min">
		<DogmaAttributeLink :attribute="uniformityAttr">
			({{ uniformityDisplay(uniformity.value) }}%)
		</DogmaAttributeLink>
	</div>
	<div v-else />

	<DefensesRowResistance :resonance-attr="emResonanceAttr" :resonance="emResonance" color="#007bff" />
	<DefensesRowResistance :resonance-attr="thermalResonanceAttr" :resonance="thermalResonance" color="#ee5f5b" />
	<DefensesRowResistance :resonance-attr="kineticResonanceAttr" :resonance="kineticResonance" color="#7a8288" />
	<DefensesRowResistance :resonance-attr="explosiveResonanceAttr" :resonance="explosiveResonance" color="#f89406" />
</template>

