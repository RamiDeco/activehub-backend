package com.activehub.usecases.descargardocumentoinstructor;

import static org.assertj.core.api.Assertions.assertThat;
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

@WebMvcTest(controllers = DescargarDocumentoInstructorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DescargarDocumentoInstructorControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private DescargarDocumentoInstructorService descargarDocumentoInstructorService;

    private static final UUID INSTRUCTOR_ID = UUID.randomUUID();
    private static final UUID DOCUMENTO_ID = UUID.randomUUID();

    @Test
    void descargar_documentoExistente_devuelve200ConBytes() {
        when(descargarDocumentoInstructorService.descargar(INSTRUCTOR_ID, DOCUMENTO_ID))
                .thenReturn(new DocumentoDescarga("cert.pdf", "application/pdf", "contenido".getBytes()));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/admin/instructores/{instructorId}/documentos/{documentoId}/archivo",
                        INSTRUCTOR_ID, DOCUMENTO_ID)
                .principal(authentication)
                .exchange())
                .hasStatus(200);
    }
}
