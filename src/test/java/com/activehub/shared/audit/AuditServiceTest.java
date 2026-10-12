package com.activehub.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditService service;

    @AfterEach
    void limpiarContexto() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void conRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private AuditLog registrarYCapturar() {
        service.registrar(UUID.randomUUID(), AuditAccion.LOGIN_OK, "Usuario", UUID.randomUUID(), null);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void registrar_guardaLaIpDelRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("190.55.1.20");
        conRequest(request);

        assertThat(registrarYCapturar().getIp()).isEqualTo("190.55.1.20");
    }

    @Test
    void registrar_detrasDeUnProxy_usaLaPrimeraDeXForwardedFor() {
        // getRemoteAddr() sería la IP del proxy, no la del usuario.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "190.55.1.20, 10.0.0.1");
        conRequest(request);

        assertThat(registrarYCapturar().getIp()).isEqualTo("190.55.1.20");
    }

    @Test
    void registrar_sinRequest_dejaLaIpEnNull() {
        // Los schedulers auditan fuera de un hilo HTTP: no hay IP que guardar.
        assertThat(registrarYCapturar().getIp()).isNull();
    }

    @Test
    void registrar_ipDemasiadoLarga_seRecortaALaColumna() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "9".repeat(80));
        conRequest(request);

        assertThat(registrarYCapturar().getIp()).hasSize(45);
    }
}
