# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 3.4 / Java 17 REST API that scrapes a Catholic parish website (configured via `church.url` in `src/main/resources/application.properties`, default `https://www.parroquiasanpablovi.es/`) using Jsoup and returns structured JSON.

## Commands

```bash
./mvnw spring-boot:run                     # run app on :8080
./mvnw clean package                       # build jar -> target/ChurchDataREST-0.0.1-SNAPSHOT.jar
./mvnw test                                # run all tests
./mvnw test -Dtest=ClassName#method        # run a single test
docker compose up --build                  # containerized run (Dockerfile expects target/*.jar already built)
```

Note: the Dockerfile copies a pre-built jar — run `./mvnw clean package` before `docker compose up`.

## Architecture

The scraping logic follows a **Strategy pattern** to keep Jsoup parsing isolated from controllers and the service:

- `controller/WebScrapingController` — exposes `/api/navigation`, `/api/news`, `/api/detail?url=...`.
- `service/WebScrapingService` — orchestrates: opens the Jsoup `Document` (with a desktop User-Agent) and delegates extraction to a strategy. `getHomeNews()` first asks the navigation strategy for a link whose name contains `actualidad`/`noticias`/`blog`, scrapes that page, and falls back to scraping the home page if nothing is found.
- `strategy/ScrapingStrategy<T>` — generic interface; concrete impls `NavigationScrapingStrategy`, `NewsScrapingStrategy`, `DetailPageScrapingStrategy` are Spring beans injected into the service. Use `WebScrapingService.executeStrategy(url, strategy)` to run an arbitrary strategy against a URL.
- `dto/` — `NavigationItemDTO`, `NewsItemDTO`, `DetailPageDTO` are the JSON response shapes.

When adding a new scraping endpoint, add a new `ScrapingStrategy<T>` implementation rather than putting Jsoup selectors in the controller or service.

## Roadmap context (from CONTEXT.md)

The three endpoints above correspond to the three planned steps: navigation extraction, home news listing, and detail-page extraction. All three are implemented; future work should preserve the Strategy abstraction.
