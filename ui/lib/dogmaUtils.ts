import {DogmaAttribute, InventoryType, DogmaTypeAttribute} from "~/refdata-openapi";

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
