<script setup lang="ts">
import {type DogmaAttribute, type DogmaTypeAttribute, type InventoryType} from "~/refdata-openapi";
import {getAttributeByName, getTypeAttributeByName} from "~/lib/dogmaUtils";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import ProgressBar from "~/components/helpers/ProgressBar.vue";

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

	<div v-if="emResonanceAttr && emResonance && emResonance.value !== undefined">
		<DogmaAttributeLink :attribute="emResonanceAttr">
			<AttributeTypeIcon :dogma-attribute="emResonanceAttr" />
			<ProgressBar :progress="1 - emResonance.value" color="#007bff">
				<DogmaValue :attribute="emResonanceAttr" :value="emResonance.value" />
			</ProgressBar>
		</DogmaAttributeLink>
	</div>
	<div v-else />

	<span v-if="thermalResonanceAttr && thermalResonance && thermalResonance.value !== undefined">
		<DogmaAttributeLink :attribute="thermalResonanceAttr">
			<AttributeTypeIcon :dogma-attribute="thermalResonanceAttr" />
			<ProgressBar :progress="1 - thermalResonance.value" color="#ee5f5b">
				<DogmaValue :attribute="thermalResonanceAttr" :value="thermalResonance.value" />
			</ProgressBar>
		</DogmaAttributeLink>
	</span>
	<div v-else />

	<div v-if="kineticResonanceAttr && kineticResonance && kineticResonance.value !== undefined">
		<DogmaAttributeLink :attribute="kineticResonanceAttr">
			<AttributeTypeIcon :dogma-attribute="kineticResonanceAttr" />
			<ProgressBar :progress="1 - kineticResonance.value" color="#7a8288;">
				<DogmaValue :attribute="kineticResonanceAttr" :value="kineticResonance.value" />
			</ProgressBar>
		</DogmaAttributeLink>
	</div>
	<div v-else />

	<div v-if="explosiveResonanceAttr && explosiveResonance && explosiveResonance.value !== undefined">
		<DogmaAttributeLink :attribute="explosiveResonanceAttr">
			<AttributeTypeIcon :dogma-attribute="explosiveResonanceAttr" />
			<ProgressBar :progress="1 - explosiveResonance.value" color="#f89406">
				<DogmaValue :attribute="explosiveResonanceAttr" :value="explosiveResonance.value" />
			</ProgressBar>
		</DogmaAttributeLink>
	</div>
	<div v-else />
</template>

<style scoped>
</style>
