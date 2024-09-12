<script setup lang="ts">
const { locale, locales, setLocale } = useI18n();
const switchLocalePath = useSwitchLocalePath()
</script>

<template>
<!--  <button @click="setLocale('en')">en</button>-->
<!--  <button @click="setLocale('fr')">fr</button>-->
  <NuxtLink :to="switchLocalePath('en')">en</NuxtLink>
  <NuxtLink :to="switchLocalePath('fr')">fr</NuxtLink>
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
