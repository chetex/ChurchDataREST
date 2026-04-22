package com.chetex.church.rest.controller;

import com.chetex.church.rest.dto.ReservaDecisionDTO;
import com.chetex.church.rest.dto.ReservaSalaDTO;
import com.chetex.church.rest.service.AdminAuthService;
import com.chetex.church.rest.service.ReservaSalaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de reservas de salas para grupos parroquiales.
 *
 * <ul>
 *   <li>{@code GET  /api/groups/get-books} — lista todas las reservas.</li>
 *   <li>{@code POST /api/groups/book} — crea una reserva en estado PENDING.</li>
 *   <li>{@code POST /api/groups/book/decision} — admin: aprueba / deniega
 *       una reserva (requiere cabecera {@code X-Admin-Secret}).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/groups")
public class GroupsController {

    private final ReservaSalaService reservaService;
    private final AdminAuthService adminAuthService;

    public GroupsController(ReservaSalaService reservaService, AdminAuthService adminAuthService) {
        this.reservaService = reservaService;
        this.adminAuthService = adminAuthService;
    }

    /** Lista todas las reservas almacenadas. */
    @GetMapping("/get-books")
    public List<ReservaSalaDTO> listBooks() {
        return reservaService.findAll();
    }

    /**
     * Crea una nueva reserva. Devuelve 201 con la entidad creada o 409 si
     * ya existe otra reserva en el mismo slot (sala+fecha+hora).
     */
    @PostMapping("/book")
    public ResponseEntity<ReservaSalaDTO> book(@RequestBody ReservaSalaDTO body) {
        ReservaSalaDTO created = reservaService.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Aplica una decisión (OK / DENY) sobre una reserva existente. Requiere
     * cabecera {@code X-Admin-Secret}.
     */
    @PostMapping("/book/decision")
    public ReservaSalaDTO decide(@RequestBody ReservaDecisionDTO body,
                                 @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecret) {
        adminAuthService.requireAdmin(adminSecret);
        return reservaService.applyDecision(body.id(), body.decision());
    }
}
