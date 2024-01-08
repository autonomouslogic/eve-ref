<script setup lang="ts">
import refdataApi from "~/refdata";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import {getIntRouteParam} from "~/lib/routeUtils";

const route = useRoute();
const {locale} = useI18n();

const attributeId = getIntRouteParam(route, "attributeId");
const attribute = await refdataApi.getDogmaAttribute({attributeId});
const unit = attribute.unitId ? await refdataApi.getUnit({unitId: attribute.unitId}) : null;

console.log(route.params);
console.log(attributeId);
console.log(attribute);
console.log(unit);
</script>

<template>
	<div v-if="!attribute">Unknown dogma attribute ID {{ attributeId }}</div>
	<div v-else>
		<h1 v-if="attribute.displayName">
			<AttributeTypeIcon :dogma-attribute="attribute" :size="50" />
			{{ attribute.displayName[locale] }}
		</h1>
		<p v-if="attribute.description">{{ attribute.description[locale] }}</p>
		<CardsContainer>
			<CardWrapper title="Dogma attribute">
				<ul>
					<li>Attribute ID: {{ attribute.attributeId }}</li>
					<li>Name: {{ attribute.name }}</li>
					<li>Category ID: {{ attribute.categoryId }}</li>
					<li>Data type: {{ attribute.dataType }}</li>
					<li>Default value: {{ attribute.defaultValue }}</li>
					<li>High is good: {{ attribute.highIsGood }}</li>
					<li>Icon ID: {{ attribute.iconId }}</li>
					<li>Published: {{ attribute.published }}</li>
					<li>Stackable: {{ attribute.stackable }}</li>
					<li>Unit ID: {{ attribute.unitId }}</li>
				</ul>
			</CardWrapper>
			<CardWrapper v-if="unit" title="Unit">
				<ul>
					<li>Unit ID: {{ unit.unitId }}</li>
					<li v-if="unit.name">Name: {{ unit.name[locale] }}</li>
					<li v-if="unit.description">Description: {{ unit.description[locale] }}</li>
					<li>Display name: {{ unit.displayName }}</li>
				</ul>
			</CardWrapper>
		</CardsContainer>
	</div>
</template>
