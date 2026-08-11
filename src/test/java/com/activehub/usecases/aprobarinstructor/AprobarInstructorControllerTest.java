package com.activehub.usecases.aprobarinstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
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

@WebMvcTest(controllers = AprobarInstructorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AprobarInstructorControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AprobarInstructorService aprobarInstructorService;

    @Test
    void aprobar_instructorExistente_devuelve200() {
        UUID instructorId = UUID.randomUUID();
        when(aprobarInstructorService.aprobar(any(), any()))
                .thenReturn(new AprobarInstructorResponse(instructorId, "APROBADO"));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.post().uri("/api/admin/instructores/{id}/aprobar", instructorId)
                .principal(authentication)
                .exchange())
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.estadoVerificacion").isEqualTo("APROBADO");
    }
}
