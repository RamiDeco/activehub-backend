package com.activehub.usecases.levantarsuspensionesvencidas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LevantarSuspensionesVencidasScheduler {

    private static final long CADA_HORA = 60 * 60 * 1000;

    private final LevantarSuspensionesVencidasService levantarSuspensionesVencidasService;

    public LevantarSuspensionesVencidasScheduler(
            LevantarSuspensionesVencidasService levantarSuspensionesVencidasService) {
        this.levantarSuspensionesVencidasService = levantarSuspensionesVencidasService;
    }

    /** La vigencia se mide en días: no hace falta más resolución que una hora. */
    @Scheduled(fixedRate = CADA_HORA)
    public void ejecutar() {
        levantarSuspensionesVencidasService.levantar();
    }
}
