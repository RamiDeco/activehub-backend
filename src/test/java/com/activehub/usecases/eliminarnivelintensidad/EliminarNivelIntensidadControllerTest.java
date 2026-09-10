package com.activehub.usecases.eliminarnivelintensidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.activehub.shared.error.GlobalExceptionHandler;
import com.activehub.shared.error.NivelIntensidadEnUsoException;
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

/** E4Ad-HU05 criterios 6 y 7. */
@WebMvcTest(controllers = EliminarNivelIntensidadController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EliminarNivelIntensidadControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private EliminarNivelIntensidadService eliminarNivelIntensidadService;

    private static UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());
    }

    @Test
    void eliminar_sinActividades_devuelve204() {
        assertThat(mvc.delete().uri("/api/admin/niveles-intensidad/{id}", UUID.randomUUID())
                .principal(admin())
                .exchange())
                .hasStatus(204);
    }

    @Test
    void eliminar_conActividadesAsociadas_devuelve409ConElMensajeDeLaHU() {
        doThrow(new NivelIntensidadEnUsoException())
                .when(eliminarNivelIntensidadService).eliminar(any(), any());

        assertThat(mvc.delete().uri("/api/admin/niveles-intensidad/{id}", UUID.randomUUID())
                .principal(admin())
                .exchange())
                .hasStatus(409)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("No podés eliminar este Nivel porque tiene actividades asociadas.");
    }
}
