<script setup lang="ts">
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {PATREON_URL} from "~/lib/urls";
const localePath = useLocalePath();

</script>

<template>
	<div class="header flex p-1">
		<span class="w-full space-x-3">
			<span>
				<NuxtLink to="/">EVE Ref</NuxtLink>
			</span>
			<span><NuxtLink :to="localePath('/categories')">Categories</NuxtLink></span>
			<span><NuxtLink to="/market-groups">Market Groups</NuxtLink></span>
			<span><NuxtLink to="/structures"><MarketGroupName :market-group-id="477" /></NuxtLink></span>
			<span><NuxtLink to="/skill-points">Skill Points</NuxtLink></span>
			<span><SearchBar /></span>
		</span>
		<span class="w-full space-x-3 text-right">
			<span>
				<ExternalLink :url="PATREON_URL" class="header-patreon">
					<span><font-awesome-icon icon="fa-brands fa-patreon" /></span>
					Patreon
				</ExternalLink>
			</span>
			<span>
				<LocaleSwitcher />
			</span>
		</span>
	</div>
</template>

<style scoped>
.header {
  background-color: var(--header-background-color);
  color: black;
}

a {
  color: black;
}

a.header-patreon {
  color: var(--link-color);
}
</style>
