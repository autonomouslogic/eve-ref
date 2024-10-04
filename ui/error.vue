<script setup lang="ts">
import InternalLink from "~/components/helpers/InternalLink.vue";

const error = useError();
</script>

<template>
	<div class="container mx-auto mt-4 flex flex-col md:flex-row">
		<img src="~/assets/logo.png" class="h-20 w-20 md:h-40 md:w-40 rounded-full mr-8 mb-8" alt="EVE Ref Logo" />
		<span>
			<div class="text-2xl md:text-4xl mb-4">{{ error.statusMessage }}</div>
			<ul>
				<li v-if="error.url">URL: {{ error.url }}</li>
				<li v-if="error.statusCode">Status code: {{ error.statusCode }}</li>
				<li v-if="error.statusMessage">Status message: {{ error.statusMessage }}</li>
				<li v-if="error.message">Message: {{ error.message }}</li>
				<li v-if="error.description">Description: {{ error.description }}</li>
				<li v-if="error.data">Data: {{ error.data }}</li>
			</ul>
			<a href="/">Go home &raquo;</a>
		</span>
	</div>
</template>
