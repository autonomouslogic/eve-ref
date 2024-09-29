<template>
	<div>
		<dt>
			<slot name="key" />
		</dt>
		<dd>
			<slot />
		</dd>
	</div>
</template>

<style scoped>
div {
  break-inside: avoid-column;
}
dt {
  @apply pt-1;
}

dd {
  @apply border-gray-700 border-b pl-4 pb-1 text-right;
}

dd:last-child {
  @apply border-b-0;
}
</style>
