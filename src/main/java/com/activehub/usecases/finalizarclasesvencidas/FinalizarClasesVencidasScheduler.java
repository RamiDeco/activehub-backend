package com.activehub.usecases.finalizarclasesvencidas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FinalizarClasesVencidasScheduler {

    private static final long CADA_5_MINUTOS = 5 * 60 * 1000;

    private final FinalizarClasesVencidasService finalizarClasesVencidasService;

    public FinalizarClasesVencidasScheduler(FinalizarClasesVencidasService finalizarClasesVencidasService) {
        this.finalizarClasesVencidasService = finalizarClasesVencidasService;
    }

    @Scheduled(fixedRate = CADA_5_MINUTOS)
    public void ejecutar() {
        finalizarClasesVencidasService.finalizar();
        // Despues de finalizar, y como llamada aparte: finalizar() commitea antes de que se
        // capture nada, y las capturas no comparten su transaccion (ver el javadoc).
        finalizarClasesVencidasService.capturarPagosPendientes();
    }
}
