<script setup lang="ts">
import {TraitBonus} from "~/refdata-openapi";
import LinkParser from "~/components/helpers/LinkParser.vue";
import UnitValue from "~/components/dogma/UnitValue.vue";

const {locale} = useI18n();

const props = defineProps<{
	title: string;
	bonuses: { [key: string]: TraitBonus; } | undefined;
}>();

const hasBonuses: boolean = props.bonuses !== undefined && Object.keys(props.bonuses).length > 0;
</script>

<template>
	<div v-if="hasBonuses">
		<h3>{{ props.title }}</h3>
		<ul>
			<li v-for="(trait, i) in props.bonuses" :key="i">
				<template v-if="trait.bonus !== undefined && trait.unitId !== undefined">
					<UnitValue :value="trait.bonus" :unit-id="trait.unitId" />&nbsp;
				</template>
				<LinkParser v-if="trait.bonusText" :content="trait.bonusText[locale]"/>
			</li>
		</ul>
	</div>
</template>
