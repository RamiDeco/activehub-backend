package com.activehub.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

/**
 * V36: con la multa sin pagar, el instructor no modifica nada de su oferta. Solo puede pagar la
 * multa, ver sus inscriptos y confirmar cobros en efectivo.
 */
@ExtendWith(MockitoExtension.class)
class MultaPendienteFilterTest {

    @Mock private PenalizacionVigenteGuard penalizacionVigenteGuard;
    @Mock private FilterChain chain;

    private final UUID instructorId = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse ejecutar(String metodo, String uri) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(instructorId, null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MultaPendienteFilter(penalizacionVigenteGuard, JsonMapper.builder().build())
                .doFilter(request, response, chain);
        return response;
    }

    @Test
    void conMulta_crearUnaActividad_responde403ConElMotivo() throws Exception {
        when(penalizacionVigenteGuard.tieneMultaPendiente(instructorId)).thenReturn(true);

        MockHttpServletResponse response = ejecutar("POST", "/api/instructor/actividades");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("SIN_PERMISO").contains("multa pendiente de pago");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void conMulta_cancelarUnaClaseOResponderUnaResena_tambienSeFrena() throws Exception {
        when(penalizacionVigenteGuard.tieneMultaPendiente(instructorId)).thenReturn(true);

        assertThat(ejecutar("POST", "/api/instructor/clases/" + UUID.randomUUID() + "/cancelar").getStatus()).isEqualTo(403);
        assertThat(ejecutar("POST", "/api/instructor/resenas/" + UUID.randomUUID() + "/respuesta").getStatus()).isEqualTo(403);
        assertThat(ejecutar("DELETE", "/api/instructor/mercadopago").getStatus()).isEqualTo(403);
    }

    @Test
    void conMulta_confirmarUnCobroEnEfectivo_pasa() throws Exception {
        MockHttpServletResponse response =
                ejecutar("POST", "/api/instructor/inscripciones/" + UUID.randomUUID() + "/confirmar-cobro");

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
        verify(penalizacionVigenteGuard, never()).tieneMultaPendiente(any());
    }

    @Test
    void conMulta_enviarElComprobante_pasa() throws Exception {
        ejecutar("POST", "/api/instructor/sanciones/" + UUID.randomUUID() + "/comprobante");

        verify(chain).doFilter(any(), any());
    }

    @Test
    void lasLecturas_pasanAunqueTengaMulta() throws Exception {
        ejecutar("GET", "/api/instructor/clases");

        verify(chain).doFilter(any(), any());
        verify(penalizacionVigenteGuard, never()).tieneMultaPendiente(any());
    }

    @Test
    void sinMulta_pasa() throws Exception {
        when(penalizacionVigenteGuard.tieneMultaPendiente(instructorId)).thenReturn(false);

        ejecutar("PUT", "/api/instructor/actividades/" + UUID.randomUUID());

        verify(chain).doFilter(any(), any());
    }

    @Test
    void fueraDelAreaDelInstructor_noSeMira() throws Exception {
        ejecutar("PUT", "/api/usuarios/me");

        verify(chain).doFilter(any(), any());
        verify(penalizacionVigenteGuard, never()).tieneMultaPendiente(any());
    }
}
