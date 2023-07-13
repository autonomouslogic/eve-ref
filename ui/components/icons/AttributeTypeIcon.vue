<script setup lang="ts">
import refdataApi from "~/refdata";
import {DogmaAttribute, Icon, InventoryCategory} from "~/refdata-openapi";

const ownImageServer = "https://everef.net/img";

const props = defineProps<{
	dogmaAttribute: DogmaAttribute
}>();

const iconId = props.dogmaAttribute.iconId || 0;
const icon: Icon = await refdataApi.getIcon({iconId});
let iconUrl = "";
if (icon) {
	if (icon.iconFile?.toLowerCase().startsWith("res:/ui/texture/icons/")) {
		iconUrl = ownImageServer + "/Icons/items/" + icon.iconFile.substring(22);
	}
}
</script>

<template>
	<span v-if="iconUrl">
		<img :src="iconUrl" width="25" height="25">
	</span>
	<template v-else>&nbsp;</template>
</template>
