<script setup lang="ts">
import {
	CYBERNETIC_SUBPROCESSOR_BASIC,
	CYBERNETIC_SUBPROCESSOR_IMPROVED,
	CYBERNETIC_SUBPROCESSOR_STANDARD,
	LIMITED_CYBERNETIC_SUBPROCESSOR,
	LIMITED_CYBERNETIC_SUBPROCESSOR_BETA,
	LIMITED_MEMORY_AUGMENTATION,
	LIMITED_MEMORY_AUGMENTATION_BETA,
	LIMITED_NEURAL_BOOST,
	LIMITED_NEURAL_BOOST_BETA,
	LIMITED_OCULAR_FILTER,
	LIMITED_OCULAR_FILTER_BETA,
	LIMITED_SOCIAL_ADAPTATION_CHIP,
	LIMITED_SOCIAL_ADAPTATION_CHIP_BETA,
	MEMORY_AUGMENTATION_BASIC,
	MEMORY_AUGMENTATION_IMPROVED,
	MEMORY_AUGMENTATION_STANDARD,
	NEURAL_BOOST_BASIC,
	NEURAL_BOOST_IMPROVED,
	NEURAL_BOOST_STANDARD,
	OCULAR_FILTER_BASIC,
	OCULAR_FILTER_IMPROVED,
	OCULAR_FILTER_STANDARD,
	SOCIAL_ADAPTATION_CHIP_BASIC,
	SOCIAL_ADAPTATION_CHIP_IMPROVED,
	SOCIAL_ADAPTATION_CHIP_STANDARD,
} from "~/lib/typeConstants";
import type {ImplantSet} from "~/components/skillpoints/ImplantSet";
import ImplantsTable from "~/components/skillpoints/ImplantsTable.vue";

const sets: ImplantSet[] = [
	{
		name: "Limited",
		bonus: 1,
		typeIds: [
			LIMITED_OCULAR_FILTER,
			LIMITED_MEMORY_AUGMENTATION,
			LIMITED_NEURAL_BOOST,
			LIMITED_CYBERNETIC_SUBPROCESSOR,
			LIMITED_SOCIAL_ADAPTATION_CHIP,
		]
	} as ImplantSet,
	{
		name: "Limited Beta",
		bonus: 2,
		typeIds: [
			LIMITED_OCULAR_FILTER_BETA,
			LIMITED_MEMORY_AUGMENTATION_BETA,
			LIMITED_NEURAL_BOOST_BETA,
			LIMITED_CYBERNETIC_SUBPROCESSOR_BETA,
			LIMITED_SOCIAL_ADAPTATION_CHIP_BETA,
		]
	} as ImplantSet,
	{
		name: "Basic",
		bonus: 3,
		typeIds: [
			OCULAR_FILTER_BASIC,
			MEMORY_AUGMENTATION_BASIC,
			NEURAL_BOOST_BASIC,
			CYBERNETIC_SUBPROCESSOR_BASIC,
			SOCIAL_ADAPTATION_CHIP_BASIC,
		]
	} as ImplantSet,
	{
		name: "Standard",
		bonus: 4,
		typeIds: [
			OCULAR_FILTER_STANDARD,
			MEMORY_AUGMENTATION_STANDARD,
			NEURAL_BOOST_STANDARD,
			CYBERNETIC_SUBPROCESSOR_STANDARD,
			SOCIAL_ADAPTATION_CHIP_STANDARD,
		]
	} as ImplantSet,
	{
		name: "Improved",
		bonus: 5,
		typeIds: [
			OCULAR_FILTER_IMPROVED,
			MEMORY_AUGMENTATION_IMPROVED,
			NEURAL_BOOST_IMPROVED,
			CYBERNETIC_SUBPROCESSOR_IMPROVED,
			SOCIAL_ADAPTATION_CHIP_IMPROVED,
		]
	} as ImplantSet,
	// {
	//   name: "Advanced",
	//   bonus: 6,
	//   typeIds: [
	//     OCULAR_FILTER_ADVANCED,
	//     MEMORY_AUGMENTATION_ADVANCED,
	//     NEURAL_BOOST_ADVANCED,
	//     CYBERNETIC_SUBPROCESSOR_ADVANCED,
	//     SOCIAL_ADAPTATION_CHIP_ADVANCED,
	//   ]
	// } as ImplantSet,
	// {
	//   name: "Elite",
	//   bonus: 7,
	//   typeIds: [
	//     OCULAR_FILTER_ELITE,
	//     MEMORY_AUGMENTATION_ELITE,
	//     NEURAL_BOOST_ELITE,
	//     CYBERNETIC_SUBPROCESSOR_ELITE,
	//   ]
	// } as ImplantSet,
];
</script>

<template>
	<h2>Implants</h2>
	<p>
		Each set with a breakdown per implant.
		ISK/SP is calculated for the full set, based on how long the implants are installed for.
		If you get podded, this will show you how much you end up paying per SP.
		Since installation is permanent, ISK/SP tends to zero.
	</p>

	<ImplantsTable :sets="sets" />
</template>

<style scoped>
</style>
