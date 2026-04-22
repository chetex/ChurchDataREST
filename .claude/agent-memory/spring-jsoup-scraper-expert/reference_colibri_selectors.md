---
name: Colibri WP selector crib sheet for parroquiasanpablovi.es
description: Stable Jsoup selectors observed on the parish site (Colibri WP theme), for posts, titles, dates, thumbnails and pagination.
type: reference
---

The parish website is built with the Colibri WP WordPress theme. Selectors that work reliably for scraping:

- Post listing container: `.hentry` (one per post on home / category / archive listings).
- Post title: `.h-blog-title` — anchor inside is the canonical post URL (`abs:href`).
- Excerpt: `.colibri-post-excerpt`.
- Thumbnail: `.colibri-post-thumbnail img` (also consider `abs:data-src` for lazy-loaded images).
- Date: `.h-blog-meta .metadata-item` (rendered in Spanish, e.g. "marzo 29, 2026").
- Pagination links: `a.page-numbers[href]` (standard WordPress).
- Static-page fallback headings: `.h-heading, h1, h2, h3` grouped by closest `.h-element, .h-x-container, section, div` container.

Nav menu selectors (see `NavigationScrapingStrategy`): primary is `nav.elementor-nav-menu--main > ul > li`, with progressively generic fallbacks.

Use these as the default starting point for any new `ScrapingStrategy` targeting this site.
