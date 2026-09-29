package com.activehub.usecases.registrarinteraccion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.interaccion.InteraccionAlumno;
import com.activehub.domain.interaccion.InteraccionAlumnoRepository;
import com.activehub.domain.interaccion.TipoInteraccion;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistrarInteraccionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-03-10T12:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private InteraccionAlumnoRepository interaccionRepository;

    private RegistrarInteraccionService service;
    private UUID alumnoId;
    private UUID actividadId;
    private Usuario alumno;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        service = new RegistrarInteraccionService(
                usuarioRepository, actividadRepository, interaccionRepository, Clock.fixed(AHORA, ZoneOffset.UTC));

        alumnoId = UUID.randomUUID();
        alumno = new Usuario();
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        actividadId = UUID.randomUUID();
        actividad = new Actividad();
        ReflectionTestUtils.setField(actividad, "id", actividadId);

        lenient().when(usuarioRepository.findById(alumnoId)).thenReturn(Optional.of(alumno));
        lenient().when(actividadRepository.findById(actividadId)).thenReturn(Optional.of(actividad));
        lenient().when(interaccionRepository.existeVistaReciente(any(), any(), any())).thenReturn(false);
        lenient().when(interaccionRepository.existeBusquedaReciente(any(), anyString(), any())).thenReturn(false);
    }

    @Test
    void registrar_vista_guardaLaInteraccion() {
        service.registrar(alumnoId, new RegistrarInteraccionRequest("VISTA_ACTIVIDAD", actividadId, null));

        ArgumentCaptor<InteraccionAlumno> captor = ArgumentCaptor.forClass(InteraccionAlumno.class);
        verify(interaccionRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoInteraccion.VISTA_ACTIVIDAD);
        assertThat(captor.getValue().getActividad()).isEqualTo(actividad);
        assertThat(captor.getValue().getTermino()).isNull();
    }

    @Test
    void registrar_busqueda_guardaElTerminoRecortado() {
        service.registrar(alumnoId, new RegistrarInteraccionRequest("BUSQUEDA", null, "  natación  "));

        ArgumentCaptor<InteraccionAlumno> captor = ArgumentCaptor.forClass(InteraccionAlumno.class);
        verify(interaccionRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoInteraccion.BUSQUEDA);
        assertThat(captor.getValue().getTermino()).isEqualTo("natación");
        assertThat(captor.getValue().getActividad()).isNull();
    }

    /**
     * El antirrebote: abrir diez veces la misma actividad no puede convertirla en el gusto
     * dominante del alumno.
     */
    @Test
    void registrar_mismaVistaDentroDeLaVentana_noLaGuardaDeNuevo() {
        when(interaccionRepository.existeVistaReciente(eq(alumnoId), eq(actividadId), any())).thenReturn(true);

        service.registrar(alumnoId, new RegistrarInteraccionRequest("VISTA_ACTIVIDAD", actividadId, null));

        verify(interaccionRepository, never()).save(any());
    }

    @Test
    void registrar_mismaBusquedaDentroDeLaVentana_noLaGuardaDeNuevo() {
        when(interaccionRepository.existeBusquedaReciente(eq(alumnoId), anyString(), any())).thenReturn(true);

        service.registrar(alumnoId, new RegistrarInteraccionRequest("BUSQUEDA", null, "natación"));

        verify(interaccionRepository, never()).save(any());
    }

    /** Dos letras es alguien tipeando, no una busqueda. Se descarta sin molestar a la pantalla. */
    @Test
    void registrar_busquedaDemasiadoCorta_seDescartaEnSilencio() {
        service.registrar(alumnoId, new RegistrarInteraccionRequest("BUSQUEDA", null, "yo"));

        verify(interaccionRepository, never()).save(any());
    }

    @Test
    void registrar_tipoDesconocido_esUn400() {
        assertThatThrownBy(() -> service.registrar(alumnoId, new RegistrarInteraccionRequest("ME_GUSTA", null, null)))
                .isInstanceOf(ValidacionException.class)
                .hasMessage("Tipo de interacción desconocido.");
    }

    @Test
    void registrar_vistaSinActividad_esUn400() {
        assertThatThrownBy(() -> service.registrar(alumnoId, new RegistrarInteraccionRequest("VISTA_ACTIVIDAD", null, null)))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void registrar_busquedaSinTermino_esUn400() {
        assertThatThrownBy(() -> service.registrar(alumnoId, new RegistrarInteraccionRequest("BUSQUEDA", null, "  ")))
                .isInstanceOf(ValidacionException.class);
    }

    /** Una actividad dada de baja no existe para el catalogo, asi que tampoco como señal. */
    @Test
    void registrar_vistaDeActividadInexistente_esUn404() {
        UUID otra = UUID.randomUUID();
        when(actividadRepository.findById(otra)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(alumnoId, new RegistrarInteraccionRequest("VISTA_ACTIVIDAD", otra, null)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void registrar_tipoEnMinusculas_tambienSirve() {
        service.registrar(alumnoId, new RegistrarInteraccionRequest("vista_actividad", actividadId, null));

        verify(interaccionRepository).save(any());
    }
}
