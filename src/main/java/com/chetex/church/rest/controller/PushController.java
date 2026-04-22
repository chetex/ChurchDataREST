package com.chetex.church.rest.controller;

import com.chetex.church.rest.dto.PushRequestDTO;
import com.chetex.church.rest.dto.PushResponseDTO;
import com.chetex.church.rest.service.AdminAuthService;
import com.chetex.church.rest.service.PushNotificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de envío de push notifications (Firebase Cloud Messaging).
 *
 * <p>Requiere cabecera {@code X-Admin-Secret} que coincida con
 * {@code admin.secret}. La respuesta incluye {@code status}, {@code topic}
 * efectivo y el {@code messageId} de FCM (o el mensaje de error si falla).</p>
 */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushNotificationService pushService;
    private final AdminAuthService adminAuthService;

    public PushController(PushNotificationService pushService, AdminAuthService adminAuthService) {
        this.pushService = pushService;
        this.adminAuthService = adminAuthService;
    }

    /**
     * Envía una push notification a todos los usuarios (topic "all" por defecto).
     */
    @PostMapping("/admin")
    public PushResponseDTO sendAdmin(@RequestBody PushRequestDTO body,
                                     @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminAuthService.requireAdmin(adminSecret);
        return pushService.send(body);
    }
}
