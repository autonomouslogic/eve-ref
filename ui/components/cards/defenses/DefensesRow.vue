<script setup lang="ts">
import {type DogmaAttribute, type DogmaTypeAttribute, type InventoryType} from "~/refdata-openapi";
import {getAttributeByName, getTypeAttributeByName} from "~/lib/dogmaUtils";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";

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

function attributeValueCalc(dogmaTypeAttribute: DogmaTypeAttribute | undefined, round: boolean = false): number | undefined {
	if (!dogmaTypeAttribute || !dogmaTypeAttribute.value) {
		return undefined;
	}
	const value = (1 - dogmaTypeAttribute.value) * 100;
	return round ? Math.round(value) : value;
}
</script>

<template>
	<template v-if="hp">
		<span v-if="hpAttr?.displayName"><AttributeTypeIcon :dogma-attribute="hpAttr" />{{ hpAttr.displayName[locale] }}</span>
		<span>{{ hp.value }} HP</span>
		<span>uniformity:</span>
		<span>{{ attributeValueCalc(uniformity) }}%</span>
		<span v-if="emResonanceAttr"><AttributeTypeIcon :dogma-attribute="emResonanceAttr" />EM:</span>
		<span>{{ attributeValueCalc(emResonance, true) }}%</span>
		<span v-if="thermalResonanceAttr"><AttributeTypeIcon :dogma-attribute="thermalResonanceAttr" />thermal:</span>
		<span>{{ attributeValueCalc(thermalResonance, true) }}%</span>
		<span v-if="kineticResonanceAttr"><AttributeTypeIcon :dogma-attribute="kineticResonanceAttr" />kinetic:</span>
		<span>{{ attributeValueCalc(kineticResonance, true) }}%</span>
		<span v-if="explosiveResonanceAttr"><AttributeTypeIcon :dogma-attribute="explosiveResonanceAttr" />explosive:</span>
		<span>{{ attributeValueCalc(explosiveResonance, true) }}%</span>
	</template>
</template>
