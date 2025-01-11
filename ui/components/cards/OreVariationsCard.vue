<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const oreMetaLevels: { [key: string]: string } = {
	"0": "Mission/NPE Ore",
	"1": "Standard Ore/Ice",
	"2": "+5% Ore",
	"3": "+10% Ore",
	"4": "High Quality Ice or Extracted Ore",
	"5": "Jackpot Moon Ore",
};

</script>

<template>
	<template v-if="inventoryType.oreVariations">
		<CardWrapper :title="title">
			<div v-for="(typeIds, metaLevel) in inventoryType.oreVariations" :key="metaLevel">
				<p v-if="metaLevel in oreMetaLevels">{{ oreMetaLevels[metaLevel] }}</p>
				<p v-else>Ore meta level {{ metaLevel }}</p>
				<div v-for="typeId in typeIds" :key="typeId">
					<TypeLink :typeId="typeId" />
				</div>
			</div>
		</CardWrapper>
	</template>
</template>
