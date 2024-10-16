<script setup lang="ts">
import {type BlueprintMaterial, type DogmaAttribute, type InventoryType, type Schematic} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import Duration from "~/components/dogma/units/Duration.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import {CUBIC_METER} from "~/lib/unitConstants";
import UnitValue from "~/components/dogma/UnitValue.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const producedBySchematics: Schematic[] = [];
if (props.inventoryType.producedBySchematicIds) {
	for (let schematicId of props.inventoryType.producedBySchematicIds) {
		const schematic = await refdataApi.getSchematic({schematicId});
		producedBySchematics.push(schematic);
	}
}

const usedBySchematics: Schematic[] = [];
if (props.inventoryType.usedBySchematicIds) {
	for (let schematicId of props.inventoryType.usedBySchematicIds) {
		const schematic = await refdataApi.getSchematic({schematicId});
		usedBySchematics.push(schematic);
	}
}

const installableSchematicIds: Schematic[] = [];
if (props.inventoryType.installableSchematicIds) {
	for (let schematicId of props.inventoryType.installableSchematicIds) {
		const schematic = await refdataApi.getSchematic({schematicId});
		installableSchematicIds.push(schematic);
	}
}

const typeVolumes: {[key: string]: number} = {};

async function loadMaterialVolume(material: BlueprintMaterial) {
	if (material.typeId) {
		const type = await refdataApi.getType({typeId: material.typeId});
		if (type.volume) {
			typeVolumes[material.typeId] = type.volume;
		}
	}
}

for (const schematic of producedBySchematics.values()) {
	if (schematic.products) {
		for (let materialId in schematic.products) {
			const material = schematic.products[materialId];
			await loadMaterialVolume(material);
		}
	}
	if (schematic.materials) {
		for (let materialId in schematic.materials) {
			const material = schematic.materials[materialId];
			await loadMaterialVolume(material);
		}
	}
}



// for (let schematic of producedBySchematics) {
//   Object.entries(schematic.products);
//   if (schematic.products) {
//     for (let product of schematic.products) {
//       if (product.typeId && product.quantity) {
//         typeVolumes[product.typeId] = product.quantity * typeVolumes[product.typeId];
//       }
//     }
//   }
// }


</script>

<template>
	<template v-if="dogmaAttributes.length > 0 || producedBySchematics.length > 0 || inventoryType.harvestedByPinTypeIds || usedBySchematics.length > 0 || installableSchematicIds.length > 0 || inventoryType.buildablePinTypeIds">
		<CardWrapper :title="title">
			<AttributeList :elements="dogmaAttributes.length">
				<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />
			</AttributeList>

			<template v-if="producedBySchematics.length > 0">

				<template v-for="schematic in producedBySchematics" :key="schematic.schematicId">
					<h3 v-if="schematic.name?.en">{{ schematic.name.en }}</h3>

					<AttributeList>
						<AttributeListItem>
							<template v-slot:key>Schematic ID:</template>
							{{schematic.schematicId}}
						</AttributeListItem>
						<AttributeListItem v-if="schematic.cycleTime">
							<template v-slot:key>Cycle time:</template>
							<Duration :milliseconds="schematic.cycleTime * 1000" />
						</AttributeListItem>
					</AttributeList>

					<table v-if="schematic.products" class="standard-table">
						<thead>
							<tr>
								<th>Product</th>
								<th class="text-right">Quantity</th>
								<th class="text-right">Volume</th>
							</tr>
						</thead>
						<tr v-for="(product, productId) in schematic.products" :key="productId">
							<td><TypeLink :type-id="product.typeId" /></td>
							<td class="text-right">
								<FormattedNumber :number="product.quantity" />
							</td>
							<td class="text-right">
								<template v-if="product.typeId && product.quantity && typeVolumes[product.typeId]">
									<UnitValue :unit-id="CUBIC_METER" :value="product.quantity * typeVolumes[product.typeId]" />
								</template>
							</td>
						</tr>
					</table>

					<table v-if="schematic.materials" class="standard-table">
						<thead>
							<tr>
								<th>Material</th>
								<th class="text-right">Quantity</th>
								<th class="text-right">Volume</th>
							</tr>
						</thead>
						<tr v-for="(material, materialId) in schematic.materials" :key="materialId">
							<td>
								<TypeLink :type-id="material.typeId" />
							</td>
							<td class="text-right">
								<FormattedNumber :number="material.quantity" />
							</td>
							<td class="text-right">
								<template v-if="material.typeId && material.quantity && typeVolumes[material.typeId]">
									<UnitValue :unit-id="CUBIC_METER" :value="material.quantity * typeVolumes[material.typeId]" />
								</template>
							</td>
						</tr>
					</table>

					<table v-if="schematic.pinTypeIds && schematic.pinTypeIds.length > 0" class="standard-table">
						<thead>
							<tr>
								<th>Produced in</th>
							</tr>
						</thead>
						<tr v-for="pinTypeId in schematic.pinTypeIds" :key="pinTypeId">
							<td>
								<TypeLink :type-id="pinTypeId" />
							</td>
						</tr>
					</table>

				</template>
			</template>

			<div v-if="inventoryType.harvestedByPinTypeIds && inventoryType.harvestedByPinTypeIds.length > 0" class="mt-3">
				<b>Harvested by:</b>
				<ul>
					<li v-for="pinTypeId in inventoryType.harvestedByPinTypeIds" :key="pinTypeId">
						<TypeLink :type-id="pinTypeId" />
					</li>
				</ul>
			</div>

			<div v-if="usedBySchematics.length > 0" class="mt-3">
				<b>Used by schematics:</b>
				<ul>
					<template v-for="schematic in usedBySchematics" :key="schematic.schematicId">
						<template v-if="schematic.products">
							<li v-for="(product, productId) in schematic.products" :key="productId">
								<TypeLink :type-id="product.typeId" />
							</li>
						</template>
					</template>
				</ul>
			</div>

			<div v-if="installableSchematicIds.length > 0" class="mt-3">
				<b>Installable schematics:</b>
				<ul>
					<template v-for="schematic in installableSchematicIds" :key="schematic.schematicId">
						<template v-if="schematic.products">
							<li v-for="(product, productId) in schematic.products" :key="productId">
								<TypeLink :type-id="product.typeId" />
							</li>
						</template>
					</template>
				</ul>
			</div>

			<div v-if="inventoryType.buildablePinTypeIds && inventoryType.buildablePinTypeIds.length > 0" class="mt-3">
				<b>Buildable pins:</b>
				<ul>
					<li v-for="pinTypeId in inventoryType.buildablePinTypeIds" :key="pinTypeId">
						<TypeLink :type-id="pinTypeId" />
					</li>
				</ul>
			</div>

		</CardWrapper>
	</template>
</template>

<style>
th {
  @apply text-left;
}
</style>
