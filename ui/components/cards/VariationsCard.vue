<script setup lang="ts">
import {DogmaAttribute, InventoryType, MetaGroup} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";

const {locale} = useI18n();

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const metaGroups: {[key: string]: MetaGroup } = {};
if (props.inventoryType.typeVariations) {
  const promises = [];
	for (const metaGroupId of Object.keys(props.inventoryType.typeVariations)) {
		promises.push((async () => {
      metaGroups[metaGroupId] = await refdataApi.getMetaGroup({metaGroupId: parseInt(metaGroupId)});
		})());
	}
	await Promise.all(promises);
}

</script>

<template>
	<template v-if="inventoryType.typeVariations && metaGroups">
		<CardWrapper :title="title">
			<template v-for="(variations, metaGroupId) in inventoryType.typeVariations" :key="metaGroupId">
				<h3 class="text-base">{{ metaGroups[metaGroupId].name[locale] }}</h3>
				<ul>
					<li v-for="typeId in variations" :key="typeId">
						<TypeLink :type-id="typeId" />
					</li>
				</ul>
			</template>
		</CardWrapper>
	</template>
</template>
