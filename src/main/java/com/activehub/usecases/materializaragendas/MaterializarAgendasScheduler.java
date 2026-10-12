package com.activehub.usecases.materializaragendas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaterializarAgendasScheduler {

    /** Una vez por hora alcanza: la ventana de anticipación es de 7 días. */
    private static final long CADA_HORA = 60 * 60 * 1000;

    private final MaterializarAgendasService materializarAgendasService;

    public MaterializarAgendasScheduler(MaterializarAgendasService materializarAgendasService) {
        this.materializarAgendasService = materializarAgendasService;
    }

    @Scheduled(fixedRate = CADA_HORA)
    public void ejecutar() {
        materializarAgendasService.materializar();
    }
}
