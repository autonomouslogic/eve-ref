<script setup lang="ts">
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import CategoryLink from "~/components/helpers/CategoryLink.vue";

const route = useRoute();
const {locale} = useI18n();

const groupId = getIntRouteParam(route, "groupId");

const group = await refdataApi.getGroup({groupId});
useHead({
	title: group.name?.[locale.value]
});
const typeIds = group.typeIds;
</script>

<template>
	<div>
		<h1 v-if="group.name">{{ group.name[locale] }}</h1>
		<div class="mb-3">
			Category: <CategoryLink :category-id="group.categoryId" />
		</div>
		<ul>
			<li v-for="typeId in typeIds" :key="typeId">
				<TypeLink :typeId="typeId"></TypeLink>
			</li>
		</ul>
	</div>
</template>
