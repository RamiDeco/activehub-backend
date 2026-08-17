package com.activehub.usecases.finalizarclasesvencidas;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinalizarClasesVencidasSchedulerTest {

    @Mock
    private FinalizarClasesVencidasService finalizarClasesVencidasService;

    @Test
    void ejecutar_invocaAlService() {
        FinalizarClasesVencidasScheduler scheduler = new FinalizarClasesVencidasScheduler(finalizarClasesVencidasService);

        scheduler.ejecutar();

        verify(finalizarClasesVencidasService).finalizar();
    }
}
