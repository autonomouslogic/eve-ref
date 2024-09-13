<script setup lang="ts">
const { locale, setLocale, availableLocales } = useI18n();
</script>

<template>
	<select name="locale" id="locale" v-model="locale">
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
