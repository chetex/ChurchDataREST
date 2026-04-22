package com.chetex.church.rest.dto;

import java.util.List;

/**
 * Respuesta compuesta del endpoint {@code /api/construye}.
 *
 * <p>Agrupa los bloques navegables de la página "Juntos Crecemos Mejor"
 * (Suscripción, Transferencia, Bizum…) más los enlaces destacados que el
 * frontend móvil necesita accesibles de forma directa: formulario online
 * de suscripción, PDF de suscripción y tabla de beneficios fiscales.</p>
 *
 * @param title                  Título principal de la página (h1).
 * @param sections               Lista de bloques con título/subtítulo/texto/imagen.
 * @param linkSuscripcionOnline  URL del formulario de suscripción online (puede ser null).
 * @param linkSuscripcionPDF     URL del PDF de suscripción (puede ser null).
 * @param linkBeneficios         URL de la página de beneficios fiscales (puede ser null).
 */
public record ConstruyePageDTO(
        String title,
        List<PageSectionDTO> sections,
        String linkSuscripcionOnline,
        String linkSuscripcionPDF,
        String linkBeneficios
) {
}
