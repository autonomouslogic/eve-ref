<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {EVE_REFERRAL_URL} from "~/lib/urls";

useHead({
	title: "Skill Points"
});
</script>

<template>
	<h1>Skill Points</h1>

	<p>
		This is an overview of the various ways to acquire skill points.
		New accounts can get 1,000,000 free skill points by using <ExternalLink :url="EVE_REFERRAL_URL">this referral link</ExternalLink>.
	</p>

	<SkillpointsAccounts />

	<SkillpointsPacks />

	<SkillpointsInjectors />

	<SkillpointsAccelerators />

	<SkillpointsImplants />

	<h2>Resources</h2>
	<div>
		<ul class="list-disc list-inside">
			<li><ExternalLink url="https://wiki.eveuniversity.org/Skills_and_learning">Skills and learning</ExternalLink> - UniWiki</li>
			<li><ExternalLink url="https://www.qsna.eu/eve/pilot-services/omega-cost">Pilot Services Overview</ExternalLink> - Quantum Anomaly</li>
		</ul>
	</div>
</template>

<style scoped>
th, tr {
  @apply border-b border-slate-500;
}
</style>
