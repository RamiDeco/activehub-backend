package com.activehub.usecases.rechazarinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * El motivo del rechazo es obligatorio (2026-10-26). Además, el Controller no tenía @Valid y
 * ninguna validación del request corría, tampoco @SinHtml.
 */
@WebMvcTest(controllers = RechazarInstructorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RechazarInstructorControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RechazarInstructorService rechazarInstructorService;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

    @Test
    void rechazar_conMotivo_devuelve200() {
        UUID id = UUID.randomUUID();
        when(rechazarInstructorService.rechazar(eq(id), any(), any()))
                .thenReturn(new RechazarInstructorResponse(id, "RECHAZADO", "Falta el certificado"));

        assertThat(mvc.perform(MockMvcRequestBuilders.post("/api/admin/instructores/" + id + "/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Falta el certificado\"}")
                        .principal(authentication)))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.motivoRechazo").isEqualTo("Falta el certificado");
    }

    @Test
    void rechazar_motivoEnBlanco_devuelve400SinLlamarAlServicio() {
        assertThat(mvc.perform(MockMvcRequestBuilders.post("/api/admin/instructores/" + UUID.randomUUID() + "/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  \"}")
                        .principal(authentication)))
                .hasStatus(400);
        verify(rechazarInstructorService, never()).rechazar(any(), any(), any());
    }

    @Test
    void rechazar_motivoConHtml_devuelve400() {
        assertThat(mvc.perform(MockMvcRequestBuilders.post("/api/admin/instructores/" + UUID.randomUUID() + "/rechazar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"<script>alert(1)</script>\"}")
                        .principal(authentication)))
                .hasStatus(400);
    }
}
