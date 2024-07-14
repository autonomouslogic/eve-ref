const ERROR = "[unknown translation]";

export function translate (translations: { [key: string]: string } | undefined, locale: string) {
    const fallbackLocale = "en";
    if (!translations) {
        return ERROR;
    }
    const msg = translations[locale];
    if (!msg && msg !== "") {
        return msg;
    }
    return translations[fallbackLocale] || ERROR; // should use fallbackLocale
}
