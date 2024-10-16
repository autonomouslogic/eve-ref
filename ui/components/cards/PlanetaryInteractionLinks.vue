<script setup lang="ts">
import {type Blueprint} from "~/refdata-openapi";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const links = {
	"Adam4EVE": "https://www.adam4eve.eu/manu_calc.php?typeID=",
	"EVE Cookbook": "https://evecookbook.com/?share=1&blueprintTypeId=",
	"EVE Tycoon": "https://evetycoon.com/industryCalculator/",
	"Fuzzwork": "https://www.fuzzwork.co.uk/blueprint/?typeid="
};

</script>

<template>
	<div class="mt-2">
		View on:
		<ul>
			<li>
				<ExternalLink url="https://www.adam4eve.eu/pi_chain.php">PI Chain on Adam4Eve</ExternalLink>
			</li>
			<li>
				<ExternalLink url="https://www.adam4eve.eu/pi_rank.php">PI Profitability on Adam4Eve</ExternalLink>
			</li>
		</ul>
	</div>
</template>
