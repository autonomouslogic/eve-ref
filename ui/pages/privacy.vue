<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";

useHead({
	title: "Privacy Policy"
});
</script>

<template>
	<span>
		<h1>Privacy</h1>
		<p>
			This website (everef.net) is created and managed by <ExternalLink url="https://kennethjorgensen.com/">Kenneth Jørgensen</ExternalLink>
			It does not, by itself, use or store any cookies nor does it or I collect any personal information.
			However, the following services are used:
		</p>
		<ul class="list-disc list-inside">
			<li><ExternalLink url="https://aws.amazon.com/privacy/">Amazon Web Services</ExternalLink></li>
			<li><ExternalLink url="https://www.backblaze.com/company/policy/privacy">Backblaze</ExternalLink></li>
			<li><ExternalLink url="https://www.cloudflare.com/privacypolicy/">Cloudflare</ExternalLink></li>
			<li><ExternalLink url="https://www.goatcounter.com/help/privacy">Goatcounter</ExternalLink></li>
			<li><ExternalLink url="https://www.hetzner.com/privacy-policy-notice">Hetzner</ExternalLink></li>
			<li><ExternalLink url="https://support.google.com/adsense/answer/1348695">Google AdSense</ExternalLink> (displays ads and uses Google's tracking technologies)</li>
			<li><ExternalLink url="https://usercentrics.com/privacy-policy/">Usercentrics</ExternalLink> (manages your consent preferences)</li>
		</ul>
		<p>
			To the best of my knowledge, this website is fully compliant with laws such as GDPR, EPD, and CCPA.
			If you believe this is not the case, feel free to contact me.
		</p>
	</span>
</template>
