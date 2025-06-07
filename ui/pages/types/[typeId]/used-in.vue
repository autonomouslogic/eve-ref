<script setup lang="ts">
import refdataApi, {cacheTypeBundle} from "~/refdata";
import {type InventoryGroup} from "~/refdata-openapi";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import TypeCards from "~/components/types/TypeCards.vue";
import LinkParser from "~/components/helpers/LinkParser.vue";
import {getIntRouteParam} from "~/lib/routeUtils";
import {tr} from "~/lib/translate";
import EveImage from "~/components/icons/EveImage.vue";
import {getTypeIconUrl} from "~/lib/urls";
import {SHIP} from "~/lib/categoryConstants";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const {locale} = useI18n();
const route = useRoute();
const typeId: number = getIntRouteParam(route, "typeId");
if (!typeId) {
	console.error("typeId is null");
}

await cacheTypeBundle(typeId);

const inventoryType = await refdataApi.getType({typeId});
if (inventoryType == undefined) {
	throw new Error(`Inventory type ${typeId} not found`);
}

const pageTitle = tr(inventoryType.name, locale.value);

useHead({
	title: `Blueprints using ${pageTitle}`
});
</script>

<template>
	<h1 v-if="inventoryType.name" class="mb-3">
		Blueprints using <TypeLink :type-id="inventoryType.typeId" />
	</h1>

	<ClientOnly>
		<div v-if="!inventoryType.usedInBlueprints">
			<TypeLink :type-id="inventoryType.typeId" /> isn't used in any blueprints.
		</div>
		<table v-else class="standard-table">
			<thead>
				<tr>
					<th>Blueprint</th>
					<th>Activity</th>
					<th class="text-right">Quantity</th>
				</tr>
			</thead>
			<tbody>
				<template v-for="(activityEntry, blueprintId) in inventoryType.usedInBlueprints" :key="blueprintId">
					<tr v-for="(usedIn, activity) in activityEntry" :key="activity">
						<td>
							<TypeLink :type-id="blueprintId" />
						</td>
						<td>{{ activity }}</td>
						<td class="text-right">
							<FormattedNumber :number="usedIn.quantity" :decimals="0" />
						</td>
					</tr>
				</template>
			</tbody>
		</table>
	</ClientOnly>
</template>
