<script setup lang="ts">
import refdataApi from "~/refdata";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const route = useRoute();
const {locale} = useI18n();

const attributeId = getIntRouteParam(route, "attributeId");
const attribute = await refdataApi.getDogmaAttribute({attributeId});
const unit = attribute.unitId ? await refdataApi.getUnit({unitId: attribute.unitId}) : null;

useHead({
	title: tr(attribute.displayName, locale.value)
});
if (attribute.description) {
	useSeoMeta({
		ogDescription: tr(attribute.displayName, locale.value)
	});
}
</script>

<template>
	<div v-if="!attribute">Unknown dogma attribute ID {{ attributeId }}</div>
	<div v-else>
		<h1 v-if="attribute.displayName">
			<AttributeTypeIcon :dogma-attribute="attribute" :size="50" />
			{{ tr(attribute.displayName, locale) }}
		</h1>
		<p v-if="attribute.description">{{ tr(attribute.description, locale) }}</p>
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
					<li>
						<ExternalLink :url="`https://ref-data.everef.net/dogma_attributes/${attribute.attributeId}`">Reference Data JSON</ExternalLink>
					</li>
					<li>
						<ExternalLink :url="`https://esi.evetech.net/latest/dogma/attributes/${attribute.attributeId}`">ESI JSON</ExternalLink>
					</li>
					<li>
						<ExternalLink :url="`https://sde.jita.space/latest/dogma/attributes/${attribute.attributeId}`">SDE JSON</ExternalLink>
					</li>
				</ul>
			</CardWrapper>
			<CardWrapper v-if="unit" title="Unit">
				<ul>
					<li>Unit ID: {{ unit.unitId }}</li>
					<li v-if="unit.name">Name: {{ tr(unit.name, locale) }}</li>
					<li v-if="unit.description">Description: {{ tr(unit.description, locale) }}</li>
					<li>Display name: {{ unit.displayName }}</li>
				</ul>
			</CardWrapper>
		</CardsContainer>
	</div>
</template>
