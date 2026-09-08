package com.activehub.usecases.habilitarclasesproximas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HabilitarClasesProximasScheduler {

    private static final long CADA_5_MINUTOS = 5 * 60 * 1000;

    private final HabilitarClasesProximasService habilitarClasesProximasService;

    public HabilitarClasesProximasScheduler(HabilitarClasesProximasService habilitarClasesProximasService) {
        this.habilitarClasesProximasService = habilitarClasesProximasService;
    }

    @Scheduled(fixedRate = CADA_5_MINUTOS)
    public void ejecutar() {
        habilitarClasesProximasService.habilitar();
    }
}
