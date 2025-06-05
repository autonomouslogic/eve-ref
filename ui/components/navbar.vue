<script setup lang="ts">
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {PATREON_URL} from "~/lib/urls";
import InternalLink from "~/components/helpers/InternalLink.vue";
import MotdBanner from "~/components/MotdBanner.vue";

const localePath = useLocalePath();
</script>

<template>
	<div class="header flex flex-col p-1 space-y-2">
		<div class="flex flex-nowrap flex-row">
			<span class="flex-auto space-x-3">
				<span>
					<InternalLink to="/">EVE Ref</InternalLink>
				</span>
				<span>
					<InternalLink to="/categories">Categories</InternalLink>
				</span>
				<span>
					<InternalLink to="/market-groups">Market Groups</InternalLink>
				</span>
				<span>
					<InternalLink to="/meta-groups">Meta Groups</InternalLink>
				</span>
				<span>
					<InternalLink to="/structures">Structures</InternalLink>
				</span>
				<span>
					<InternalLink to="/skill-points">Skill Points</InternalLink>
				</span>
			</span>
			<span class="flex-auto space-x-3 text-right">
				<span>
					<InternalLink to="/giveaways">Giveaways</InternalLink>
				</span>
				<span>
					<ExternalLink :url="PATREON_URL" class="header-patreon whitespace-nowrap">
						<span>
							<font-awesome-icon icon="fa-brands fa-patreon"/>
						</span>
						Patreon
					</ExternalLink>
				</span>
			</span>
		</div>
		<span>
			<SearchBar class="w-full md:w-3/4 xl:w-2/4" />
		</span>
	</div>

	<MotdBanner/>
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
