<script setup lang="ts">
import refdataApi from "~/refdata";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";

const attributeIds: number[] = await refdataApi.getAllDogmaAttributes();
useHead({
	title: "Dogma Attributes"
});
</script>

<template>
	<div>
		<h1>Dogma attributes</h1>
		<div class="flex flex-col">
			<DogmaAttributeLink
				class="py-2"
				v-for="attributeId in attributeIds"
				:key="attributeId"
				:attribute="attributeId">
			</DogmaAttributeLink>
		</div>
	</div>
</template>
