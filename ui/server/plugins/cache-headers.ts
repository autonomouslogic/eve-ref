const DENY_LIST_PREFIXES = ['/search'];

export default defineNitroPlugin((nitroApp) => {
	nitroApp.hooks.hook('render:response', (response, { event }) => {
		if (response.statusCode !== 200) {
			return;
		}

		const contentType = response.headers?.['content-type'];
		if (!contentType || !String(contentType).includes('text/html')) {
			return;
		}

		const path = event.path.split('?')[0];
		if (DENY_LIST_PREFIXES.some((prefix) => path.startsWith(prefix))) {
			return;
		}

		response.headers = {
			...response.headers,
			'cache-control': 'public, max-age=5, s-maxage=3600, immutable',
		};
	});
});
