import {
    ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
    BASIC_BOOST_CEREBRAL_ACCELERATOR,
    EXPERT_BOOST_CEREBRAL_ACCELERATOR,
    GENIUS_BOOST_CEREBRAL_ACCELERATOR,
    SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
    STANDARD_BOOST_CEREBRAL_ACCELERATOR
} from "~/lib/typeConstants";
import {DAY} from "~/lib/timeUtils";

export class OmegaPrice {
    months: number = 0;
    plex: number = 0;
}

export const omegaPrices: OmegaPrice[] = [
    { months: 1, plex: 500 } as OmegaPrice,
    { months: 3, plex: 1200 } as OmegaPrice,
    { months: 6, plex: 2100 } as OmegaPrice,
    { months: 12, plex: 3600 } as OmegaPrice,
    { months: 24, plex: 6600 } as OmegaPrice,
]

export class MctPrice {
    count: number = 0;
    plex: number = 0;
}

export const mctPrices: MctPrice[] = [
    { count: 1, plex: 350 } as MctPrice,
    { count: 3, plex: 800 } as MctPrice,
    { count: 6, plex: 1500 } as MctPrice,
    { count: 12, plex: 2700 } as MctPrice,
    { count: 24, plex: 4600 } as MctPrice,
]

export class AcceleratorPrice {
    typeId: number = 0;
    plex: number = 0;
}

export const acceleratorPrices: AcceleratorPrice[] = [
    { typeId: BASIC_BOOST_CEREBRAL_ACCELERATOR, plex: 5 } as AcceleratorPrice,
    { typeId: STANDARD_BOOST_CEREBRAL_ACCELERATOR, plex: 20 } as AcceleratorPrice,
    { typeId: ADVANCED_BOOST_CEREBRAL_ACCELERATOR, plex: 45 } as AcceleratorPrice,
    { typeId: SPECIALIST_BOOST_CEREBRAL_ACCELERATOR, plex: 80 } as AcceleratorPrice,
    { typeId: EXPERT_BOOST_CEREBRAL_ACCELERATOR, plex: 125 } as AcceleratorPrice,
    { typeId: GENIUS_BOOST_CEREBRAL_ACCELERATOR, plex: 180 } as AcceleratorPrice,
];

export class AlphaInjectorPrice {
    count: number = 0;
    plex: number = 0;
}

export const alphaInjectorPrices: AlphaInjectorPrice[] = [
    { count: 1, plex: 25 } as AlphaInjectorPrice,
    { count: 5, plex: 110 } as AlphaInjectorPrice,
    { count: 10, plex: 200 } as AlphaInjectorPrice,
];

export class PackPrice {
    name: string = "";
    plex: number = 0;
    omega: number = 0;
    includedSp: number = 0;
    accelerator: number = 0;
    skillExtractors: number = 0;
    mct: number = 0;
}

export const packPrices: PackPrice[] = [
    {
        name: "Apprentice Bundle",
        plex: 565,
        includedSp: 50_000,
        accelerator: GENIUS_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Novice Bundle",
        plex: 658,
        includedSp: 100_000 + 250_000,
        accelerator: EXPERT_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Graduate Bundle",
        plex: 820,
        includedSp: 2 * 100_000 + 2 * 250_000,
        accelerator: GENIUS_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Master Bundle",
        plex: 1530,
        includedSp: 1_000_000 + 3 * 250_000,
        accelerator: STANDARD_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Explorer Career Pack",
        plex: 275,
        omega: 14 * DAY,
        accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Industrialist Career Pack",
        plex: 350,
        omega: 14 * DAY,
        accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Enforcer Career Pack",
        plex: 425,
        omega: 14 * DAY,
        accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Soldier of Fortune Career Pack",
        plex: 475,
        omega: 14 * DAY,
        accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Warclone Omega Bundle",
        plex: 250,
        omega: 14 * DAY,
    } as PackPrice,
    {
        name: "Bronze Starter Pack",
        plex: 250,
        includedSp: 50_000,
        accelerator: SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Silver Starter Pack",
        plex: 500,
        includedSp: 250_000,
        omega: 30 * DAY,
        accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Gold Starter Pack",
        plex: 1000,
        includedSp: 500_000,
        omega: 60 * DAY,
        accelerator: STANDARD_BOOST_CEREBRAL_ACCELERATOR,
    } as PackPrice,
    {
        name: "Platinum Starter Pack",
        plex: 2000,
        includedSp: 500_000,
        omega: 90 * DAY,
        mct: 2,
        accelerator: SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
        skillExtractors: 10,
    } as PackPrice,
]
