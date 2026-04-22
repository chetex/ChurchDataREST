package com.chetex.church.rest.dto;

/**
 * Body del endpoint {@code POST /api/push/admin}. Representa una push
 * notification a enviar a todos los clientes suscritos al topic
 * {@code all}.
 *
 * @param title Título de la notificación (campo "title" de FCM).
 * @param body  Cuerpo de la notificación (campo "body" de FCM).
 * @param topic Topic FCM destino; si es null se usa {@code all}.
 */
public record PushRequestDTO(String title, String body, String topic) {
}
