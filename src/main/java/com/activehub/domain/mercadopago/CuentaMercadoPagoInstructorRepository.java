package com.activehub.domain.mercadopago;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaMercadoPagoInstructorRepository
        extends JpaRepository<CuentaMercadoPagoInstructor, UUID> {

    Optional<CuentaMercadoPagoInstructor> findByInstructorId(UUID instructorId);
}
