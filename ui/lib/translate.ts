import {locales} from "~/i18n.config";

const ERROR = "[unknown translation]";
const fallbackLocale = "en";

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
