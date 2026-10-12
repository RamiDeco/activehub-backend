package com.activehub.usecases.renovartokensmercadopago;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RenovarTokensMercadoPagoScheduler {

    private static final long CADA_HORA = 60 * 60 * 1000;

    private final RenovarTokensMercadoPagoService renovarTokensMercadoPagoService;

    public RenovarTokensMercadoPagoScheduler(RenovarTokensMercadoPagoService renovarTokensMercadoPagoService) {
        this.renovarTokensMercadoPagoService = renovarTokensMercadoPagoService;
    }

    /** Los tokens duran meses y se renuevan con días de margen: una hora sobra. */
    @Scheduled(fixedRate = CADA_HORA)
    public void ejecutar() {
        renovarTokensMercadoPagoService.renovar();
    }
}
