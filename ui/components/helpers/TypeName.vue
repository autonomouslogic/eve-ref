<script setup lang="ts">
import refdataApi from "~/refdata";
import {type InventoryType} from "~/refdata-openapi";
import {tr} from "~/lib/translate";
import InternalLink from "~/components/helpers/InternalLink.vue";

const {locale} = useI18n();

const props = defineProps<{
	typeId: number | string | undefined
}>();

if (props.typeId === undefined) {
	throw new Error("typeId is required");
}

const typeId: number = typeof props.typeId === "string" ? parseInt(props.typeId) : props.typeId;

const type: InventoryType | undefined = props.typeId ? await refdataApi.getType({typeId}) : undefined;
</script>

<template>
	<template
		v-if="type && type.name">
		{{ tr(type.name, locale) }}
	</template>
</template>
