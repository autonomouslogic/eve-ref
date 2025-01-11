<script setup lang="ts">
import {getIntRouteParam} from "~/lib/routeUtils";

const route = useRoute();
const typeId = getIntRouteParam(route, "typeId");
if (!typeId) {
	console.error("typeId is null");
}
await navigateTo(`/types/${typeId}`);
</script>
