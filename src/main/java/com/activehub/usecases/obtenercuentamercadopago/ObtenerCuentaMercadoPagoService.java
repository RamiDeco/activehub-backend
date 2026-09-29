package com.activehub.usecases.obtenercuentamercadopago;

import com.activehub.domain.mercadopago.CuentaMercadoPagoInstructorRepository;
import com.activehub.domain.mercadopago.EstadoCuentaMercadoPago;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObtenerCuentaMercadoPagoService {

    private final CuentaMercadoPagoInstructorRepository cuentaRepository;
    private final Clock clock;

    public ObtenerCuentaMercadoPagoService(CuentaMercadoPagoInstructorRepository cuentaRepository, Clock clock) {
        this.cuentaRepository = cuentaRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ObtenerCuentaMercadoPagoResponse obtener(UUID instructorId) {
        return cuentaRepository.findResumenByInstructorId(instructorId)
                .map(c -> {
                    // Misma regla que CuentaMercadoPagoGuard para decidir si puede cobrar.
                    boolean vinculada = c.vigente(clock.instant());
                    String estado = c.getEstado() == EstadoCuentaMercadoPago.VINCULADA && !vinculada
                            ? EstadoCuentaMercadoPago.TOKEN_VENCIDO.name()
                            : c.getEstado().name();
                    return new ObtenerCuentaMercadoPagoResponse(
                            vinculada, estado, c.getMpUserId(), c.getActualizadaEl(), c.getExpiraEl());
                })
                .orElse(new ObtenerCuentaMercadoPagoResponse(false, null, null, null, null));
    }
}
