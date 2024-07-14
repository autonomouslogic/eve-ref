import {i18nConfigGlobalMessages} from "~/i18n.config";

const ERROR = "[unknown translation]";
const fallbackLocale = "en";
const locales = Object.keys(i18nConfigGlobalMessages);

export function prepMessages(messages: { [key: string]: string } | undefined): { [key: string]: string } {
    if (messages === undefined) {
        return {};
    }
    const defaultMessage = messages[fallbackLocale];
    for (let locale of locales) {
        if (!messages[locale]) {
            messages[locale] = defaultMessage;
        }
    }
    return messages;
}

export function translate (translations: { [key: string]: string } | undefined, locale: string) {
    if (!translations) {
        return ERROR;
    }
    const msg = translations[locale];
    if (!msg && msg !== "") {
        return msg;
    }
    return translations[fallbackLocale] || ERROR; // should use fallbackLocale
}
