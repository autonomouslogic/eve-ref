<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";

useHead({
	title: "Skill Points"
});
</script>

<template>
	<h1>Skill Points</h1>

	<SkillpointsAccounts />

	<SkillpointsInjectors />

	<SkillpointsAccelerators />

	<h2>Resources</h2>
	<div>
		<ul class="list-disc list-inside">
			<li><ExternalLink url="https://wiki.eveuniversity.org/Skills_and_learning">Skills and learning</ExternalLink> - UniWiki</li>
		</ul>
	</div>
</template>

<style scoped>
th, tr {
  @apply border-b border-slate-500;
}
</style>
