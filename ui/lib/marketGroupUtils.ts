import refdataApi from "~/refdata";

/**
 * Returns all the type IDs for the supplied marekt group and all its children.
 * @param marketGroupId
 */
export async function getRecursiveMarketGroupTypeIds(marketGroupId: number): Promise<number[]> {
    const marketGroup = await refdataApi.getMarketGroup({marketGroupId});
    let typeIds: number[] = [];
    if (marketGroup.typeIds) {
        const groupTypeIds = marketGroup.typeIds;
        groupTypeIds.sort();
        typeIds = typeIds.concat(groupTypeIds);
    }
    if (marketGroup.childMarketGroupIds) {
        for (let childMarketGroupId of marketGroup.childMarketGroupIds) {
            const childTypeIds = await getRecursiveMarketGroupTypeIds(childMarketGroupId);
            typeIds = typeIds.concat(childTypeIds);
        }
    }
    return typeIds;
}
