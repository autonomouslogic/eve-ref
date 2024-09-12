<script setup lang="ts">

import Money from "~/components/dogma/units/Money.vue";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

interface DonorEntry {
	donor_name: string
	amount: number
	character_id: number
	corporation_id: number
}

const {status, data: donors} = await useLazyFetch<DonorEntry[]>("https://static.everef.net/donations.json", {
	server: false
});

</script>

<template>
	<table class="table w-full" v-if="status == 'success'">
		<tr v-for="donor in donors" :key="donor.donor_name">
			<td class="font-bold">
				<ExternalLink v-if="donor.character_id" :url="'https://evewho.com/character/' + donor.character_id">
					{{ donor.donor_name }}
				</ExternalLink>
				<ExternalLink v-else-if="donor.corporation_id" :url="'https://evewho.com/corporation/' + donor.corporation_id">
					{{ donor.donor_name }}
				</ExternalLink>
				<template v-else>{{ donor.donor_name }}</template>
			</td>
			<td class="text-right"><Money :value="donor.amount" /></td>
		</tr>
	</table>
	<div class="w-full text-right">
		<NuxtLink to="/about">Donate ISK</NuxtLink>
	</div>
</template>
