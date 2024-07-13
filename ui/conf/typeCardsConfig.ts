const cards: {[key: string]: any} = {
	"defenses":
		{
			"name": {"en": "defenses"},
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
	"market":
		{
			"name": {"en": "market"},
			"component": "market"
		},
	"basic":
		{
			"name": {"en": "basic"},
			"component": "basic",
			"dogmaAttributes":
				[
					"techLevel",
					"metaLevel",
					"metaLevelOld",
					"capacity",
					"volume",
					"mass",
					"radius",
					"nondestructible",
					"cargoScanResistance",
					"disallowInHighSec",
					"disallowInEmpireSpace",
					"isCapitalSize",
				]
		},
	"offenses":
		{
			"name": {"en": "offenses"},
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
	"npc":
		{
			"name": {"en": "npc"},
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
					"shieldRechargeRate",
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
			"name": {"en": "module"},
			"dogmaAttributes":
				[
					"duration",
					"maxGroupFitted",
					"maxGroupActive",
					"disallowEarlyDeactivation",
					"disallowRepeatingActivation",
					"canActivateInGateCloak",
					"moduleReactivationDelay",
					"signatureRadiusAdd"
				]
		},
	"variations":
		{
			"name": {"en": "variations"},
			"component": "variations",
			"alwaysShow": true
		},
	"skill":
		{
			"name": {"en": "skill"},
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
			"name": {"en": "cargo"},
			"dogmaAttributes":
				[
					"fleetHangarCapacity",
					"specialOreHoldCapacity",
					"specialPlanetaryCommoditiesHoldCapacity",
					"shipMaintenanceBayCapacity",
					"specialFuelBayCapacity",
					"specialMineralHoldCapacity",
					"hasFleetHangars",
					"hasShipMaintenanceBay"
				]
		},
	"fitting":
		{
			"name": {"en": "fitting"},
			"dogmaAttributes":
				[
					"powerOutput",
					"cpuOutput",
					"capacitorCapacity",
					"rechargeRate",
					"turretSlotsLeft",
					"launcherSlotsLeft",
					"hiSlots",
					"medSlots",
					"lowSlots",
					"rigSlots",
					"upgradeSlotsLeft",
					"rigSize",
					"upgradeCapacity",
					"serviceSlots",
					"cpu",
					"power",
					"capacitorNeed",
					"maxTypeFitted"
				]
		},
	"structure":
		{
			"name": {"en": "structure"},
			"dogmaAttributes":
				[
					"tetheringRange",
					"shieldDamageLimit",
					"armorDamageLimit",
					"structureDamageLimit",
					"structureAoERoFRoleBonus",
					"structureRoleBonus",
					"structureServiceRoleBonus",
					"structureFullPowerStateHitpointMultiplier"
				]
		},
	"probe":
		{
			"name": {"en": "probe"},
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
			"name": {"en": "targeting"},
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
			"name": {"en": "weapon"},
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
					"chargeGroup1",
					"chargeGroup2",
					"chargeGroup3"
				]
		},
	"ammunition":
		{
			"name": {"en": "ammunition"},
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
					"weaponRangeMultiplier",
					"empFieldRange",
					"aoeCloudSize",
					"explosionDelay",
					"aoeFalloff",
					"aimedLaunch",
					"explosionRange",
					"detonationRange"
				]
		},
	"navigation":
		{
			"name": {"en": "navigation"},
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
			"name": {"en": "drones"},
			"dogmaAttributes":
				[
					"droneCapacity",
					"droneBandwidth"
				]
		},
	"fighters":
		{
			"name": {"en": "fighters"},
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
			"name": {"en": "fighterSupport"},
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
			"name": {"en": "requiredSkills"},
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
			"name": {"en": "insurance"},
			"component": "insurance",
			"alwaysShow": true
		},
	"overloading":
		{
			"name": {"en": "overloading"},
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
	"capBooster":
		{
			"name": {"en": "capBooster"},
			"dogmaAttributes":
				[
					"capacitorBonus",
					"capNeedBonus"
				]
		},
	"commandBurst":
		{
			"name": {"en": "commandBurst"},
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
	"crystal":
		{
			"name": {"en": "crystal"},
			"dogmaAttributes":
				[
					"crystalsGetDamaged",
					"crystalVolatilityChance",
					"crystalVolatilityDamage"
				]
		},
	"fuel":
		{
			"name": {"en": "fuel"},
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
			"name": {"en": "usage"},
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
			"name": {"en": "mining"},
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
	"reprocessing":
		{
			"name": {"en": "reprocessing"},
			"component": "reprocessing",
			"dogmaAttributes":
				[
					"reprocessingSkillType"
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
	"typeManufacturing":
		{
			"name": {"en": "Manufacturing"},
			"component": "typeManufacturing"
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
	"blueprintResearch":
		{
			"name": {"en": "Research"},
			"component": "blueprintResearch"
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
	"industry":
		{
			"name": {"en": "Industry"},
			"component": "industry"
		},
	"other":
		{
			"name": {"en": "other"}
		},
	"typeInfo":
		{
			"name": {"en": "typeInfo"},
			"component": "typeInfo",
			"alwaysShow": true
		},
	"skins":
		{
			"name": {"en": "skins"},
			"component": "skins",
			"alwaysShow": true
		},
	"skin":
		{
			"name": {"en": "skin"},
			"component": "skin",
			"alwaysShow": true
		}
};

export default cards;
