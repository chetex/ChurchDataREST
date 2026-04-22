package com.chetex.church.rest.service;

import com.chetex.church.rest.dto.ReservaSalaDTO;
import com.chetex.church.rest.entity.ReservaSala;
import com.chetex.church.rest.entity.ReservaSala.EstadoReserva;
import com.chetex.church.rest.repository.ReservaSalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de reservas de sala. Centraliza validaciones, detección de
 * colisiones y transiciones de estado para que los controladores queden
 * finos y testables.
 */
@Service
public class ReservaSalaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaSalaService.class);

    private final ReservaSalaRepository repository;

    public ReservaSalaService(ReservaSalaRepository repository) {
        this.repository = repository;
    }

    /** Lista todas las reservas registradas. */
    public List<ReservaSalaDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Crea una nueva reserva en estado PENDING. Si ya existe una reserva
     * con la misma sala+fecha+hora, lanza 409 CONFLICT.
     */
    public ReservaSalaDTO create(ReservaSalaDTO dto) {
        validateCreate(dto);

        repository.findBySalaAndFechaAndHora(dto.sala(), dto.fecha(), dto.hora())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe una reserva para " + dto.sala() + " el " + dto.fecha() + " a las " + dto.hora());
                });

        ReservaSala entity = new ReservaSala(dto.sala(), dto.fecha(), dto.hora(), dto.grupo());
        ReservaSala saved = repository.save(entity);
        log.info("Reserva creada id={} sala={} fecha={} hora={} grupo={}",
                saved.getId(), saved.getSala(), saved.getFecha(), saved.getHora(), saved.getGrupo());
        return toDto(saved);
    }

    /**
     * Aplica una decisión (OK / DENY) a una reserva existente. La decisión
     * es case-insensitive; cualquier otro valor se rechaza con 400.
     */
    public ReservaSalaDTO applyDecision(Long id, String decisionRaw) {
        if (decisionRaw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decision no puede ser null");
        }
        EstadoReserva decision;
        try {
            decision = EstadoReserva.valueOf(decisionRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "decision debe ser OK o DENY, recibido: " + decisionRaw);
        }
        if (decision == EstadoReserva.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "decision no puede ser PENDING");
        }

        ReservaSala reserva = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Reserva no encontrada: id=" + id));

        reserva.setEstado(decision);
        ReservaSala updated = repository.save(reserva);
        log.info("Decisión aplicada id={} nuevoEstado={}", updated.getId(), updated.getEstado());
        return toDto(updated);
    }

    /** Validación básica del payload entrante. */
    private void validateCreate(ReservaSalaDTO dto) {
        if (dto == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body vacío");
        if (dto.sala() == null || dto.sala().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sala requerida");
        if (dto.fecha() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha requerida");
        if (dto.hora() == null || dto.hora().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hora requerida");
        if (dto.grupo() == null || dto.grupo().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grupo requerido");
    }

    private ReservaSalaDTO toDto(ReservaSala e) {
        return new ReservaSalaDTO(
                e.getId(), e.getSala(), e.getFecha(), e.getHora(), e.getGrupo(),
                e.getEstado() == null ? null : e.getEstado().name()
        );
    }
}
