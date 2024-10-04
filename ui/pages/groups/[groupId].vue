<script setup lang="ts">
import refdataApi from "~/refdata";
import {getIntRouteParam} from "~/lib/routeUtils";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import {getGroupDogma} from "~/lib/mainDogmaAttributes";
import {tr} from "~/lib/translate";

const route = useRoute();
const {locale} = useI18n();

const groupId = getIntRouteParam(route, "groupId");

const group = await refdataApi.getGroup({groupId});
useHead({
	title: tr(group.name, locale.value)
});
const typeIds: number[] = group.typeIds?.filter((typeId) => typeId !== undefined) as number[];
const attributes = await getGroupDogma(groupId);
</script>

<template>
	<div>
		<h1 v-if="group.name">{{ tr(group.name, locale) }}p</h1>
		<div class="mb-3">
			Category: <CategoryLink :category-id="group.categoryId" />
		</div>
		<CompareTable
			:type-ids="typeIds"
			:dogma-attribute-names="attributes || []"
			direction="vertical"
			:compact-attribute-names="true"
			:show-meta-group="true"/>
	</div>
</template>
