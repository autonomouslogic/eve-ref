<script setup lang="ts">
const {locale, availableLocales } = useI18n();
const selectedLocale = ref(locale);
</script>

<template>
  <div>
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
    <NuxtLink
      class="underline hover:underline-offset-4 font-bold text-blue-500 hover:text-blue-700"
      to="/categories">
      Categories
    </NuxtLink>
  </div>
  <hr>
</template>
