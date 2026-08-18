package com.activehub.domain.penalizacion;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, UUID> {
}
