<script setup lang="ts">
import refdataApi from "~/refdata";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";

const {locale} = useI18n();

const attributeIds: number[] = await refdataApi.getAllDogmaAttributes();
</script>

<template>
	<div>
		<h1>Dogma attributes</h1>
		<ul>
			<li v-for="attributeId in attributeIds" :key="attributeId">
				<DogmaAttributeLink :attribute-id="attributeId"></DogmaAttributeLink>
			</li>
		</ul>
	</div>
</template>
