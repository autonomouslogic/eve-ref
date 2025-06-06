<script setup lang="ts">
import refdataApi from "~/refdata";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";

const attributeIds: number[] = await refdataApi.getAllDogmaAttributes();
useHead({
	title: "Dogma Attributes"
});
</script>

<template>
	<div>
		<h1 class="mb-3">Dogma attributes</h1>

		<ClientOnly>
			<table class="standard-table">
				<thead>
					<tr>
						<th>ID</th>
						<th>Name</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="attributeId in attributeIds" :key="attributeId">
						<td>{{ attributeId }}</td>
						<td>
							<DogmaAttributeLink :attribute="attributeId" />
						</td>
					</tr>
				</tbody>
			</table>
		</ClientOnly>
	</div>
</template>
