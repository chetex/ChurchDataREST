

Ideas Globales para el REST api
-------------------------------------

 - Un endpoint para extraer el menu de la página principal YA
 - Un endpoint para extraer los elementos de la pagina principal. Modificar porque el texto que trae, tiene un 
   titulo y un subtitulo. Revisar para venga en dos partes.
 - Un endpoint para saber si hay elementos nuevos (post) en la web (areNewElements). Una petición sencilla que devuelva 
       un booleano indicando si hay nuevos elementos o no. 
   > Siempre se realiza esta petición antes que ninguna, para saber si recachear o no.
 - Un endpoint en el que se le pase la pagina seleccionada (url) y te devuelva los elementos de esa pagina imagenes
   texto, texto con enlaces, textos en negrita, texto especiales, que guarde el formato de cada texto en html,
   la idea es que luego una aplicación movil va a leer ese texto del JSON y lo va a mostrar en la app.
 - Un endpoint para extraer las redes sociales en la cabecera y unirlo con el link de Telegram del footer. Nombre de red
   social y link

 - El endpoint REST tendra una base de datos de postgreSQL, en la que se almacenan las peticiones JSON respuesta,
     Si al hacer la petición inicial de areNewElements, devuelve que no hay elementos nuevos (false) entonces, lee
     la información de base de datos y devuelve el JSON almacenado en base de datos.
   > Se almacena en cache las peticiones JSON respuesta.
   > Si la petición ya se ha hecho, se devuelve la respuesta de la base de datos.

Prompt
--------

Actúa como un Desarrollador Senior de Java y experto en arquitecturas REST con Spring Boot. 
Tu objetivo es seguir implementando el codigo de ChurchDataREST API REST que actúe como un intermediario
(scraper y sistema de caché) entre una página web externa y una aplicación móvil.

Haz un estudio de la aplicación existente que ya hay codigo desarrollado.

Stack Tecnológico Requerido:

Framework: Spring Boot (Java)
Base de datos: PostgreSQL (actuando como almacén de caché de las respuestas JSON)
Persistencia: Ya se está usando...Spring Data JPA
Scraping: Ya se está usando... JSoup (o la librería de parseo HTML que consideres más eficiente)

Arquitectura y Lógica Central (Estrategia de Caché):
La API debe almacenar las respuestas JSON completas en PostgreSQL para evitar hacer scraping innecesario a la web de origen.
El flujo de trabajo se rige por un endpoint de validación que la app móvil llamará siempre antes de cualquier otra petición.
Si ese endpoint determina que NO hay elementos nuevos en la web, la API deberá omitir el scraping y devolver directamente 
los JSON cacheados en PostgreSQL. 

Si SÍ hay elementos nuevos, la API deberá realizar el scraping de la web, 
parsear los datos, actualizar la base de datos PostgreSQL con los nuevos JSON, 
y devolver esta información fresca al cliente.

Especificación de Endpoints a desarrollar:

GET /api/status/new-elements (Endpoint Crítico):
Propósito: Determinar si hay posts/elementos nuevos en la web.
Respuesta: Un booleano (true o false). Debe ser una petición muy ligera y rápida.

GET /api/home/menu:
Propósito: Extraer y devolver la estructura de navegación/menú de la página principal. Ya lo tienes implementado.

GET /api/home/elements:
Propósito: Extraer los elementos principales de la página de inicio. (ya está implementado)
Requisito técnico: Quiero que modifiques.... El texto principal extraído de la web suele venir en un solo bloque.
Debes implementar lógica para procesarlo y separarlo explícitamente en dos campos distintos 
dentro del JSON de respuesta: title (título principal) y subtitle (subtítulo).

GET /api/page/content (o GET con query param url):
Propósito: En la petición anterior de /api/home/elements tienes la URL de la pagina que llamas, 
Le vamos a pasar esta URL como query param, y devolver todo su contenido estructurado.
Requisito técnico: Debe identificar y extraer imágenes, texto regular, textos con enlaces (links), 
textos en negrita y textos con formatos especiales.
Formato de salida: Es obligatorio conservar las etiquetas HTML originales correspondientes a 
cada texto dentro del JSON. La aplicación móvil se encargará de leer y renderizar ese HTML nativamente.

GET /api/socials:
Propósito: Extraer y consolidar la información de las redes sociales de la web.
Requisito técnico: Debe hacer scraping de la cabecera (header) para obtener las redes sociales generales, 
buscar en el pie de página (footer) para encontrar un enlace específico de Telegram, y unir ambos resultados.
Respuesta: Un array de objetos JSON donde cada elemento contenga el nombre de la red social y su respectivo enlace.

Todas estas peticiónes iran almacenadas en la base de datos PostgreSQL como JSON completos, para que si la petición de validación de nuevos elementos devuelve false, 
se pueda devolver directamente el JSON almacenado sin necesidad de hacer scraping.

Entregables:
Por favor, comienza generando las entidades JPA para almacenar los JSON, y la implementación del primer endpoint de 
validación de caché (areNewElements). Recuerda documentar el código línea por línea en ingles con comentarios sencillos.

Mobile app
------------

 - Usar material design 3 de google, usando los componentes que ofrece https://m3.material.io/
 - Mandar paleta de colores de la web, y de la parroquia a la app para que se adapte a los colores de la web.
   > Preguntar el prompt y como hacer la paleta a la IA
 - Tendra una base de datos SQllite para guardar el JSON por si el REST se cae.