package com.activehub.usecases.recomendaractividades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.shared.error.GlobalExceptionHandler;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = RecomendarActividadesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RecomendarActividadesControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RecomendarActividadesService recomendarActividadesService;

    private RecomendarActividadesResponse.Recomendada recomendada(String nombre, double puntaje) {
        return new RecomendarActividadesResponse.Recomendada(
                UUID.randomUUID(),
                nombre,
                new RecomendarActividadesResponse.TipoActividad(UUID.randomUUID(), "Yoga"),
                new RecomendarActividadesResponse.Categoria(UUID.randomUUID(), "Bienestar"),
                new RecomendarActividadesResponse.NivelIntensidad(UUID.randomUUID(), "Física baja"),
                new RecomendarActividadesResponse.Instructor(UUID.randomUUID(), "Sofía", "Instructora"),
                new BigDecimal("4000"),
                "Mendoza",
                "#000000",
                new BigDecimal("4.5"),
                60,
                null,
                null,
                null,
                puntaje,
                List.of("Coincide con tu interés en Yoga"));
    }

    @Test
    void recomendar_devuelveLaListaConSuPuntajeYSusMotivos() {
        when(recomendarActividadesService.recomendar(any(), isNull(), isNull(), isNull()))
                .thenReturn(new RecomendarActividadesResponse(false, List.of(recomendada("Yoga al amanecer", 6.5))));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/alumno/recomendaciones").principal(authentication))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.recomendadas[0].motivos[0]").isEqualTo("Coincide con tu interés en Yoga");
    }

    /** El alumno sale del token, nunca de un parametro: nadie pide las recomendaciones de otro. */
    @Test
    void recomendar_usaElAlumnoDelToken() {
        UUID alumnoId = UUID.randomUUID();
        when(recomendarActividadesService.recomendar(any(), any(), any(), any()))
                .thenReturn(new RecomendarActividadesResponse(false, List.of()));

        var authentication = new UsernamePasswordAuthenticationToken(alumnoId, null, List.of());
        assertThat(mvc.get().uri("/api/alumno/recomendaciones?lat=-32.89&lng=-68.84&limite=5")
                        .principal(authentication))
                .hasStatus(200);

        verify(recomendarActividadesService).recomendar(eq(alumnoId), eq(-32.89), eq(-68.84), eq(5));
    }

    @Test
    void recomendar_sinSenales_devuelveListaVaciaYElIndicador() {
        when(recomendarActividadesService.recomendar(any(), isNull(), isNull(), isNull()))
                .thenReturn(new RecomendarActividadesResponse(true, List.of()));

        var authentication = new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of());

        assertThat(mvc.get().uri("/api/alumno/recomendaciones").principal(authentication))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.sinSenales").isEqualTo(true);
    }
}
