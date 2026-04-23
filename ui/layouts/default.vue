<template>
	<NuxtLoadingIndicator :height="8" />
	<div class="flex flex-col h-screen justify-between">

		<header>
			<navbar />
		</header>

		<!-- EVE Ref header -->
		<div style="text-align: center;">
			<Adsbygoogle
				ad-slot="2103102198"
				ad-format="auto"
				:hide-unfilled="true"
				:ad-full-width-responsive="true"
			/>
		</div>

		<main class="container mb-auto px-4">
			<slot/>
		</main>

		<!-- EVE Ref footer -->
		<div style="text-align: center;">
			<Adsbygoogle
				ad-slot="7711774562"
				ad-format="auto"
				:hide-unfilled="true"
				:ad-full-width-responsive="true"
			/>
		</div>

		<footer class="mt-4">
			<Footer/>
		</footer>

	</div>

</template>

<style scoped>
</style>

<script setup lang="ts">
</script>
