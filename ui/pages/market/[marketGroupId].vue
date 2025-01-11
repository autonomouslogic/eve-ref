<script setup lang="ts">
import {getIntRouteParam} from "~/lib/routeUtils";

const route = useRoute();
const marketGroupId = getIntRouteParam(route, "marketGroupId");
if (!marketGroupId) {
	console.error("marketGroupId is null");
}
await navigateTo(`/market-groups/${marketGroupId}`);
</script>
