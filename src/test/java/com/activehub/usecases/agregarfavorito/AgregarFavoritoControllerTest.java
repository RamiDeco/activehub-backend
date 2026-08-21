package com.activehub.usecases.agregarfavorito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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

@WebMvcTest(controllers = AgregarFavoritoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AgregarFavoritoControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AgregarFavoritoService agregarFavoritoService;

    private static final UUID ACTIVIDAD_ID = UUID.randomUUID();

    @Test
    void agregar_devuelve204() {
        UUID alumnoId = UUID.randomUUID();
        var authentication = new UsernamePasswordAuthenticationToken(alumnoId, null, List.of());

        assertThat(mvc.post().uri("/api/alumno/actividades/{id}/favorito", ACTIVIDAD_ID)
                .principal(authentication)
                .exchange())
                .hasStatus(204);

        verify(agregarFavoritoService).agregar(ACTIVIDAD_ID, alumnoId);
    }
}
