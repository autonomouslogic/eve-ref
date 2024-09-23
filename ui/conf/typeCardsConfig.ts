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
			"name": {"en": "Module"},
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
			"name": {"en": "Fitting"},
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
			"name": {"en": "Structure"},
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
					"chargeGroup1",
					"chargeGroup2",
					"chargeGroup3"
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
					"droneBandwidth"
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
	"capBooster":
		{
			"name": {"en": "Cap Booster"},
			"dogmaAttributes":
				[
					"capacitorBonus",
					"capNeedBonus"
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
	"crystal":
		{
			"name": {"en": "Crystal"},
			"dogmaAttributes":
				[
					"crystalsGetDamaged",
					"crystalVolatilityChance",
					"crystalVolatilityDamage"
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
	"reprocessing":
		{
			"name": {"en": "Reprocessing"},
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
			"name": {"en": "Other"}
		},
	"typeInfo":
		{
			"name": {"en": "Type Info"},
			"component": "typeInfo",
			"alwaysShow": true
		},
	"skins":
		{
			"name": {"en": "Skins"},
			"component": "skins",
			"alwaysShow": true
		},
	"skin":
		{
			"name": {"en": "Skin"},
			"component": "skin",
			"alwaysShow": true
		}
};

export default cards;
