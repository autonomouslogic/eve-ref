<script setup lang="ts">
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";

const route = useRoute();
const {locale} = useI18n();

const groupId = route.params.groupId;
var group = await refdataApi.getGroup({groupId});
var typeIds = group.typeIds;
</script>

<template>
	<div>
		<h1>{{ group.name[locale] }}</h1>
		<p>Types:</p>
		<ul>
			<li v-for="typeId in typeIds" :key="typeId">
				<TypeLink :typeId="typeId"></TypeLink>
			</li>
		</ul>
	</div>
</template>
