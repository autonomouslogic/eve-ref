<script setup lang="ts">
import refdataApi from "~/refdata";

const {value, unitId} = defineProps<{
  value: string | number | undefined,
  unitId: number | undefined
}>();

const {locale} = useI18n();

const unit = await refdataApi.getUnit({unitId});
</script>

<template>
  {{ value }} {{ unit.displayName }}
</template>
