package com.chetex.church.rest.dto;

/**
 * Enlace a una red social extraído de la cabecera/pie de la web.
 *
 * @param name Nombre canónico de la red (p.ej. "facebook", "instagram", "twitter", "telegram", "youtube").
 * @param url  URL absoluta del perfil o canal.
 */
public record SocialLinkDTO(
        String name,
        String url
) {
}
