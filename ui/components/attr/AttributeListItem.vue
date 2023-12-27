<template>
	<dt>
		<slot name="key" />
	</dt>
	<dd>
		<slot />
	</dd>
</template>

<style scoped>
dt {
  @apply pt-1;
}

dd {
  @apply border-gray-300 border-b pl-4 text-right;
  @apply pb-1;
}

dd:last-child {
  @apply border-b-0;
}
</style>
