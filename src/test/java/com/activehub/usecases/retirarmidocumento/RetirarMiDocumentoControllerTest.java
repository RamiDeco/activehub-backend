package com.activehub.usecases.retirarmidocumento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.ValidacionException;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = RetirarMiDocumentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RetirarMiDocumentoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RetirarMiDocumentoService retirarMiDocumentoService;

    private final UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

    @Test
    void retirar_devuelve204() {
        assertThat(mvc.perform(MockMvcRequestBuilders
                        .delete("/api/instructor/documentos/" + UUID.randomUUID())
                        .principal(authentication)))
                .hasStatus(204);
    }

    @Test
    void retirar_elUltimo_devuelve400() {
        doThrow(new ValidacionException("Tenés que tener al menos un documento cargado."))
                .when(retirarMiDocumentoService).retirar(any(), any());

        assertThat(mvc.perform(MockMvcRequestBuilders
                        .delete("/api/instructor/documentos/" + UUID.randomUUID())
                        .principal(authentication)))
                .hasStatus(400);
    }
}
