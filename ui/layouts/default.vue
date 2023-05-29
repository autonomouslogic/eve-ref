<template>
  <div class="wrapper">
    <navbar />
    <slot />
  </div>
</template>

<style>
.wrapper {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}
</style>