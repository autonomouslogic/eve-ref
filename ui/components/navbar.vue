<script setup lang="ts">
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

const {locale, availableLocales } = useI18n();
const selectedLocale = ref(locale);

</script>

<template>
	<div class="header flex p-1">
		<span class="w-full space-x-3">
			<span>
				<NuxtLink to="/">EVE Ref</NuxtLink>
			</span>
			<span><NuxtLink to="/categories">Categories</NuxtLink></span>
			<span><NuxtLink to="/market-groups">Market Groups</NuxtLink></span>
			<span><NuxtLink to="/structures"><MarketGroupName :market-group-id="477" /></NuxtLink></span>
		</span>
		<span class="w-full space-x-3 text-right">
      <span>
        <a href="https://patreon.com/everef" class="header-patreon">
							<span><font-awesome-icon icon="fa-brands fa-patreon" /></span>
							Patreon
						</a>
      </span>
			<span>
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
