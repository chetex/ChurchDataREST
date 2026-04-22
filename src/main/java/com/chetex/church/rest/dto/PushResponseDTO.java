package com.chetex.church.rest.dto;

/**
 * Respuesta del envío de push notifications.
 *
 * @param status   "OK" si FCM aceptó el mensaje, "FAIL" si hubo error.
 * @param topic    Topic efectivo al que se envió (por defecto "all").
 * @param messageId ID devuelto por FCM (null en caso de error o stub).
 * @param error    Mensaje de error si status="FAIL" (null en caso OK).
 */
public record PushResponseDTO(String status, String topic, String messageId, String error) {
}
