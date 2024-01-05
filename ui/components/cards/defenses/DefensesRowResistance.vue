<script setup lang="ts">
import {type DogmaAttribute, type DogmaTypeAttribute} from "~/refdata-openapi";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import ProgressBar from "~/components/helpers/ProgressBar.vue";

const props = defineProps<{
	resonanceAttr: DogmaAttribute | undefined,
	resonance: DogmaTypeAttribute | undefined,
	color: string
}>();

</script>

<template>
	<div v-if="resonanceAttr && resonance && resonance.value !== undefined">
		<DogmaAttributeLink :attribute="resonanceAttr">
			<div class="flex">
				<div class="w-max">
					<AttributeTypeIcon :dogma-attribute="resonanceAttr" />
				</div>
				<div class="w-full flex content-center py-1">
					<ProgressBar :progress="1 - resonance.value" :color="color">
						<DogmaValue :attribute="resonanceAttr" :value="resonance.value"/>
					</ProgressBar>
				</div>
			</div>
		</DogmaAttributeLink>
	</div>
	<div v-else/>
</template>
