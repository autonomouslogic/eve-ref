<script setup lang="ts">
import TypeLink from "~/components/helpers/TypeLink.vue";
import TypeLinkService from "~/services/typelink.service";

const typeLinkService = new TypeLinkService();

const props = defineProps<{
	content: string;
}>();

const result = typeLinkService.parse(props.content);

</script>

<template>
	<template v-for="(item, i) in (typeLinkService.parse(content))" :key="i">
		<TypeLink :type-id="item" v-if="typeof item === 'number'"/>
		<template v-else>{{ item }}</template>
	</template>
</template>
