<script setup lang="ts">
const {locale, availableLocales, messages} = useI18n();

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
      >{{ messages[availableLocale].lang }}
      </option>
    </select>
  </div>
  <hr>
</template>

<style scoped>

</style>