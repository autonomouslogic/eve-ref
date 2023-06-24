<script setup lang="ts">
import TypeLink from "~/components/helpers/TypeLink.vue";
</script>

<template>
  <div>
    <h1>Index Page</h1>
    <ul>
      <li>
        <NuxtLink
          class="underline hover:underline-offset-4 font-bold text-blue-500 hover:text-blue-700"
          to="/categories">
          Categories
        </NuxtLink>
      </li>
      <li><TypeLink type-id="645"></TypeLink></li>
    </ul>
  </div>
</template>
