<script setup lang="ts">
import {PATREON_URL} from "~/lib/urls";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
</script>

<template>
	<a :href="PATREON_URL" target="_blank" rel="noopener noreferrer" class="patreon-fab" aria-label="Support on Patreon">
		<FontAwesomeIcon icon="fa-brands fa-patreon" />
	</a>
</template>

<style scoped>
.patreon-fab {
	position: fixed;
	bottom: 1.5rem;
	right: 1.5rem;
	z-index: 1000;
	width: 3.5rem;
	height: 3.5rem;
	border-radius: 50%;
	background-color: #f96854;
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 1.5rem;
	box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
	transition: transform 0.5s, box-shadow 0.2s;
	text-decoration: none;
}

.patreon-fab:hover {
	transform: scale(1.25);
	box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
}
</style>
