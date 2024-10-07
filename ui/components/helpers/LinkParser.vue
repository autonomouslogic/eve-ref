<script setup lang="ts">
import TypeLink from "~/components/helpers/TypeLink.vue";
import TypeLinkService from "~/services/typelink.service";

const typeLinkService = new TypeLinkService();

const props = defineProps<{
	content: string;
}>();

</script>

<template>
	<template v-for="(item, i) in (typeLinkService.parse(content))" :key="i">
		<TypeLink :type-id="item" v-if="typeof item === 'number'"/>
		<span v-else v-html="item"></span>
	</template>
</template>
