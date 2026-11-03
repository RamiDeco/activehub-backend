package com.activehub.domain.penalizacion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ComprobantePenalizacionRepository extends JpaRepository<ComprobantePenalizacion, UUID> {

    /** Los comprobantes de un conjunto de multas, del más reciente al más viejo. */
    @Query("SELECT c FROM ComprobantePenalizacion c LEFT JOIN FETCH c.revisadoPor "
            + "WHERE c.penalizacion.id IN :penalizacionIds ORDER BY c.createdAt DESC")
    List<ComprobantePenalizacion> findDePenalizaciones(Collection<UUID> penalizacionIds);

    /** El comprobante que espera revisión, si lo hay: siempre es uno solo por multa. */
    Optional<ComprobantePenalizacion> findFirstByPenalizacionIdAndEstadoOrderByCreatedAtDesc(
            UUID penalizacionId, EstadoComprobantePenalizacion estado);
}
