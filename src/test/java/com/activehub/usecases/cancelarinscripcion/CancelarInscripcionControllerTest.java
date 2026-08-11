package com.activehub.usecases.cancelarinscripcion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.SinPermisoException;
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

@WebMvcTest(controllers = CancelarInscripcionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CancelarInscripcionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CancelarInscripcionService cancelarInscripcionService;

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void cancelar_propia_devuelve204() {
        assertThat(mvc.delete().uri("/api/alumno/inscripciones/{id}", UUID.randomUUID())
                .principal(principal())
                .exchange())
                .hasStatus(204);
    }

    @Test
    void cancelar_ajena_devuelve403() {
        doThrow(new SinPermisoException("No podés cancelar una inscripción que no te pertenece."))
                .when(cancelarInscripcionService).cancelar(any(), any());

        assertThat(mvc.delete().uri("/api/alumno/inscripciones/{id}", UUID.randomUUID())
                .principal(principal())
                .exchange())
                .hasStatus(403)
                .bodyJson()
                .extractingPath("$.code").isEqualTo("SIN_PERMISO");
    }
}
