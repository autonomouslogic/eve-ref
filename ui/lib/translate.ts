const fallbackLocale = "en";

export function tr(messages: { [key: string]: string } | undefined, locale: string): string | undefined {
	if (!messages) {
		return undefined;
	}
	if (messages.hasOwnProperty(locale)) {
		return messages[locale];
	}
	return messages[fallbackLocale];
}
