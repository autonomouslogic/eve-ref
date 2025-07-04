const cards: {[key: string]: any} = {
	"defenses":
		{
			"name": {"en": "Defenses"},
			"component": "defenses",
			"dogmaAttributes":
			[
				"shieldCapacity",
				"shieldUniformity",
				"shieldEmDamageResonance",
				"shieldThermalDamageResonance",
				"shieldKineticDamageResonance",
				"shieldExplosiveDamageResonance",
				"armorHP",
				"armorUniformity",
				"armorEmDamageResonance",
				"armorThermalDamageResonance",
				"armorKineticDamageResonance",
				"armorExplosiveDamageResonance",
				"hp",
				"structureUniformity",
				"emDamageResonance",
				"thermalDamageResonance",
				"kineticDamageResonance",
				"explosiveDamageResonance"
			]
		},
	"variations":
		{
			"name": {"en": "Variations"},
			"component": "variations",
			"alwaysShow": true
		},
	"market":
		{
			"name": {"en": "Market"},
			"component": "market"
		},
	"basic":
		{
			"name": {"en": "Basic"},
			"component": "basic",
			"dogmaAttributes":
				[
					"techLevel",
					"metaLevel",
					"metaLevelOld",
					"hp",
					"capacity",
					"volume",
					"mass",
					"radius",
					"nondestructible",
					"disallowInHighSec",
					"disallowInEmpireSpace",
					"isCapitalSize",
				]
		},
	"offenses":
		{
			"name": {"en": "Offenses"},
			"dogmaAttributes":
				[
					"energyNeutralizerAmount",
					"energyNeutralizerDuration",
					"energyNeutralizerRangeOptimal",
					"energyNeutralizerEntityChance",
					"warpScrambleRange",
					"warpScrambleStrength",
					"entityWarpScrambleChance",
					"speedFactor",
					"entityAttackRange",
					"entityAttackDelayMin",
					"entityAttackDelayMax",
					"entityMissileTypeID"
				]
		},
	"resistances":
		{
			"name": {"en": "Resistances"},
			"dogmaAttributes":
				[
					"energyWarfareResistance",
					"weaponDisruptionResistance",
					"stasisWebifierResistance",
					"energyWarfareResistance",
					"cargoScanResistance",
				]
		},
	"subsystem":
		{
			"name": {"en": "Subsystem"},
			"dogmaAttributes":
				[
					"fitsToShipType",
					"subSystemSlot",
					"armorHPBonusAdd",
					"structureHPBonusAdd",
					"powerEngineeringOutputBonus",
					"maxLockedTargetsBonus",
					"cpuOutputBonus2",
					"hiSlotModifier",
					"medSlotModifier",
					"lowSlotModifier",
					"turretHardPointModifier",
					"launcherHardPointModifier",
					"cargoCapacityAdd",
					"subsystemBonusMassAddition",
					"virusStrengthBonus",
					"subsystemMHTFittingReduction",
					"roleBonusCommandBurstAoERange",
					"subsystemMMissileFittingReduction",
					"subsystemMPTFittingReduction",
					"subsystemMRARFittingReduction",
					"subsystemCommandBurstFittingReduction",
					"remoteArmorRepairerFalloffBonus",
					"remoteArmorRepairerOptimalBonus",
					"subsystemMRSBFittingReduction",
					"remoteShieldBoosterFalloffBonus",
					"agilityBonusAdd",
				]
		},
	"npc":
		{
			"name": {"en": "NPC"},
			"dogmaAttributes":
				[
					"AI_ChanceToNotTargetSwitch",
					"AI_IgnoreDronesBelowSignatureRadius",
					"AI_ShouldUseEffectMultiplier",
					"AI_ShouldUseEvasiveManeuver",
					"AI_ShouldUseSignatureRadius",
					"AI_ShouldUseTargetSwitching",
					"entityFactionLoss",
					"entityGroupRespawnChance",
					"entityKillBounty",
					"entitySecurityMaxGain",
					"entitySecurityStatusKillBonus",
					"modifyTargetSpeedChance",
					"modifyTargetSpeedDuration",
					"modifyTargetSpeedRange",
					"entityOverviewShipGroupId",
					"entityEquipmentMax",
					"entityEquipmentMin",
					"entityLootCountMin",
					"entityDefenderChance",
					"entityShieldBoostAmountPerSecond",
					"entityShieldBoostDuration",
					"entityChaseMaxDelay",
					"entityChaseMaxDelayChance",
					"entityChaseMaxDistance",
					"entityChaseMaxDuration",
					"entityChaseMaxDurationChance",
					"entityFlyRange",
					"entityArmorRepairAmount",
					"entityArmorRepairDelayChance",
					"entityArmorRepairDuration",
					"entityShieldBoostAmount"
				]
		},
	"module":
		{
			"name": {"en": "Module"},
			"dogmaAttributes":
				[
					"duration",
					"durationHighisGood",
					"maxGroupFitted",
					"maxGroupActive",
					"disallowEarlyDeactivation",
					"disallowRepeatingActivation",
					"canActivateInGateCloak",
					"moduleReactivationDelay",
					"signatureRadiusAdd"
				]
		},
	"oreVariations":
		{
			"name": {"en": "Ore Variations"},
			"component": "oreVariations",
			"alwaysShow": true
		},
	"skill":
		{
			"name": {"en": "Skill"},
			"dogmaAttributes":
				[
					"primaryAttribute",
					"secondaryAttribute",
					"skillTimeConstant",
					"canNotBeTrainedOnTrial"
				]
		},
	"cargo":
		{
			"name": {"en": "Cargo"},
			"dogmaAttributes":
				[
					"capacity",
					"fleetHangarCapacity",
					"frigateEscapeBayCapacity",
					"generalMiningHoldCapacity",
					"outputMoonMaterialBayCapacity",
					"shipMaintenanceBayCapacity",
					"specialAmmoHoldCapacity",
					"specialAsteroidHoldCapacity",
					"specialBoosterHoldCapacity",
					"specialColonyResourcesHoldCapacity",
					"specialCommandCenterHoldCapacity",
					"specialCorpseHoldCapacity",
					"specialFuelBayCapacity",
					"specialGasHoldCapacity",
					"specialIceHoldCapacity",
					"specialIndustrialShipHoldCapacity",
					"specialLargeShipHoldCapacity",
					"specialMaterialBayCapacity",
					"specialMediumShipHoldCapacity",
					"specialMineralHoldCapacity",
					"specialMobileDepotHoldCapacity",
					"specialPlanetaryCommoditiesHoldCapacity",
					"specialQuafeHoldCapacity",
					"specialSalvageHoldCapacity",
					"specialShipHoldCapacity",
					"specialSmallShipHoldCapacity",
					"specialSubsystemHoldCapacity",
					"hasFleetHangars",
					"hasShipMaintenanceBay"
				]
		},
	"fitting":
		{
			"name": {"en": "Fitting"},
			"dogmaAttributes":
				[
					"powerOutput",
					"cpuOutput",
					"turretSlotsLeft",
					"launcherSlotsLeft",
					"hiSlots",
					"medSlots",
					"lowSlots",
					"serviceSlots",
					"cpu",
					"power",
					"capacitorNeed",
					"maxTypeFitted"
				]
		},
	"overload":
		{
			"name": {"en": "Overloading"},
			"dogmaAttributes":
				[
					"heatCapacityHi",
					"heatDissipationRateHi",
					"heatDissipationRateMed",
					"heatDissipationRateLow",
					"heatCapacityMed",
					"heatCapacityLow",
					"heatGenerationMultiplier",
					"heatAttenuationHi",
					"heatAttenuationMed",
					"heatAttenuationLow",
				]
		},
	"rigging":
		{
			"name": {"en": "Rigging"},
			"dogmaAttributes":
				[
					"rigSlots",
					"upgradeSlotsLeft",
					"rigSize",
					"upgradeCapacity",
					"upgradeCost",
				]
		},
	"playerStructure":
		{
			"name": {"en": "Player Structure"},
			"dogmaAttributes":
				[
					"tetheringRange",
					"shieldDamageLimit",
					"armorDamageLimit",
					"structureDamageLimit",
					"structureAoERoFRoleBonus",
					"structureRoleBonus",
					"structureFullPowerStateHitpointMultiplier",
					"structureServiceRoleBonus",
					"strEngMatBonus",
					"strEngCostBonus",
					"strEngTimeBonus"
				]
		},
	"probe":
		{
			"name": {"en": "Probe"},
			"dogmaAttributes":
				[
					"baseMaxScanDeviation",
					"baseScanRange",
					"baseSensorStrength",
					"rangeFactor"
				]
		},
	"targeting":
		{
			"name": {"en": "Targeting"},
			"dogmaAttributes":
				[
					"maxLockedTargets",
					"maxAttackTargets",
					"maxTargetRange",
					"scanLadarStrength",
					"scanMagnetometricStrength",
					"scanRadarStrength",
					"scanGravimetricStrength",
					"signatureRadius",
					"scanResolution",
					"scanSpeed",
					"optimalSigRadius"
				]
		},
	"weapon":
		{
			"name": {"en": "Weapon"},
			"dogmaAttributes":
				[
					"damageMultiplier",
					"falloff",
					"maxRange",
					"speed",
					"trackingSpeed",
					"accuracyBonus",
					"reloadTime",
					"chargeRate",
					"miningAmount",
					"miningWastedVolumeMultiplier",
					"miningWasteProbability",
					"accessDifficultyBonus",
					"virusCoherence",
					"virusStrength",
					"virusElementSlots",
					"falloffBonus",
					"maxRangeBonus",
					"trackingSpeedBonus",
					"aoeVelocityBonus",
					"aoeCloudSizeBonus",
					"chargeGroup1",
					"chargeGroup2",
					"chargeGroup3",
					"chargeGroup4",
					"chargeGroup5"
				]
		},
	"ammunition":
		{
			"name": {"en": "Ammunition"},
			"dogmaAttributes":
				[
					"emDamage",
					"explosiveDamage",
					"kineticDamage",
					"thermalDamage",
					"baseArmorDamage",
					"baseShieldDamage",
					"trackingSpeedMultiplier",
					"chargeSize",
					"fallofMultiplier",
					"launcherGroup",
					"launcherGroup2",
					"launcherGroup3",
					"launcherGroup4",
					"launcherGroup5",
					"launcherGroup6",
					"weaponRangeMultiplier",
					"empFieldRange",
					"aoeCloudSize",
					"explosionDelay",
					"aoeFalloff",
					"aimedLaunch",
					"explosionRange",
					"detonationRange",
					"crystalsGetDamaged",
					"crystalVolatilityChance",
					"crystalVolatilityDamage",
					"specializationAsteroidYieldMultiplier",
					"specializationCrystalMiningWastedVolumeMultiplierBonus",
					"specializationCrystalMiningWasteProbabilityBonus",
					"specializationAsteroidDurationMultiplier",
					"specializationAsteroidTypeList",
					"unfitCapCost",
					"capNeedBonus",
					"aoeVelocity",
					"aoeDamageReductionFactor",
					"maxFOFTargetRange",
					"maxRangeBonusBonus",
					"trackingSpeedBonusBonus",
					"falloffBonusBonus",
					"aoeCloudSizeBonusBonus",
					"aoeVelocityBonusBonus",
					"signatureRadiusBonusBonus"
				]
		},
	"navigation":
		{
			"name": {"en": "Navigation"},
			"dogmaAttributes":
				[
					"maxVelocity",
					"warpSpeedMultiplier",
					"agility",
					"canJump",
					"jumpDriveCapacitorNeed",
					"jumpDriveConsumptionAmount",
					"jumpDriveConsumptionType",
					"jumpDriveRange",
					"jumpFatigueMultiplier",
					"jumpHarmonics",
					"jumpDriveDuration",
					"advancedCapitalAgility"
				]
		},
	"drones":
		{
			"name": {"en": "Drones"},
			"dogmaAttributes":
				[
					"droneCapacity",
					"droneBandwidth",
					"droneBandwidthUsed"
				]
		},
	"fighters":
		{
			"name": {"en": "Fighters"},
			"dogmaAttributes":
				[
					"fighterCapacity",
					"fighterHeavySlots",
					"fighterLightSlots",
					"fighterStandupHeavySlots",
					"fighterStandupLightSlots",
					"fighterStandupSupportSlots",
					"fighterTubes"
				]
		},
	"fighterSupport":
		{
			"name": {"en": "Fighter Support"},
			"dogmaAttributes":
				[
					"fighterBonusROFPercent",
					"fighterBonusShieldCapacityPercent",
					"fighterBonusShieldRechargePercent",
					"fighterBonusVelocityPercent"
				]
		},
	"requiredSkills":
		{
			"name": {"en": "Required Skills"},
			"component": "requiredSkills",
			"dogmaAttributes":
				[
					"requiredSkill1",
					"requiredSkill1Level",
					"requiredSkill2",
					"requiredSkill2Level",
					"requiredSkill3",
					"requiredSkill3Level",
					"requiredSkill4",
					"requiredSkill4Level",
					"requiredSkill5",
					"requiredSkill5Level"
				]
		},
	"insurance":
		{
			"name": {"en": "Insurance"},
			"component": "insurance",
			"alwaysShow": true
		},
	"overloading":
		{
			"name": {"en": "Overloading"},
			"dogmaAttributes":
				[
					"requiredThermoDynamicsSkill",
					"heatDamage",
					"overloadDamageModifier",
					"heatAbsorbtionRateModifier"
				]
		},
	"booster":
		{
			"name": {"en": "Booster"},
			"component": "booster",
			"dogmaAttributes":
				[
					"boosterDuration",
					"boosterLastInjectionDatetime",
					"boosterMaxCharAgeHours",
					"boosterness",
					"followsJumpClones",
					"intelligenceBonus",
					"perceptionBonus",
					"charismaBonus",
					"willpowerBonus",
					"memoryBonus"
				]
		},
	"capacitor":
		{
			"name": {"en": "Capacitor"},
			"dogmaAttributes":
				[
					"capacitorCapacity",
					"rechargeRate",
					"capacitorBonus",
					"capNeedBonus"
				]
		},
	"shield":
		{
			"name": {"en": "Shield"},
			"dogmaAttributes":
				[
					"shieldCapacity",
					"shieldRechargeRate",
					"shieldUniformity",
					"shieldEmDamageResonance",
					"shieldThermalDamageResonance",
					"shieldKineticDamageResonance",
					"shieldExplosiveDamageResonance",
				]
		},
	"armor":
		{
			"name": {"en": "Armor"},
			"dogmaAttributes":
				[
					"armorHP",
					"armorUniformity",
					"armorEmDamageResonance",
					"armorThermalDamageResonance",
					"armorKineticDamageResonance",
					"armorExplosiveDamageResonance",
				]
		},
	"structure":
		{
			"name": {"en": "Structure"},
			"dogmaAttributes":
				[
					"hp",
					"structureRequiresDeedType",
					"structureUniformity",
					"emDamageResonance",
					"thermalDamageResonance",
					"kineticDamageResonance",
					"explosiveDamageResonance",
					"strRefiningYieldBonus",
					"structureGasDecompressionEfficiencyBonus",
				]
		},
	"commandBurst":
		{
			"name": {"en": "Command Burst"},
			"dogmaAttributes":
				[
					"warfareBuff1ID",
					"warfareBuff1Multiplier",
					"warfareBuff2ID",
					"warfareBuff2Multiplier",
					"warfareBuff3ID",
					"warfareBuff3Multiplier",
					"warfareBuff4ID",
					"warfareBuff4Multiplier"
				]
		},
	"fuel":
		{
			"name": {"en": "Fuel"},
			"dogmaAttributes":
				[
					"serviceModuleFuelAmount",
					"serviceModuleFuelOnlineAmount",
					"serviceModuleFullPowerStateHitpointMultiplier",
					"serviceModuleFuelConsumptionGroup"
				]
		},
	"usage":
		{
			"name": {"en": "Usage"},
			"component": "usage",
			"alwaysShow": true,
			"dogmaAttributes":
				[
					"canFitShipGroup01",
					"canFitShipGroup02",
					"canFitShipGroup03",
					"canFitShipGroup04",
					"canFitShipGroup05",
					"canFitShipGroup06",
					"canFitShipGroup07",
					"canFitShipGroup08",
					"canFitShipGroup09",
					"canFitShipGroup10",
					"canFitShipType1",
					"canFitShipType2",
					"canFitShipType3",
					"canFitShipType4",
					"canFitShipType5",
					"canFitShipType6",
					"canFitShipType7",
					"canFitShipType8",
					"canFitShipType9",
					"canFitShipType10",
					"canFitShipType11"
				]
		},
	"mining":
		{
			"name": {"en": "Mining"},
			"dogmaAttributes":
				[
					"asteroidMetaLevel",
					"oreBasicType",
					"compressionQuantityNeeded",
					"compressionTypeID",
					"asteroidRadiusGrowthFactor",
					"asteroidRadiusUnitSize",
					"asteroidMaxRadius"
				]
		},
	"planetInteraction":
		{
			"name": {"en": "Planetary Interaction"},
			"component": "planetInteraction",
			"alwaysShow": true,
			"dogmaAttributes":
				[
					"exportTaxMultiplier",
					"importTaxMultiplier",
					"extractorDepletionRange",
					"extractorDepletionRate",
					"harvesterType",
					"pinCycleTime",
					"pinExtractionQuantity",
					"planetRestriction"
				]
		},
	"producedFrom":
		{
			"name": {"en": "Produced from"},
			"component": "producedFrom"
		},
	"blueprintManufacturing":
		{
			"name": {"en": "Manufacturing"},
			"component": "blueprintManufacturing"
		},
	"blueprintInvention":
		{
			"name": {"en": "Invention"},
			"component": "blueprintInvention"
		},
	"blueprintMaterialResearch":
		{
			"name": {"en": "Material Research"},
			"component": "blueprintMaterialResearch"
		},
	"blueprintTimeResearch":
		{
			"name": {"en": "Time Research"},
			"component": "blueprintTimeResearch"
		},
	"blueprintCopying":
		{
			"name": {"en": "Copying"},
			"component": "blueprintCopying"
		},
	"blueprintReaction":
		{
			"name": {"en": "Reaction"},
			"component": "blueprintReaction"
		},
	"engineeringRig":
		{
			"name": {"en": "Engineering Rig"},
			"component": "engineeringRig",
			"alwaysShow": true,
			"dogmaAttributes":
				[
					"attributeEngRigTimeBonus",
					"RefRigTimeBonus",
					"attributeEngRigMatBonus",
					"RefRigMatBonus",
					"attributeThukkerEngRigMatBonus",
					"attributeEngRigCostBonus",
					"hiSecModifier",
					"lowSecModifier",
					"nullSecModifier"
				]
		},
	"reprocessing":
		{
			"name": {"en": "Reprocessing"},
			"component": "reprocessing",
			"dogmaAttributes":
				[
					"reprocessingSkillType"
				]
		},
	"industry":
		{
			"name": {"en": "Industry"},
			"component": "industry"
		},
	"decryptor":
		{
			"name": {"en": "Decryptor"},
			"dogmaAttributes":
				[
					"inventionPropabilityMultiplier",
					"inventionMEModifier",
					"inventionTEModifier",
					"inventionMaxRunModifier"
				]
		},
	"other":
		{
			"name": {"en": "Other"}
		},
	"typeInfo":
		{
			"name": {"en": "Type Info"},
			"component": "typeInfo",
			"alwaysShow": true
		},
	// "skins":
	// 	{
	// 		"name": {"en": "Skins"},
	// 		"component": "skins",
	// 		"alwaysShow": true
	// 	},
	// "skin":
	// 	{
	// 		"name": {"en": "Skin"},
	// 		"component": "skin",
	// 		"alwaysShow": true
	// 	}
};

export default cards;
