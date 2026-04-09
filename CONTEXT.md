# CONTEXT.md — ChurchDataREST

Documento maestro de contexto del proyecto. Toda IA o desarrollador que trabaje en este repositorio debe leerlo antes de proponer cambios.

## Tech Stack

Detectado en `pom.xml`:

- **Java:** 17
- **Spring Boot:** 3.4.0 (`spring-boot-starter-web`, `spring-boot-starter-test`, `spring-boot-devtools`)
- **Web Scraping:** Jsoup 1.18.1
- **Testing:** Spring Boot Test + Hamcrest 1.3
- **API Docs:** Springfox 3.0.0 *(⚠️ incompatible con Spring Boot 3.x — se recomienda migrar a `springdoc-openapi-starter-webmvc-ui`)*
- **Build:** Maven (`spring-boot-maven-plugin`)
- **Contenedores:** Dockerfile + docker-compose (requiere `./mvnw clean package` previo)

### Dependencias clave que faltan o convendría añadir
- **Lombok** — eliminar boilerplate en DTOs y servicios.
- **springdoc-openapi-starter-webmvc-ui** — sustituir Springfox (roto en Boot 3).
- **spring-boot-starter-validation** — validar parámetros (`@Valid`, `@NotBlank`) en endpoints como `/api/detail?url=`.
- *(Opcional)* **Caffeine / spring-boot-starter-cache** — cachear respuestas de scraping para no martillear el sitio origen.
- Jackson ya viene transitivo con `starter-web`, no requiere acción.

## Arquitectura y Patrones

Separación estricta en capas:

```
controller/   → REST endpoints. Sin Jsoup. Sin lógica de negocio.
service/      → Orquestación. Abre el Document Jsoup y delega en estrategias.
strategy/     → Lógica de scraping aislada por tipo de página.
dto/          → Contratos JSON de salida (inmutables).
```

### Patrón Strategy
Cada tipo de página scrapeable implementa `ScrapingStrategy<T>`:
- `NavigationScrapingStrategy` → menús/submenús.
- `NewsScrapingStrategy` → listado de noticias en home.
- `DetailPageScrapingStrategy` → vista de detalle de noticia.

`WebScrapingService.executeStrategy(url, strategy)` ejecuta cualquier estrategia sobre cualquier URL, manteniendo Jsoup encapsulado en `strategy/`.

### Patrón Factory
Se introducirá un `ScrapingStrategyFactory` que resuelva la estrategia adecuada a partir de un enum (`ScrapingType.NAVIGATION | NEWS | DETAIL`). Permite que el servicio o el controlador soliciten estrategias por tipo sin acoplarse al bean concreto, facilitando añadir nuevos tipos sin tocar código existente (Open/Closed).

### Reglas arquitectónicas
1. **Ningún selector Jsoup fuera de `strategy/`.**
2. Los controladores **solo** validan entrada y devuelven DTOs.
3. Los DTOs son **inmutables** (records Java 17 o Lombok `@Value`).
4. Cada nuevo endpoint = nueva `ScrapingStrategy`, nunca lógica suelta en el servicio.
5. La URL base vive en `application.properties` (`church.url`), nunca hardcodeada.

## Roadmap de Desarrollo

### Fase 1 — Endpoint de Navegación
`GET /api/navigation` → JSON con `[{ name, url, submenus: [...] }]` recorriendo el menú principal del sitio. Implementado vía `NavigationScrapingStrategy`. Pendiente: refactor para consumirse a través de `ScrapingStrategyFactory`.

### Fase 2 — Endpoint de Listado de Noticias (Home)
`GET /api/news` → JSON `[{ title, subtitle, image, link, excerpt }]`. El servicio busca primero un enlace del menú cuyo nombre contenga `actualidad|noticias|blog`; si no existe, scrapea la home como fallback.

### Fase 3 — Endpoint de Detalle
`GET /api/detail?url=...` → JSON `{ title, subtitle, image, content }` donde `content` preserva texto completo y enlaces internos.

### Próximos pasos sugeridos
- Introducir `ScrapingStrategyFactory` + enum `ScrapingType`.
- Migrar Springfox → springdoc-openapi.
- Añadir validación con `spring-boot-starter-validation`.
- Tests unitarios por estrategia con HTML fixtures.
- Cache TTL configurable.

## Reglas de IA (AI Prompts)

Cualquier asistente que lea este archivo debe seguir estas directrices al generar código o respuestas:

1. **Priorizar Clean Code** — nombres expresivos, métodos cortos, una responsabilidad por clase.
2. **Respetar la arquitectura** — nunca poner Jsoup en controladores/servicios; siempre crear una nueva `ScrapingStrategy`.
3. **Explicar el código línea por línea** cuando se introduzcan patrones nuevos o lógica no trivial.
4. **Acompañar siempre con un ejemplo de uso** (curl + JSON de respuesta esperada).
5. **Justificar decisiones de diseño** brevemente — por qué Strategy, por qué Factory, por qué un DTO en vez de Map.
6. **No añadir dependencias sin justificarlas** en la sección Tech Stack de este documento.
7. **Tests primero** cuando sea razonable: cada nueva estrategia llega con su test usando un HTML fixture.
8. **Inmutabilidad por defecto** en DTOs (records Java 17).
9. **Idioma:** comentarios y logs en español; nombres de clases/métodos en inglés.
10. **No inventar selectores HTML** — si se necesita scrapear algo nuevo, pedir el HTML real o la URL.
