package com.activehub.shared.notificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.activehub.domain.inscripcion.EstadoPago;
import org.junit.jupiter.api.Test;

class NotificacionMensajesTest {

    @Test
    void detalleReintegro_sinCapturar_diceQueNoSeLeVaACobrar() {
        assertThat(NotificacionMensajes.detalleReintegro(EstadoPago.Cancelado))
                .isEqualTo(" Se liberó la reserva del pago en tu tarjeta: no se te va a cobrar.");
    }

    @Test
    void detalleReintegro_capturado_diceQueSeLeReembolsa() {
        assertThat(NotificacionMensajes.detalleReintegro(EstadoPago.Reembolsado))
                .isEqualTo(" Te reembolsamos el pago por Mercado Pago; según tu banco, puede tardar unos días en verse en tu tarjeta.");
    }
}
