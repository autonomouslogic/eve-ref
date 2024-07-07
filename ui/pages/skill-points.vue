<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {EVE_REFERRAL_URL} from "~/lib/urls";
import {cacheGroupBundle} from "~/refdata";
import {CYBERIMPLANT} from "~/lib/groupConstants";

useHead({
	title: "Skill Points"
});

await cacheGroupBundle(CYBERIMPLANT);

</script>

<template>
	<h1>Skill Points</h1>

	<p>
		This is an overview of the various ways to acquire skill points.
		Months are 30 days.
	</p>
	<p>
		New accounts get 1,000,000 free skill points by using <ExternalLink :url="EVE_REFERRAL_URL">this referral link</ExternalLink>.
	</p>

	<div>
		<SkillpointsAccounts />

		<SkillpointsInjectors />

		<SkillpointsAccelerators />

		<SkillpointsPacks />

		<SkillpointsImplants />

		<ClientOnly>
			<Suspense>
				<SkillpointsFactionImplants />
				<template #fallback>
					Loading Faction Implants
				</template>
			</Suspense>
		</ClientOnly>
	</div>

	<h2>Resources</h2>
	<div>
		<ul class="list-disc list-inside">
			<li><ExternalLink url="https://wiki.eveuniversity.org/Skills_and_learning">Skills and learning</ExternalLink> - UniWiki</li>
			<li><ExternalLink url="https://www.qsna.eu/eve/pilot-services/omega-cost">Pilot Services Overview</ExternalLink> - Quantum Anomaly</li>
		</ul>
	</div>
</template>

<style scoped>
:deep(h2) {
  @apply mt-6;
}
:deep(th), :deep(tr) {
  @apply border-b border-slate-500;
}
:deep(th), :deep(td) {
  @apply px-2 whitespace-nowrap;
}
</style>
