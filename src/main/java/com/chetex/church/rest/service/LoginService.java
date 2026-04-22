package com.chetex.church.rest.service;

import com.chetex.church.rest.dto.LoginResponseDTO;
import com.chetex.church.rest.entity.AppUser;
import com.chetex.church.rest.repository.AppUserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Servicio de autenticación de usuarios. Valida credenciales contra la
 * tabla {@code app_user} usando BCrypt para comparar el hash almacenado
 * con la contraseña enviada.
 *
 * <p>Al arrancar la app siembra un usuario admin inicial si la tabla está
 * vacía, con credenciales tomadas de {@code admin.bootstrap.*} (con
 * valores por defecto seguros solo para entornos de desarrollo).</p>
 */
@Service
public class LoginService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private final AppUserRepository repository;

    // Credenciales iniciales del admin (cambiar en producción).
    private final String bootstrapAdminUsername;
    private final String bootstrapAdminPassword;

    public LoginService(AppUserRepository repository,
                        @Value("${admin.bootstrap.username:admin}") String bootstrapAdminUsername,
                        @Value("${admin.bootstrap.password:admin123}") String bootstrapAdminPassword) {
        this.repository = repository;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }

    /**
     * Comprueba usuario + contraseña.
     *
     * @return {@link LoginResponseDTO} con {@code status="OK"} y rol si cuadran;
     *         {@code status="FAIL"} y rol null si no.
     */
    public LoginResponseDTO authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return new LoginResponseDTO("FAIL", null);
        }
        Optional<AppUser> maybeUser = repository.findByUsername(username.trim());
        if (maybeUser.isEmpty()) {
            log.info("Login FAIL (usuario no existe): {}", username);
            return new LoginResponseDTO("FAIL", null);
        }
        AppUser user = maybeUser.get();
        boolean matches;
        try {
            matches = BCrypt.checkpw(rawPassword, user.getPasswordHash());
        } catch (IllegalArgumentException e) {
            // Hash malformado en BD — tratarlo como fallo de autenticación.
            log.warn("Hash BCrypt inválido para user={}: {}", username, e.getMessage());
            matches = false;
        }
        if (!matches) {
            log.info("Login FAIL (password incorrecta): {}", username);
            return new LoginResponseDTO("FAIL", null);
        }
        log.info("Login OK: user={} role={}", user.getUsername(), user.getRole());
        return new LoginResponseDTO("OK", user.getRole());
    }

    /**
     * Siembra un usuario admin si la tabla está vacía. Útil en primer
     * arranque / entornos de dev.
     */
    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;
        String hash = BCrypt.hashpw(bootstrapAdminPassword, BCrypt.gensalt(10));
        AppUser admin = new AppUser(bootstrapAdminUsername, hash, "ADMIN");
        repository.save(admin);
        log.info("Usuario admin inicial sembrado: username={} (contraseña tomada de admin.bootstrap.password)",
                bootstrapAdminUsername);
    }
}
