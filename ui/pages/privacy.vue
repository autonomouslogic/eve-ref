<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";

useHead({
	title: "Privacy"
});
</script>

<template>
	<span>
		<h1>Privacy</h1>
		<p>
			This website (everef.net) is created and managed by <ExternalLink url="https://kennethjorgensen.com/">Kenneth Jørgensen</ExternalLink>
			It does not, by itself, use or store any cookies nor does it or I collect any personal information.
			However, the following services are used to host this website.
			Please refer to their respective privacy policies to see how they affect you and your privacy:
			<ul class="list-disc list-inside">
				<li><ExternalLink url="https://aws.amazon.com/privacy/">Amazon S3</ExternalLink></li>
				<li><ExternalLink url="https://www.backblaze.com/company/policy/privacy">Backblaze</ExternalLink></li>
				<li><ExternalLink url="https://www.cloudflare.com/privacypolicy/">Cloudflare</ExternalLink></li>
				<li><ExternalLink url="https://www.goatcounter.com/help/privacy">Goatcounter</ExternalLink></li>
				<li><ExternalLink url="https://www.hetzner.com/privacy-policy-notice">Hetzner</ExternalLink></li>
				<li><ExternalLink url="https://www.netlify.com/privacy/">Netlify</ExternalLink></li>
			</ul>
		</p>
		<p>
			This website is ad-free and will remain so for the time being.
			No third-party "tags", such as Google Analytics or Twitter share or Facebook like buttons, are included anywhere.
			To the best of my knowledge, this website is fully compliant with laws such as GDPR, EPD, and CCPA.
			If you believe this is not the case, feel free to contact me.
		</p>
	</span>
</template>
