<script setup lang="ts">
import {Blueprint} from "~/refdata-openapi";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const props = defineProps<{
	blueprint: Blueprint,
}>();

const links = {
	"Fuzzwork": "https://www.fuzzwork.co.uk/blueprint/?typeid="
};

</script>

<template>
	<div v-if="blueprint">
		View on:
		<ul>
			<li v-for="(link, name) in links" :key="name">
				<ExternalLink :url="`${link}${blueprint.blueprintTypeId}`">{{name}}</ExternalLink>
			</li>
		</ul>
	</div>
</template>
