package com.chetex.church.rest.service;

import com.chetex.church.rest.dto.PushRequestDTO;
import com.chetex.church.rest.dto.PushResponseDTO;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Envía push notifications vía Firebase Cloud Messaging (topic-based).
 *
 * <p>La inicialización es perezosa: solo se crea la {@link FirebaseApp} si
 * la ruta al archivo de credenciales ({@code firebase.credentials-path})
 * apunta a un fichero existente. De este modo el resto de endpoints
 * funcionan aunque todavía no se haya provisionado la cuenta de servicio
 * de Firebase. En ese caso, {@link #send(PushRequestDTO)} devuelve
 * {@code status=FAIL} explicando que FCM no está configurado.</p>
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    // Topic por defecto cuando el cliente no especifica ninguno.
    private static final String DEFAULT_TOPIC = "all";

    // Ruta al fichero de credenciales (service account JSON) de Firebase.
    private final String credentialsPath;

    // Flag de inicialización: sólo se envía si Firebase está correctamente inicializado.
    private boolean firebaseReady = false;

    public PushNotificationService(@Value("${firebase.credentials-path:}") String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    /**
     * Inicialización diferida al arranque: si hay fichero, crea la
     * {@link FirebaseApp}. Si no, marca el servicio como no-ready.
     */
    @PostConstruct
    public void init() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FCM no configurado: firebase.credentials-path está vacío. Push notifications deshabilitadas.");
            return;
        }
        Path p = Path.of(credentialsPath);
        if (!Files.exists(p)) {
            log.warn("FCM: fichero de credenciales no encontrado en {}. Push notifications deshabilitadas.", p);
            return;
        }
        try (FileInputStream fis = new FileInputStream(p.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(fis))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            firebaseReady = true;
            log.info("FirebaseApp inicializada desde {}", p);
        } catch (Exception e) {
            log.error("Error inicializando FirebaseApp: {}", e.getMessage(), e);
        }
    }

    /**
     * Publica un mensaje al topic indicado (o {@code all} por defecto).
     */
    public PushResponseDTO send(PushRequestDTO request) {
        if (request == null || request.title() == null || request.body() == null) {
            return new PushResponseDTO("FAIL", null, null, "title y body son obligatorios");
        }
        String topic = (request.topic() == null || request.topic().isBlank())
                ? DEFAULT_TOPIC
                : request.topic();

        if (!firebaseReady) {
            return new PushResponseDTO("FAIL", topic, null,
                    "Firebase no está inicializado (configura firebase.credentials-path)");
        }

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(request.title())
                        .setBody(request.body())
                        .build())
                .build();
        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("Push enviado a topic='{}' messageId={}", topic, messageId);
            return new PushResponseDTO("OK", topic, messageId, null);
        } catch (Exception e) {
            log.error("Error enviando push a topic='{}': {}", topic, e.getMessage(), e);
            return new PushResponseDTO("FAIL", topic, null, e.getMessage());
        }
    }
}
