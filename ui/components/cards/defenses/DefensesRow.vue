<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import {getAttributeByName, getTypeAttributeByName} from "~/lib/dogmaUtils";

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

</script>

<template>
	<template v-if="hp">
		<span>{{ hpAttr!.displayName[locale] }}</span>
		<span>{{ hp!.value }} HP</span>
		<span>uniformity:</span>
		<span>{{ (1-uniformity!.value) * 100 }}%</span>
		<span>EM:</span>
		<span>{{ Math.round((1-emResonance?.value) * 100) }}%</span>
		<span>thermal:</span>
		<span>{{ Math.round((1-thermalResonance?.value) * 100) }}%</span>
		<span>kinetic:</span>
		<span>{{ Math.round((1-kineticResonance?.value) * 100) }}%</span>
		<span>explosive:</span>
		<span>{{ Math.round((1-explosiveResonance?.value) * 100) }}%</span>
	</template>
</template>
