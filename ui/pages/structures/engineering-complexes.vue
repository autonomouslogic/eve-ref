<script setup lang="ts">
import refdataApi, {cacheBundle} from "~/refdata";
import {DogmaAttribute, DogmaTypeAttribute, InventoryGroup, InventoryType} from "~/refdata-openapi";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import TypeCards from "~/components/types/TypeCards.vue";
import LinkParser from "~/components/helpers/LinkParser.vue";
import {getAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";

const {locale} = useI18n();

const marketGroupId = 2324;
const marketGroup = await refdataApi.getMarketGroup({marketGroupId});
const structureIds = marketGroup.typeIds;
if (!structureIds) {
	throw new Error(`Market group ${marketGroupId} has no type IDs`);
}
structureIds.sort();
const structures = await Promise.all(structureIds.map(typeId => refdataApi.getType({typeId})));
const dogmaAttributes: { [key: string]: DogmaTypeAttribute } = {};
for (let structure of structures) {
	const attrs = await loadDogmaAttributesForType(structure);
	for (let k in attrs) {
		dogmaAttributes[k] = attrs[k];
	}
}
const dogmaAttributesArray = Object.values(dogmaAttributes);

const attrNames = [
	"rigSize",
	"hiSlots",
	"medSlots",
	"lowSlots",
	"upgradeSlotsLeft",
	"serviceSlots",
	"launcherSlotsLeft",
	"upgradeCapacity",
	"cpuOutput",
	"capacitorCapacity",
	"powerGridOutput",
	"shieldCapacity",
	"shieldEmDamageResonance",
	"shieldThermalDamageResonance",
	"shieldKineticDamageResonance",
	"shieldExplosiveDamageResonance",
	"armorHP",
	"hp",
	"maxLockedTargets",
	"maxTargetRange"
];
const listAttributes = attrNames.map(name => getAttributeByName(name, dogmaAttributesArray));

</script>

<template>
	<h1>Engineering Complexes</h1>
	<table class="table-auto divide-gray-200">
		<thead>
			<th></th>
			<th v-for="structure in structures" :key="structure.typeId">
				<h2><type-link :type-id="structure.typeId" /></h2>
			</th>
		</thead>
		<tbody>
			<template v-for="(attr, index) in listAttributes" :key="index">
				<tr v-if="attr && attr.attributeId">
					<td>
						<AttributeTypeIcon :dogma-attribute="attr" :size="25" />
						<DogmaAttributeLink :attribute="attr" />
					</td>
					<td v-for="structure in structures" :key="structure.typeId">
						<template v-if="structure.dogmaAttributes && structure.dogmaAttributes[attr.attributeId]">
							<dogma-value :value="structure.dogmaAttributes[attr.attributeId].value" :attribute="attr" />
						</template>
					</td>
				</tr>
			</template>
		</tbody>
	</table>
</template>
