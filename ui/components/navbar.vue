<script setup lang="ts">
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {PATREON_URL} from "~/lib/urls";
import InternalLink from "~/components/helpers/InternalLink.vue";

const localePath = useLocalePath();
</script>

<template>
	<div class="header flex p-1">
		<span class="w-full space-x-3">
			<span>
				<InternalLink to="/">EVE Ref</InternalLink>
			</span>
			<span><InternalLink :to="localePath('/categories', 'en')">Categories</InternalLink></span>
			<span><InternalLink to="/market-groups">Market Groups</InternalLink></span>
			<span><InternalLink to="/structures"><MarketGroupName :market-group-id="477" /></InternalLink></span>
			<span><InternalLink to="/skill-points">Skill Points</InternalLink></span>
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
