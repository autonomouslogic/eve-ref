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
    accelerators: Accelerators[] = [];
    skillExtractors: number = 0;
    mct: number = 0;
}

export class Accelerators {
    accelerator: number = 0;
    count: number = 1;
}

export const packPrices: PackPrice[] = [
    {
        name: "Apprentice Bundle",
        plex: 280,
        includedSp: 250_000,
        accelerators: [
            {
            accelerator: GENIUS_BOOST_CEREBRAL_ACCELERATOR
        } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Novice Bundle",
        plex: 520,
        includedSp: 500_000,
        accelerators: [
            {
                accelerator: STANDARD_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators,
            {
                accelerator: EXPERT_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Graduate Bundle",
        plex: 735,
        includedSp: 250_000 + 500_000,
        accelerators: [
            {
                accelerator: SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
                count: 2
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Master Bundle",
        plex: 1510,
        includedSp: 1_000_000 + 500_000 + 250_000,
        accelerators: [
            {
                accelerator: STANDARD_BOOST_CEREBRAL_ACCELERATOR,
                count: 2
            } as Accelerators
            ],
    } as PackPrice,
    {
        name: "Prodigy Bundle",
        plex: 1154,
        includedSp: 1_000_000 + 250_000,
        accelerators: [
            {
                accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
                count: 2
            } as Accelerators
            ],
    } as PackPrice,
    {
        name: "Explorer Career Pack",
        plex: 275,
        omega: 14 * DAY,
        accelerators: [
            {
                accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Industrialist Career Pack",
        plex: 350,
        omega: 14 * DAY,
        accelerators: [
            {
                accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Enforcer Career Pack",
        plex: 425,
        omega: 14 * DAY,
        accelerators: [
            {
                accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Soldier of Fortune Career Pack",
        plex: 475,
        omega: 14 * DAY,
        accelerators: [
            {
                accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Bronze Starter Pack",
        plex: 250,
        includedSp: 50_000,
        omega: 14 * DAY,
        accelerators: [
            {
                accelerator: SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Silver Starter Pack",
        plex: 500,
        includedSp: 250_000,
        omega: 30 * DAY,
        accelerators: [
            {
                accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Gold Starter Pack",
        plex: 1000,
        includedSp: 500_000,
        omega: 60 * DAY,
        accelerators: [
            {
                accelerator: STANDARD_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
    } as PackPrice,
    {
        name: "Platinum Starter Pack",
        plex: 2000,
        includedSp: 500_000,
        omega: 90 * DAY,
        mct: 2,
        accelerators: [
            {
                accelerator: SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
                count: 1
            } as Accelerators
        ],
        skillExtractors: 10,
    } as PackPrice,
]
