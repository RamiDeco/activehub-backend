package com.activehub.usecases.liberarpagos;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LiberarPagosScheduler {

    private static final long CADA_15_MINUTOS = 15 * 60 * 1000;

    private final LiberarPagosService liberarPagosService;

    public LiberarPagosScheduler(LiberarPagosService liberarPagosService) {
        this.liberarPagosService = liberarPagosService;
    }

    @Scheduled(fixedRate = CADA_15_MINUTOS)
    public void ejecutar() {
        liberarPagosService.liberar();
    }
}
