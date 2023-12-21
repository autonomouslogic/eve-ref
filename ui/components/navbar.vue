<script setup lang="ts">
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";

const {locale, availableLocales } = useI18n();
const selectedLocale = ref(locale);

</script>

<template>
	<div class="p-1 space-x-3 border-b-2 border-neutral-400">
		<span>
			Language:
			<select name="locale" id="locale" v-model="selectedLocale">
				<option
					v-for="availableLocale in availableLocales"
					:key="availableLocale"
					:value="availableLocale"
					:selected="availableLocale === locale"
					v-t="{path: 'languageName', locale: availableLocale}"
				></option>
			</select>
		</span>
		<span><NuxtLink to="/categories">Categories</NuxtLink></span>
		<span><NuxtLink to="/market-groups">Market Groups</NuxtLink></span>
    <NuxtLink to="/structures"><MarketGroupName :market-group-id="477" /></NuxtLink>
	</div>
</template>
