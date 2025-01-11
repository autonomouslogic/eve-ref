import {type DogmaAttribute, type DogmaTypeAttribute, type InventoryType} from "~/refdata-openapi";
import refdataApi from "~/refdata";

export async function loadDogmaAttributesForType(type: InventoryType): Promise<{ [key: string]: DogmaAttribute }> {
    const dogmaAttributes: { [key: string]: DogmaAttribute } = {};
    if (type.dogmaAttributes) {
        const promises = [];
        for (const attrId in type.dogmaAttributes) {
            promises.push((async () => {
                const attr = await refdataApi.getDogmaAttribute({attributeId: parseInt(attrId)});
                if (attr && attr.name) {
                    dogmaAttributes[attr.name] = attr;
                }
            })());
        }
        await Promise.all(promises);
    }
    return dogmaAttributes;
}

export function hasDogmaAttributeValue(name: string, type: InventoryType, attributes: DogmaAttribute[]): boolean {
    if (!type.dogmaAttributes) {
        return false;
    }
    const attribute = getAttributeByName(name, attributes);
    if (!attribute) {
        return false;
    }
    if (!attribute.attributeId) {
        return false;
    }
    return !!type.dogmaAttributes[attribute.attributeId.toString()];
}

export function getAttributeByName(name: string, attributes: DogmaAttribute[]): DogmaAttribute | undefined {
    return attributes.find((attribute) => attribute.name === name);
}

export function getTypeAttributeByName(name: string, type: InventoryType, attributes: DogmaAttribute[]): DogmaTypeAttribute | undefined {
    if (!hasDogmaAttributeValue(name, type, attributes)) {
        return undefined;
    }
    const attribute = getAttributeByName(name, attributes);
    return type.dogmaAttributes![attribute!.attributeId!.toString()];
}
