export default class TypeLinkService {
    // Split string into component parts by <a> and <url> tags.
    private splitString(message: string): string[] {
        return message.split(/(<(?:a|url).*?<\/(?:a|url)>)/g);
    }

    public parse(message: string): (string|number)[] {
        message = message.replaceAll(/\r?\n/g, "<br/>");
        const components: string[] = this.splitString(message);

        return components.map((component: string): (string|number) => {
            if (component.startsWith('<a') || component.startsWith('<url')) {
                if (component.includes('//')) {
                    return component.replace(/<[^>]*>/g, '');
                }

                const match = component.match(/<(?:a|url).*?(\d+)[^\d]/);
                if (match && match.length > 1) {
                    return parseInt(match[1]);
                }
            }
            return component;
        });
    }
}
