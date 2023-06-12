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
  </div>
  <hr>
</template>
