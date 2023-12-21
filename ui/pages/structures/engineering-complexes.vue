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
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";

const {locale} = useI18n();

const marketGroupId = 2324;
const marketGroup = await refdataApi.getMarketGroup({marketGroupId});
const structureIds = marketGroup.typeIds;
if (!structureIds) {
	throw new Error(`Market group ${marketGroupId} has no type IDs`);
}
structureIds.sort();

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
	"powerOutput",
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
	"maxTargetRange",
	"strEngMatBonus",
	"strEngCostBonus",
	"strEngTimeBonus",
	"structureServiceRoleBonus",
	"structureRequiresDeedType",
	"pauseShieldRepairDpsThreshold",
	"pauseArmorRepairDpsThreshold",
	"pauseHullRepairDpsThreshold"
];
</script>

<template>
	<h1>
		<MarketGroupName :market-group-id="marketGroupId" />
	</h1>
	<CompareComparisonTable :type-ids="structureIds" :dogma-attribute-names="attrNames" />
</template>
