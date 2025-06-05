import refdataApi from "~/refdata";
import {ANCIENT_RELICS, BLUEPRINT, SKINS} from "~/lib/categoryConstants";

export const MARKEE_DRAGON_URL = "https://store.markeedragon.com/affiliate.php?id=933&redirect=index.php?cat=4";
export const EVE_REFERRAL_URL = "https://www.eveonline.com/signup?invc=b28d194d-7181-4bf0-8e3f-72cebbc7ca7d";
export const HETZNER_REFERAL_URL = "https://hetzner.cloud/?ref=cDNPHOBYSP52";
export const PATREON_URL = "https://patreon.com/everef";
export const EVE_STORE_URL = "https://store.eveonline.com/";
export const DATASETS_DOCS_URL = "https://docs.everef.net/datasets/";
export const DATE_TOVIKOV_CHAR = "https://evewho.com/character/1452072530";
export const TOVIKOV_INTERSTELLAR_CORP = "https://evewho.com/corporation/98544114";
export const EVE_REF_CHAR = "https://evewho.com/character/2113778331";
export const EVE_REF_CORP = "https://evewho.com/corporation/98547654";
export const DISCORD_URL = "/discord";
export const REAL_DISCORD_URL = "https://discord.gg/fZYPAxFyXG";
export const GITHUB_URL = "https://github.com/autonomouslogic/eve-ref/";
export const YOUTUBE_URL = "https://www.youtube.com/@eve-ref";

export async function getTypeIconUrl(typeId: number, variation?: string): Promise<string> {
    const inventoryType = await refdataApi.getType({typeId});
    if (inventoryType.categoryId == SKINS) {
        return "";
    }
    if (!variation) {
        switch (inventoryType.categoryId) {
            case BLUEPRINT:
                variation = "bp";
                break;
            case ANCIENT_RELICS:
                variation = "relic";
                break;
            default:
                variation = "icon";
                break;
        }
    }
    return `https://images.evetech.net/types/${typeId}/${variation}`;
}
