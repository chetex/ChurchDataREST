package com.chetex.church.rest.repository;

import com.chetex.church.rest.entity.ReservaSala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repositorio de reservas de sala. Expone consultas concretas que el
 * servicio necesita (detección de colisión por slot).
 */
public interface ReservaSalaRepository extends JpaRepository<ReservaSala, Long> {

    /**
     * Busca una reserva existente por la combinación sala + fecha + hora,
     * que es la clave funcional de la reserva. Se usa para detectar
     * colisiones antes de crear una nueva reserva.
     */
    Optional<ReservaSala> findBySalaAndFechaAndHora(String sala, LocalDate fecha, String hora);
}
