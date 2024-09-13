<script setup lang="ts">
const { locale, locales, setLocale } = useI18n();
const switchLocalePath = useSwitchLocalePath();
</script>

<template>
	<div>current locale: {{ locale }}</div><br/>
	<button @click="setLocale('en')">set en</button><br/>
	<button @click="setLocale('fr')">set fr</button><br/>
	<NuxtLink :to="switchLocalePath('en')">switch en</NuxtLink><br/>
	<NuxtLink :to="switchLocalePath('fr')">switch fr</NuxtLink>
	<div>
		{{ locales.length}}
		<div v-for="loc in locales" :key="loc.code">
			loc: {{ loc }}
		</div>
	</div>
<!--    <NuxtLink :to="switchLocalePath(loc.code)">{{ loc.name }}</NuxtLink>-->
<!--  <select name="locale" id="locale" v-model="locale">-->
<!--    <option-->
<!--      v-for="loc in locales"-->
<!--      :key="loc.code"-->
<!--      :value="loc.code"-->
<!--      :selected="loc.code === locale"-->
<!--    >{{loc.name}}</option>-->
<!--  </select>-->
</template>

<style scoped>
</style>
