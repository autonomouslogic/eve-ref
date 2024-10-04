<script setup lang="ts">
const error = useError();
</script>

<template>
	<div class="flex flex-col h-screen justify-between">
		<div class="container mb-auto px-4">
			<h1>{{ error.statusCode }}</h1>
			<ul>
				<li v-if="error.url">URL: {{ error.url }}</li>
				<li v-if="error.statusCode">Status code: {{ error.statusCode }}</li>
				<li v-if="error.statusMessage">Status message: {{ error.statusMessage }}</li>
				<li v-if="error.message">Message: {{ error.message }}</li>
				<li v-if="error.description">Description: {{ error.description }}</li>
				<li v-if="error.data">Data: {{ error.data }}</li>
			</ul>
			<a href="/">Go home &raquo;</a>
		</div>
	</div>
</template>
