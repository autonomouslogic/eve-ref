<script setup lang="ts">
import refdataApi from "~/refdata";
import {DogmaAttribute, Icon} from "~/refdata-openapi";

const ownImageServer = "https://everef.net/img";

const props = defineProps<{
	dogmaAttribute: DogmaAttribute,
	size?: number
}>();

const realSize = props.size || 25;
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
	<img v-if="iconUrl" :src="iconUrl" :width="realSize" :height="realSize" />
	<template v-else>&nbsp;</template>
</template>

<style scoped>
img {
  display: inline-block;
}
</style>
