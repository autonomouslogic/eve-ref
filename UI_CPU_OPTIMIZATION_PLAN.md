# UI CPU Optimization Plan (everef.net)

Status: proposal for review, 2026-10-22. Agreed scope: Phase 1 (cut CPU per render) and Phase 2 (cache SSR HTML at Cloudflare's CDN). Prerendering and bundle slimming are parked under "Notes for later review".

## Context

- Server CPU (Pages Functions), monthly percentiles: 76,352 / 182,316 / 828,483 / 1,554,578. The unit is **microseconds**: 1,554,578 ms would be ~26 min of CPU, above the Workers 5-minute maximum. So P50 76 ms, P75 182 ms, P99 0.83 s, P99.9 1.55 s. A lean Nuxt SSR render is ~5–20 ms.
- Billing: Pages Functions are billed as Workers Standard: $0.02 per 1M CPU-ms beyond 30M included, $0.30 per 1M requests beyond 10M. CDN cache hits and static assets cost nothing.
- Deploy: Cloudflare Pages Git integration. Nitro auto-selects the `cloudflare-pages` preset (`_worker.js` + generated `_routes.json`).
- Main traffic: `/types/[typeId]` (`ui/pages/types/[typeId]/index.vue`). `/type/[typeId]` (`ui/pages/type/[typeId].vue`) is only a 302 redirect page.

## Evidence

Live site (`https://everef.net/types/645`, Dominix):
- Uncompressed HTML is 1.69 MB, of which ≈1.53 MB is the `__NUXT_DATA__` payload (the entire type bundle). Repeated requests alternate between 1.69 MB and 158 KB: the module-level cache in `ui/refdata/index.ts` survives across requests per isolate. A cold isolate fetches the bundle through `useFetch`, which serializes it into the payload; a warm isolate hits the string cache and skips the payload.
- HTML responses are `cf-cache-status: DYNAMIC` and every response (including `robots.txt` and 404s) carries `set-cookie: i18n_redirected=…`.
- `/type/645` and `/market/4` return 302 from a full Nuxt boot. `/wp-login.php` with `Accept: text/html` renders a full 42 KB SSR 404 page. `/robots.txt` contains only Cloudflare's content-signal preamble, with no rules. `/_nuxt/*` is correctly served as static assets.
- Each page inlines ~40 KB of CSS in `<head>`.
- `https://ref-data.everef.net/types/645/bundle` is 1.44 MB with 46 types. 820 KB of it (57%) is the `used_in_blueprints` of the 7 minerals pulled in by the reprocessing card. 245 KB is descriptions in 8 languages for all 46 types, of which only one is displayed. `groups/25` is 1.1 MB and `groups/18` is 1.0 MB. Ref-data is served with `cache-control: max-age=120` and rebuilt daily (`/meta` `build_time` 00:10 UTC).
- There are 53,290 type IDs × 8 locale URL variants (`prefix_and_default`), ≈426k crawlable type URLs, and the pages emit no hreflang tags.

Local benchmarks (Node, with the real bundle and the generated client bundled through esbuild):

| Cost center | Now | After fix |
|---|---|---|
| `devalue.stringify` of the SSR payload (Nuxt) | 37 ms | 0 (not in payload) |
| Bundle pipeline in `refdata/index.ts`: parse → `JSON.stringify` → `Response.json()` → `BundleFromJSON` → per-entity `ToJSON` + `JSON.stringify` | 52 ms cold, ~40 ms again on every warm-isolate hit | ~10 ms (one parse, linear convert) |
| `BundleFromJSON` for `groups/25` (85 types) | 94 ms | 9 ms (linear `mapValues`) |
| One `getType` cache hit (string → `Response.json()` → `InventoryTypeFromJSON`). The Dominix page makes ~150 (51 type links × 2 lookups, skill tree, variations). | ~1 ms (645), ~0.9 ms (Tritanium) | ~0 (object cache) |
| 200 × `new Intl.NumberFormat(...)` vs one cached instance | 5.8 ms | 0.09 ms |

Estimate: a cold Dominix render takes ~150–250 ms today and should take ~25–40 ms after Phase 1. Confirm this with the harness under Verification.

## Root causes (ranked by CPU impact)

1. **The whole bundle lands in the SSR payload.** `useFetch` inside the generated client's `fetchApi` (`ui/refdata/index.ts:93`, and the same pattern in `ui/esi/index.ts:5`) registers each fetch as asyncData, which devalue then serializes (~1.4 MB).
2. **JSON round-trips and a string cache.** The cache stores JSON strings, so every lookup re-parses and re-converts. Every bundle cache hit also re-runs `cacheBundleObj`, which does `ToJSON` + stringify for every entity.
3. **Quadratic `mapValues`.** `ui/refdata-openapi/runtime.ts:339` uses `reduce` with object spread. This comes from generator 6.6.0, pinned in `ui/openapitools.json`; the current generator template uses a plain loop. It affects every map (types, dogma attributes, effects) and is the main driver of the P99 tail on group and market-group pages.
4. **Bundle bloat.** `BundleRenderer` (`src/main/java/com/autonomouslogic/everef/cli/publishrefdata/bundle/BundleRenderer.java`) embeds the full type JSON for every referenced type.
5. **Unbounded per-isolate cache.** `const cache = {}` grows by ~2 MB of strings per heavy page. That means GC pressure and 128 MB isolate eviction, followed by cold starts. It likely contributes to P99.9.
6. **`Intl.NumberFormat` is constructed for every number** (`ui/components/helpers/FormattedNumber.vue`).
7. **Redirect pages boot the whole app** (`ui/pages/type/[typeId].vue`, `ui/pages/market/[marketGroupId].vue`). They return 302, which browsers do not cache.
8. **Junk traffic reaches SSR**: scanner paths, `apple-touch-icon.png`, `sitemap.xml`.
9. **The `i18n_redirected` cookie on every response** blocks CDN caching.
10. **Smaller render costs:**
    - `TypeCards.vue` instantiates ~45 card components per type page.
    - Hundreds of async leaf components, including double lookups in TypeLink→TypeName, GroupLink→GroupName and MarketGroupLink→MarketGroupName.
    - 5 FontAwesome SVGs per required-skill row.
    - One `useLocalePath()` route resolve per link.
    - The time-dependent `MotdBanner` is rendered on the server.

## Decisions

- i18n browser-language detection: **disable** (`detectBrowserLanguage: false`). There will be no cookie and no auto-redirect from `/` based on `Accept-Language`.
- Ref-data bundles: **unchanged for now**. See Notes.
- Prerendering: **deferred**. See Notes.
- Staleness: HTML may be cached for **up to 1 hour**.

## Phase 1 — Cut CPU per render (UI code)

1. **Fix `mapValues`.** Bump the generator in `ui/openapitools.json` from 6.6.0 to the current 7.x and regenerate both clients (`make openapi-ui`). Fix any type errors, for example the `TraitBonusFromJSON` use in `ui/components/types/traits/TypeTraits.vue`. Confirm that `runtime.ts` `mapValues` is now a loop. If the upgrade causes too much churn, fall back to a post-generate patch step in the `generate-api` npm script.
2. **Cache number formatters.** Add `ui/lib/numberFormat.ts`: a `Map` keyed by min decimals, max decimals and notation, following the module-level formatter pattern in `ui/lib/money.ts`. Use it in `FormattedNumber.vue`.
3. **Rewrite the refdata layer as an object store.**
   - Put the pure logic in `ui/lib/refdataStore.ts` with relative imports and injectable fetch and clock, so plain vitest can test it (there is no vitest config and the existing tests use relative imports).
   - Keep the exports of `ui/refdata/index.ts` unchanged so the ~50 call sites do not change. That covers the default `refdataApi` with `getType`, `getGroup`, `getCategory`, `getMarketGroup`, `getMetaGroup`, `getDogmaAttribute`, `getUnit`, `getIcon`, `getSkill`, `getBlueprint`, `getSchematic`, `getAllDogmaAttributes`, `getAllCategories`, `getAllMetaGroups` and `getRootMarketGroups`, plus the `cache*Bundle` functions.
   - Use the generated `RefdataApi` with plain `fetch` (drop the `useFetch` override). Nothing enters the SSR payload; the client fetches the bundle itself during hydration, as it already does on warm isolates today.
   - Store converted model objects keyed by path and dedupe in-flight promises. Bound memory with an LRU of ~5,000 entries and a ~10 min TTL.
   - Bundle ingestion: run `BundleFromJSON` once and insert entities directly, with no ToJSON or stringify.
     - The requested (primary) entity always overwrites.
     - Referenced entities are inserted only if absent, and their unused heavy fields (`usedInBlueprints`, `description`) are dropped, so a partial copy never replaces a full one.
   - Treat cached objects as immutable. Replace the `JSON.parse(JSON.stringify(...))` in `ui/components/types/TypeCards.vue` with a shallow copy.
4. **`ui/esi/index.ts`:** remove `useFetch` the same way and use plain fetch. Most calls are client-only, but `ui/pages/giveaways.vue` calls ESI during SSR.
5. **Redirects without SSR.** In `ui/nuxt.config.ts` `routeRules`, redirect `'/type/**' → '/types/**'` and `'/market/**' → '/market-groups/**'` with status 301, plus locale-prefixed variants generated from the locale list. Add `headers: { 'cache-control': 'public, max-age=86400' }` so the CDN caches them; Nitro sets route-rule headers before redirecting. Delete the two redirect pages.
6. **Serve static junk without the Function.** Add `ui/public/robots.txt` (allow all, disallow `/search`) and `ui/public/apple-touch-icon.png` plus `apple-touch-icon-precomposed.png`, made from `assets/logo.png`. Nitro's Pages preset already excludes every public file from Functions in `_routes.json`. Manual dashboard step: a WAF custom rule blocking `*.php`, `/wp-*`, `/.env` and `/.git` before Pages.
7. **Optional render trims, if profiling shows they matter:**
   - `TypeLink` renders the name from its already-loaded type instead of mounting `TypeName`; same for `GroupLink` and `GroupName`.
   - `RequiredSkillsRow` draws its squares with CSS instead of FontAwesome.
   - Try `features.inlineStyles: false`, which removes ~40 KB of CSS per HTML page.

## Phase 2 — Cache SSR HTML at Cloudflare's CDN (no Function invocation on a hit)

**Mechanism.** On a Pages custom domain, zone Cache Rules sit in front of Pages. Cloudflare's Pages docs warn that "the cached response might get served to your end user before Pages can act on the request". That means a cache hit never invokes the Function: zero CPU and no request charge. It also beats the Cache API, which caches per data center only and has no Tiered Cache. Hit ratio depends on traffic, so measure it.

**Where the cache headers must come from (`_headers` vs the Function).** Based on [Pages: Headers](https://developers.cloudflare.com/pages/configuration/headers/) and live checks on 2026-10-22.

Today:
- SSR HTML (e.g. `/types/645`) sends no `Cache-Control` at all, so Cloudflare reports `cf-cache-status: DYNAMIC`.
- Errors send `cache-control: no-cache`; `/types/999999999` returns 500 with that header, for both HTML and JSON.
- `/_nuxt/*` gets `public, max-age=31536000, immutable` from a `_headers` file that Nitro generates. Nitro turns the public-asset `maxAge` into route-rule `headers`, and `writeCFHeaders` in `nitropack/dist/presets/cloudflare/utils.mjs` writes them out.
- Other static files (`favicon.ico` etc.) get the Pages default `public, max-age=0, must-revalidate`.

Recommendations:
- **`_headers` cannot make SSR pages cacheable.** The Pages docs say `_headers` rules "are not applied to responses generated by Pages Functions". That covers advanced-mode `_worker.js`, which is what Nitro's `cloudflare-pages` preset emits, and SSR frameworks. Headers for those responses "should be sent from the Pages Functions' `Response`". So the cache headers for `/types/*`, `/groups/*`, etc. must be set by Nitro at runtime.
- **Don't use `routeRules` `headers` for HTML either.**
  - Nitro sets them before rendering, so they would also land on error renders unless something overrides them.
  - Nitro also copies them into the generated `_headers`, where they have no effect on Function responses but count toward the 100-rule limit.
  - Use the `render:response` plugin below instead; it acts on status 200 only.
- **Header for cacheable 200 HTML: `Cache-Control: public, max-age=0, s-maxage=3600`.**
  - `s-maxage` sets Cloudflare's 1 h edge TTL.
  - `max-age=0` makes browsers revalidate, so no browser holds stale HTML across a deploy; the revalidation is served from the CDN cache.
  - Keep errors on Nitro's `no-cache`.
  - Optional later: add `stale-while-revalidate=600` after verifying how Cloudflare handles it.
- **Exception: the 301 redirects.** Route-rule `headers` are fine on the `/type/**` and `/market/**` redirect rules (Phase 1 step 5). Redirects are deterministic and have no error path; Nitro sends the header from the Function at runtime.
- **Use `_headers` for static files only.**
  - Keep Nitro's generated `/_nuxt/*` rule.
  - Optionally add `ui/public/_headers` with `Cache-Control: public, max-age=86400` for `robots.txt`, `favicon.ico`, `apple-touch-icon*.png` and `ads.txt`. Nitro prepends an existing `_headers` file to its generated rules.
  - This only saves browser revalidations. Static asset requests are free and never invoke the Function, so there is no CPU effect. Low priority.

Repo changes:
1. Set `i18n.detectBrowserLanguage: false` in `ui/nuxt.config.ts`. This removes `Set-Cookie`; Cloudflare never caches responses that carry `Set-Cookie`.
2. Add a Nitro plugin, `ui/server/plugins/cache-headers.ts`, that hooks `render:response` (`nitropack/dist/runtime/internal/renderer.mjs`; headers are applied after the hook).
   - For status 200 HTML only, it sets `Cache-Control: public, max-age=0, s-maxage=3600`: browsers revalidate, and the edge keeps the page 1 h.
   - Deny-list: `/search`. `/giveaways` is a candidate if 1 h staleness turns out to matter there.
   - Errors keep Nitro's `no-cache` and are never cached.
3. Wrap the `MotdBanner` content in `<ClientOnly>`. It is time-based (hour-of-day rotation, date windows), so cached HTML would freeze it; client-only rendering also saves SSR work.
4. Keep the query string in the cache key (Cloudflare's default). `SearchBar` pre-fills its input from `?query=` on every page, so ignoring the query would let one request inject text that others then see.

**Status (2026-10-22): repo changes 1–4 deployed.** `curl` confirms `Cache-Control` now present on 200 HTML (`max-age` since adjusted to `5` on disk, `s-maxage=3600` unchanged) and no `Set-Cookie`. `cf-cache-status: DYNAMIC` still shows on repeat requests — expected at this point: the dashboard Cache Rule below has not been created yet, so the zone has nothing telling it the response is cache-eligible; the header alone does not cache a response. This step does not change per-request CPU or latency — it only removes Function invocations on a cache HIT, which only exist once the rule is live. The metric to watch going forward is Pages Functions invocation count and the zone cache hit ratio over the following days, not request latency.

**Follow-up (2026-10-22): added `immutable` to the header** (`public, max-age=5, s-maxage=3600, immutable`), trying to address invocation count not dropping. Caveat: `immutable` is a browser hint (skips revalidation on back/forward nav, honored mainly by Firefox on reload) — it isn't a signal Cloudflare's edge cache-eligibility reads. If invocations still don't drop after this, the likely cause is still the dashboard Cache Rule (not yet created/matching), not the response header.

Dashboard configuration (manual; record it in `AGENTS.md`):
1. Caching → Tiered Cache → Smart Tiered Cache Topology: on (free). With one upper tier in front of the Pages origin, each URL renders roughly once per hour globally instead of once per data center.
2. Cache Rule "SSR HTML":
   - Match: `http.host eq "everef.net" and not starts_with(http.request.uri.path, "/_nuxt/")`.
   - Cache eligibility: Eligible for cache.
   - Edge TTL: "Use cache-control header if present, bypass cache if not", so only responses the plugin opted in get cached.
   - Browser TTL: respect origin.
3. Pages → Settings → Build watch paths: include `ui/*`, so Java-only commits do not redeploy and purge.

Deploy staleness: cached HTML references hashed `/_nuxt/*` files from the previous deployment. Purge after each production deploy.
- Preferred: a GitHub workflow on `deployment_status` (success, Production), if the Pages GitHub app posts deployment statuses. Check the repo's Environments tab.
- The workflow calls `POST /zones/{zone_id}/purge_cache` with `{"hosts":["everef.net"]}`. Check that purge by hostname is available on the plan; otherwise use purge everything.
- Secrets: `CLOUDFLARE_ZONE_ID` and a token with Zone → Cache Purge only.
- Alternative: a Cloudflare Notification webhook on Pages deploy success that triggers the purge.

Rollout:
1. Ship the repo changes.
2. Scope the rule to `/about` first.
3. `curl` twice and expect `MISS` then `HIT`, and confirm that Pages Functions invocations do not rise for hits.
4. Widen the rule to the host.

Fallback, if zone caching proves unworkable with Pages Functions: a Cache API lookup and store inside the Function, keyed on URL plus Nuxt `buildId`. It caches per data center only.

## Execution order

1. One small PR, `perf(ui): …`, with: i18n detection off, cache-header plugin, `MotdBanner` client-only, formatter cache, 301 redirects, robots.txt and icons. Then the dashboard rollout above. This is the biggest win for the least code.
2. A separate PR for the generator upgrade (`mapValues`), because the generated code churns.
3. A separate PR for the refdata store rewrite plus the ESI fetch change. This is the largest code change and lowers the cost of every cache miss.
4. After each step, compare Pages Functions CPU percentiles, invocation count and the zone cache hit ratio with the baseline above.

## Verification

- **Local CPU harness:**
  - Build with `NITRO_PRESET=node-server npm run build` and run `node --cpu-prof .output/server/index.mjs`.
  - Hit `/types/645`, `/types/587`, `/groups/25` and `/market-groups/1857` with `npx autocannon`.
  - Compare CPU per request (`process.cpuUsage` delta divided by requests) and the flame graphs before and after each PR.
  - Cross-check on workerd with `npx wrangler pages dev dist` and the DevTools profiler.
- **`curl` checks:**
  - `__NUXT_DATA__` is small (no bundle).
  - No `Set-Cookie`.
  - `Cache-Control: public, max-age=0, s-maxage=3600` on 200 HTML only.
  - `/types/999999999` still returns `cache-control: no-cache`.
  - `/_nuxt/*` is still `immutable`.
  - `/type/645` returns 301 with `Location: /types/645`.
  - `/robots.txt` is static.
  - `cf-cache-status: HIT` on repeat requests after the rule is enabled.
- **Vitest (`ui/tests/`), written first:**
  - refdata store: bundle ingest, primary overwrite versus no clobber of referenced entities, LRU eviction, TTL expiry, in-flight dedupe.
  - The formatter cache.
  - No assert messages (repo convention).
- `make lint`, `make format`, `make build-ui` (which runs the vue-tsc type check), `make test-ui`.
- **Manual checks:**
  - Client-side navigation between types (partial vs full entities).
  - Hydration without console errors.
  - Localized names on `/de/types/645`.
  - Market and insurance cards still load client-side.
- **After deploy:** watch CPU percentiles, the `exceededMemory` outcome and the cache hit ratio for a week.

## Notes for later review

- **Prerender selected pages, not everything.**
  - Constraints: Pages builds time out at 20 min, the per-deployment file limit applies, and `_routes.json` is capped at 100 rules. Nitro trims excludes beyond that, so prerendered pages that are not excluded still invoke the Function, which then serves the asset cheaply.
  - Prerendered HTML needs the site URL from config for `og:url` (`ui/plugins/head.ts` uses `useRequestURL()`), `experimental.payloadExtraction: false`, and a deterministic `buildId` to avoid re-uploading unchanged pages.
  - Once the CDN cache is live, prerendering only helps on cache misses. Re-evaluate using the hit ratio and the top paths from GoatCounter or analytics.
- **Slim bundles (Java, public format change).**
  - In `BundleRenderer`, emit referenced types without `used_in_blueprints`, `description`, `traits`, `masteries`, `dogma_effects`, `can_be_fitted_with_types`, `can_fit_types` and `engineering_rig_*`. Keep `dogma_attributes` for variations.
  - The Dominix bundle would drop from 1.44 MB to an estimated <250 KB, cutting parse cost, client download and memory.
  - Bundles are in `spec/reference-data.yaml`, so either accept the format change or publish a separate UI-only file.
  - Phase 1 drops the same fields client-side, but the full download and parse remain.
- **Traffic hygiene.**
  - Check the bot share in Security Analytics and block AI training crawlers (AI Crawl Control).
  - Reconsider 8 locale URL variants without hreflang tags. Only names and descriptions are translated, yet the crawl surface is multiplied by 8.
- **Latency, not CPU.** The ref-data `max-age=120` causes 0.4–1.4 s subrequests and a 1–3 s TTFB on misses. Raise the TTL and purge on publish.
- **Deeper render work.**
  - Have `TypeCards.vue` filter the cards it will show before instantiating them.
  - Make leaf components (TypeLink, UnitValue, AttributeTypeIcon, DogmaAttributeLink) synchronous once the store is preloaded, removing hundreds of async setups per page.
  - Structures pages load a full bundle for every compared type (`CompareTable` `loadBundles` defaults to true).
