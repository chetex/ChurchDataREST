package com.chetex.church.rest.controller;

import com.chetex.church.rest.dto.LoginRequestDTO;
import com.chetex.church.rest.dto.LoginResponseDTO;
import com.chetex.church.rest.service.LoginService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de autenticación para la app móvil.
 *
 * <p>Se expone como {@code POST /api/login/login-user} (aunque la idea
 * inicial era un GET, hacerlo POST evita registrar contraseñas en los
 * logs y en el historial del cliente HTTP).</p>
 */
@RestController
@RequestMapping("/api/login")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    /**
     * Verifica credenciales y devuelve OK/FAIL.
     *
     * @param body payload con user + password.
     * @return DTO con status y rol (si aplica).
     */
    @PostMapping("/login-user")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO body) {
        return loginService.authenticate(body.user(), body.password());
    }
}
