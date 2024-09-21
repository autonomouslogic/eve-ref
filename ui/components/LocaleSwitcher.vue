<script setup lang="ts">
const { locale, setLocale, availableLocales } = useI18n({ useScope: "global" });
const switchLocalePath = useSwitchLocalePath();
const router = useRouter();

function onLocaleChanged(event: Event) {
	const target = event.target as HTMLInputElement;
	router.push({ path: switchLocalePath(target.value) });
}
</script>

<template>
	<select @change="onLocaleChanged">
		<option
			v-for="loc in availableLocales"
			:key="loc"
			:value="loc"
			:selected="loc === locale"
		>{{ $t("languageName", 1, { locale: loc} ) }}</option>
	</select>
</template>

<style scoped>
</style>
