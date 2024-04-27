const baseDogmaNames: string[] = [
    "serviceSlots",
    "structureServiceRoleBonus",
    "structureRoleBonus",

    "rigSize",
    "upgradeCapacity",
    "upgradeSlotsLeft",

    "hiSlots",
    "medSlots",
    "lowSlots",
    "maxLockedTargets",
    "maxTargetRange",
    "launcherSlotsLeft",
    "structureAoERoFRoleBonus",

    "fighterTubes",
    "fighterCapacity",
    "fighterStandupHeavySlots",
    "fighterStandupLightSlots",
    "fighterStandupSupportSlots",
    "fighterAbilityAntiCapitalMissileResistance",

    "cpuOutput",
    "powerOutput",
    "capacitorCapacity",
    "powerGridOutput",

    "shieldCapacity",
    "shieldEmDamageResonance",
    "shieldThermalDamageResonance",
    "shieldKineticDamageResonance",
    "shieldExplosiveDamageResonance",

    "armorHP",

    "hp",
    "structureFullPowerStateHitpointMultiplier",

    "structureRequiresDeedType",

    "pauseShieldRepairDpsThreshold",
    "pauseArmorRepairDpsThreshold",
    "pauseHullRepairDpsThreshold",

    "shieldDamageLimit",
    "armorDamageLimit",
    "structureDamageLimit",
];

export const STRUCTURE_COMPARISON_ENGINEERING_COMPLEX_DOGMA_NAMES: string[] = [
    "strEngMatBonus",
    "strEngCostBonus",
    "strEngTimeBonus",
    ...baseDogmaNames
];

export const STRUCTURE_COMPARISON_REFINERY_DOGMA_NAMES: string[] = [
    "strReactionTimeMultiplier",
    "strRefiningYieldBonus",
    "structureGasDecompressionEfficiencyBonus",
    "autoFractureDelay",
    "moonAsteroidDecayTimeMultiplier",
    "structureGasDecompressionEfficiencyBonus",
    ...baseDogmaNames
];

export const STRUCTURE_COMPARISON_CITADELS_DOGMA_NAMES: string[] = [
    ...baseDogmaNames
];

export const STRUCTURE_RIGS_COMPARISON_ENGINEERING_DOGMA_NAMES: string[] = [
    "rigSize",
    "upgradeCost",
    "attributeEngRigTimeBonus",
    "attributeEngRigMatBonus",
    "attributeEngRigCostBonus",
    // "structureRigBonus1",
    // "structureRigBonus2",
    "hiSecModifier",
    "lowSecModifier",
    "nullSecModifier"
]
