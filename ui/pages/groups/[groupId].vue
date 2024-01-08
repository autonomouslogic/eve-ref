<script setup lang="ts">
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {getIntRouteParam} from "~/lib/routeUtils";

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
		<p>Types:</p>
		<ul>
			<li v-for="typeId in typeIds" :key="typeId">
				<TypeLink :typeId="typeId"></TypeLink>
			</li>
		</ul>
	</div>
</template>
