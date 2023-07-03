<script setup lang="ts">
import TypeLink from "~/components/helpers/TypeLink.vue";

const props = defineProps<{
	content: string;
}>();

const splitByATags = props.content.split(/(<a.*?<\/a>)/g);

const result = [];
splitByATags.forEach(item => {
	if (item.startsWith("<a")) {
		const match = item.match(/<a.*?(\d+)[^\d]/);
		if (match) {
			result.push(parseInt(match[1])); // Number from the tag
		}
	} else {
		result.push(item);
	}
});

</script>

<template>
	<span v-for="(item, i) in result" :key="i">
		<TypeLink :type-id="item" v-if="typeof item === 'number'"/>
		<span v-else>{{ item }}</span>
	</span>
</template>
