package com.chetex.church.rest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Reserva de una sala parroquial para un grupo. Persistida en la tabla
 * {@code reserva_sala}. La combinación {@code sala + fecha + hora} tiene
 * un índice único para impedir dobles reservas (se valida también a nivel
 * de servicio para devolver 409 con un mensaje claro).
 */
@Entity
@Table(name = "reserva_sala", indexes = {
        @Index(name = "idx_reserva_slot", columnList = "sala,fecha,hora", unique = true)
})
public class ReservaSala {

    /** Identificador autogenerado (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de la sala reservada (p. ej. "Sala de catequesis"). */
    @Column(nullable = false, length = 120)
    private String sala;

    /** Fecha de la reserva (YYYY-MM-DD). */
    @Column(nullable = false)
    private LocalDate fecha;

    /** Hora o franja horaria solicitada (texto libre del tipo "19:00-20:30"). */
    @Column(nullable = false, length = 32)
    private String hora;

    /** Nombre del grupo que solicita la reserva. */
    @Column(nullable = false, length = 120)
    private String grupo;

    /** Estado de la reserva: pendiente de aprobar, aprobada o denegada. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoReserva estado = EstadoReserva.PENDING;

    /** Timestamp de creación del registro. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // JPA requiere constructor sin args.
    public ReservaSala() {}

    public ReservaSala(String sala, LocalDate fecha, String hora, String grupo) {
        this.sala = sala;
        this.fecha = fecha;
        this.hora = hora;
        this.grupo = grupo;
        this.estado = EstadoReserva.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSala() { return sala; }
    public LocalDate getFecha() { return fecha; }
    public String getHora() { return hora; }
    public String getGrupo() { return grupo; }
    public EstadoReserva getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }

    public void setEstado(EstadoReserva estado) { this.estado = estado; }

    /** Estados posibles de la reserva. */
    public enum EstadoReserva {
        PENDING, // Esperando decisión del admin.
        OK,      // Reserva aprobada.
        DENY     // Reserva denegada.
    }
}
