package com.activehub.usecases.listarinstructores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.usuario.EstadoVerificacion;
import com.activehub.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = ListarInstructoresController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ListarInstructoresControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ListarInstructoresService listarInstructoresService;

    @Test
    void listar_sinFiltro_devuelve200() {
        when(listarInstructoresService.listar(isNull())).thenReturn(List.of(
                new ListarInstructoresResponse(
                        UUID.randomUUID(), "Ana", "Perez", "ana@mail.com", "111", null,
                        Instant.now(), "Yoga", 3, "Aprobado", null)));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/admin/instructores")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$[0].nombre").isEqualTo("Ana");
    }

    @Test
    void listar_filtraPorQueryParamEstado() {
        when(listarInstructoresService.listar(EstadoVerificacion.PENDIENTE)).thenReturn(List.of());

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/admin/instructores?estado=PENDIENTE")
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyText()
                .isEqualTo("[]");

        verify(listarInstructoresService).listar(eq(EstadoVerificacion.PENDIENTE));
    }
}
