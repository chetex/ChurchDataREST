package com.chetex.church.rest.dto;

// Marker-free record type: ideal for an immutable response payload.

/**
 * Response shape of the {@code GET /api/status/new-elements} endpoint.
 *
 * @param newElements {@code true} if the source website has changed since
 *                    the last fingerprint check, {@code false} otherwise.
 * @param checkedAt   ISO-8601 timestamp (UTC) reporting when the check ran,
 *                    useful for client-side diagnostics and cache TTL logic.
 */
public record NewElementsStatusDTO(
        boolean newElements, // Boolean flag consumed by the mobile app before any other call.
        String checkedAt     // Timestamp returned as string to keep the JSON format predictable.
) {
}
