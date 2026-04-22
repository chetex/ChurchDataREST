---
name: Project cache architecture direction
description: The API is being evolved into a scrape+cache intermediary backed by PostgreSQL, gated by a fingerprint-based "new elements" endpoint.
type: project
---

Project is pivoting from pure scraper to scrape+cache intermediary for a mobile app. New shape:

- `GET /api/status/new-elements` is the gate: mobile app calls it before any other request. Returns boolean.
- When `false`, other endpoints must serve cached JSON directly from PostgreSQL without scraping.
- When `true`, endpoints scrape, refresh the cache row, and return fresh data.
- Cache storage uses two JPA entities: `CachedResponse` (cache_key PK, payload TEXT, updated_at) and `ContentFingerprint` (scope PK, hash_value, updated_at).
- Fingerprint for the home scope is a SHA-256 over the sorted "absolute-url::title" pairs of `.hentry` posts; fallback hashes the raw body when no posts are detected.

**Why:** The mobile client wants fast startup and low bandwidth; the parish site is slow and rarely changes, so caching is the right optimisation.

**How to apply:** When adding a new scraping endpoint, also add a new `cache_key` and teach the endpoint to consult `CachedResponseRepository` before scraping. New fingerprint scopes should go through `ContentFingerprint` with a SHA-256 of stable, order-independent tokens.
