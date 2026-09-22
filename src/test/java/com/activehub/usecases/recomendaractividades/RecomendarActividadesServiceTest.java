package com.activehub.usecases.recomendaractividades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.activehub.domain.actividad.Actividad;
import com.activehub.domain.actividad.ActividadRepository;
import com.activehub.domain.actividad.Categoria;
import com.activehub.domain.actividad.Clase;
import com.activehub.domain.actividad.ClaseRepository;
import com.activehub.domain.actividad.EstadoClase;
import com.activehub.domain.actividad.NivelIntensidad;
import com.activehub.domain.actividad.TipoActividad;
import com.activehub.domain.favorito.ActividadFavorita;
import com.activehub.domain.favorito.FavoritoRepository;
import com.activehub.domain.inscripcion.EstadoInscripcion;
import com.activehub.domain.inscripcion.Inscripcion;
import com.activehub.domain.inscripcion.InscripcionRepository;
import com.activehub.domain.inscripcion.Pago;
import com.activehub.domain.interaccion.InteraccionAlumno;
import com.activehub.domain.interaccion.InteraccionAlumnoRepository;
import com.activehub.domain.resenia.Resenia;
import com.activehub.domain.resenia.ReseniaRepository;
import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.domain.usuario.Usuario;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * El motor de recomendaciones. Los casos estan escritos alrededor de los tres defectos que
 * este usecase vino a corregir: que no puntuaba ni ordenaba, que solo miraba los intereses
 * declarados, y que dos alumnos con los mismos intereses veian lo mismo.
 */
@ExtendWith(MockitoExtension.class)
class RecomendarActividadesServiceTest {

    private static final Instant AHORA = Instant.parse("2026-03-10T12:00:00Z");

    @Mock
    private ActividadRepository actividadRepository;
    @Mock
    private ClaseRepository claseRepository;
    @Mock
    private PerfilAlumnoRepository perfilAlumnoRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private FavoritoRepository favoritoRepository;
    @Mock
    private ReseniaRepository reseniaRepository;
    @Mock
    private InteraccionAlumnoRepository interaccionRepository;

    private RecomendarActividadesService service;

    private UUID alumnoId;
    private Usuario alumno;
    private Usuario instructorA;
    private Usuario instructorB;
    private Categoria bienestar;
    private Categoria deportivas;
    private TipoActividad yoga;
    private TipoActividad running;
    private TipoActividad natacion;
    private NivelIntensidad baja;
    private NivelIntensidad alta;

    private Actividad actYoga;
    private Actividad actRunning;
    private Actividad actNatacion;

    @BeforeEach
    void setUp() {
        service = new RecomendarActividadesService(
                actividadRepository, claseRepository, perfilAlumnoRepository, inscripcionRepository,
                favoritoRepository, reseniaRepository, interaccionRepository,
                Clock.fixed(AHORA, ZoneOffset.UTC));

        alumnoId = UUID.randomUUID();
        alumno = usuario("Martina", "Alumna");
        ReflectionTestUtils.setField(alumno, "id", alumnoId);

        instructorA = usuario("Sofía", "Instructora");
        instructorB = usuario("Matías", "Instructor");

        bienestar = categoria("Bienestar");
        deportivas = categoria("Deportivas");
        yoga = tipo("Yoga", bienestar);
        running = tipo("Running", deportivas);
        natacion = tipo("Natación", deportivas);
        baja = nivel("Física baja");
        alta = nivel("Física alta");

        actYoga = actividad("Yoga al amanecer", yoga, baja, instructorA, "4000", 0.0);
        actRunning = actividad("Running en el parque", running, alta, instructorB, "4000", 0.0);
        actNatacion = actividad("Natación para adultos", natacion, alta, instructorB, "4000", 0.0);

        // Por defecto: sin historial de ningun tipo. Cada caso agrega lo suyo.
        lenient().when(perfilAlumnoRepository.findByUsuarioId(alumnoId)).thenReturn(Optional.empty());
        lenient().when(inscripcionRepository.findByAlumnoIdConDetalle(any(), isNull())).thenReturn(List.of());
        lenient().when(favoritoRepository.findByUsuarioId(alumnoId)).thenReturn(List.of());
        lenient().when(reseniaRepository.findByAlumnoIdConDetalle(alumnoId)).thenReturn(List.of());
        lenient().when(interaccionRepository.ultimasDelAlumno(any(), any())).thenReturn(List.of());
        lenient().when(actividadRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(actYoga, actRunning, actNatacion));
        lenient().when(claseRepository.findByActividadIdInAndEstadoNotInOrderByFechaHoraAsc(anyList(), anyList()))
                .thenReturn(List.of());
    }

    // --- Ayudantes de construccion ---

    private Usuario usuario(String nombre, String apellido) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido(apellido);
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    private Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    private TipoActividad tipo(String nombre, Categoria categoria) {
        TipoActividad t = new TipoActividad();
        t.setNombre(nombre);
        t.setCategoria(categoria);
        ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
        return t;
    }

    private NivelIntensidad nivel(String nombre) {
        NivelIntensidad n = new NivelIntensidad();
        n.setNombre(nombre);
        ReflectionTestUtils.setField(n, "id", UUID.randomUUID());
        return n;
    }

    private Actividad actividad(
            String nombre, TipoActividad tipo, NivelIntensidad nivel, Usuario instructor, String precio, double rating) {
        Actividad a = new Actividad();
        a.setNombre(nombre);
        a.setDescripcion("Descripción de " + nombre);
        a.setTipoActividad(tipo);
        a.setNivelIntensidad(nivel);
        a.setInstructor(instructor);
        a.setPrecio(new BigDecimal(precio));
        a.setUbicacion("Mendoza");
        a.setPhotoTint("#000000");
        a.setRating(BigDecimal.valueOf(rating));
        a.setDuracionMin(60);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    private Clase clase(Actividad actividad, Instant fechaHora, int cuposMax, int cuposOcupados) {
        Clase c = new Clase();
        c.setActividad(actividad);
        c.setFechaHora(fechaHora);
        c.setHoraFin(fechaHora.plusSeconds(3600));
        c.setEstado(EstadoClase.Programada);
        c.setCuposMax(cuposMax);
        c.setCuposOcupados(cuposOcupados);
        c.setPrecio(actividad.getPrecio());
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    private Inscripcion inscripcion(Actividad actividad, Instant cuando, EstadoInscripcion estado, String monto) {
        Inscripcion i = new Inscripcion();
        i.setClase(clase(actividad, cuando.plusSeconds(3600), 10, 1));
        i.setAlumno(alumno);
        i.setEstado(estado);
        if (monto != null) {
            Pago pago = new Pago();
            pago.setMonto(new BigDecimal(monto));
            i.setPago(pago);
        }
        ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(i, "createdAt", cuando);
        return i;
    }

    private ActividadFavorita favorito(Actividad actividad, Instant cuando) {
        ActividadFavorita f = new ActividadFavorita(alumno, actividad);
        ReflectionTestUtils.setField(f, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(f, "createdAt", cuando);
        return f;
    }

    private Resenia resenia(Actividad actividad, int puntaje, Instant cuando) {
        Resenia r = new Resenia();
        r.setClase(clase(actividad, cuando, 10, 1));
        r.setAlumno(alumno);
        r.setPuntaje(puntaje);
        ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(r, "createdAt", cuando);
        return r;
    }

    private void conIntereses(TipoActividad... tipos) {
        PerfilAlumno perfil = new PerfilAlumno(alumno, List.of(tipos));
        when(perfilAlumnoRepository.findByUsuarioId(alumnoId)).thenReturn(Optional.of(perfil));
    }

    private List<String> nombres(RecomendarActividadesResponse respuesta) {
        return respuesta.recomendadas().stream().map(RecomendarActividadesResponse.Recomendada::nombre).toList();
    }

    // --- Casos ---

    /**
     * El defecto que mas se veia: sin intereses ni historial, la pantalla vieja rellenaba con
     * las tres primeras del catalogo y las rotulaba "Recomendado para vos".
     */
    @Test
    void recomendar_alumnoSinInteresesNiHistorial_noRecomiendaNada() {
        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, null);

        assertThat(respuesta.sinSenales()).isTrue();
        assertThat(respuesta.recomendadas()).isEmpty();
    }

    @Test
    void recomendar_conInteresDeclarado_ponePrimeroEseTipoYExplicaPorQue() {
        conIntereses(yoga);

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, 3);

        assertThat(respuesta.sinSenales()).isFalse();
        assertThat(nombres(respuesta)).first().isEqualTo("Yoga al amanecer");
        assertThat(respuesta.recomendadas().get(0).motivos())
                .contains("Coincide con tu interés en Yoga");
        assertThat(respuesta.recomendadas().get(0).puntaje()).isGreaterThan(0);
    }

    /** Puntua y ordena: la lista sale de mayor a menor puntaje, no en el orden del catalogo. */
    @Test
    void recomendar_ordenaPorPuntajeDescendente() {
        conIntereses(natacion);

        List<RecomendarActividadesResponse.Recomendada> recomendadas =
                service.recomendar(alumnoId, null, null, 5).recomendadas();

        assertThat(recomendadas).isNotEmpty();
        assertThat(recomendadas).isSortedAccordingTo(
                (x, y) -> Double.compare(y.puntaje(), x.puntaje()));
        assertThat(recomendadas.get(0).nombre()).isEqualTo("Natación para adultos");
    }

    /**
     * El requisito explicito: dos alumnos con los MISMOS intereses declarados tienen que ver
     * cosas distintas si su historial es distinto. Los dos declaran Natación; el segundo ademas
     * ya hizo Yoga, que no comparte ni tipo ni categoria con lo declarado — sin historial, Yoga
     * no aparece en ninguna posicion, y con historial entra segundo.
     */
    @Test
    void recomendar_mismosInteresesDistintoHistorial_daResultadoDistinto() {
        conIntereses(natacion);
        List<String> sinHistorial = nombres(service.recomendar(alumnoId, null, null, 3));

        when(inscripcionRepository.findByAlumnoIdConDetalle(any(), isNull()))
                .thenReturn(List.of(inscripcion(actYoga, AHORA.minusSeconds(86_400), EstadoInscripcion.INSCRIPTO, "4000")));
        List<String> conHistorial = nombres(service.recomendar(alumnoId, null, null, 3));

        assertThat(sinHistorial).isNotEqualTo(conHistorial);
        assertThat(sinHistorial).doesNotContain("Yoga al amanecer");
        assertThat(conHistorial).contains("Yoga al amanecer");
        // Lo declarado sigue pesando mas que una sola clase hecha.
        assertThat(conHistorial.get(0)).isEqualTo("Natación para adultos");
    }

    /** Mira las inscripciones previas aunque el alumno no haya declarado un solo interes. */
    @Test
    void recomendar_sinInteresesPeroConClasesHechas_recomiendaPorEsaHistoria() {
        when(inscripcionRepository.findByAlumnoIdConDetalle(any(), isNull()))
                .thenReturn(List.of(inscripcion(actYoga, AHORA.minusSeconds(86_400), EstadoInscripcion.INSCRIPTO, "4000")));

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, 3);

        assertThat(respuesta.sinSenales()).isFalse();
        assertThat(nombres(respuesta)).first().isEqualTo("Yoga al amanecer");
        assertThat(respuesta.recomendadas().get(0).motivos())
                .anyMatch(m -> m.startsWith("Parecida a las clases de Yoga"));
    }

    /** Una inscripcion cancelada no es evidencia de gusto: el alumno se arrepintio. */
    @Test
    void recomendar_inscripcionCancelada_noCuentaComoSenal() {
        when(inscripcionRepository.findByAlumnoIdConDetalle(any(), isNull()))
                .thenReturn(List.of(inscripcion(actYoga, AHORA.minusSeconds(86_400), EstadoInscripcion.CANCELADA, null)));

        assertThat(service.recomendar(alumnoId, null, null, 3).sinSenales()).isTrue();
    }

    /** Un favorito es señal de gusto, pero la actividad ya la tiene guardada: no se recomienda. */
    @Test
    void recomendar_favorito_cuentaComoSenalPeroNoSeRecomienda() {
        when(favoritoRepository.findByUsuarioId(alumnoId))
                .thenReturn(List.of(favorito(actYoga, AHORA.minusSeconds(86_400))));

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, 5);

        assertThat(respuesta.sinSenales()).isFalse();
        assertThat(nombres(respuesta)).doesNotContain("Yoga al amanecer");
    }

    /** Una clase futura ya anotada es un plan hecho: recomendarla de nuevo no agrega nada. */
    @Test
    void recomendar_conInscripcionAUnaClaseFutura_excluyeEsaActividad() {
        Inscripcion futura = inscripcion(actYoga, AHORA.minusSeconds(3600), EstadoInscripcion.INSCRIPTO, "4000");
        futura.setClase(clase(actYoga, AHORA.plusSeconds(86_400), 10, 1));
        when(inscripcionRepository.findByAlumnoIdConDetalle(any(), isNull())).thenReturn(List.of(futura));

        assertThat(nombres(service.recomendar(alumnoId, null, null, 5))).doesNotContain("Yoga al amanecer");
    }

    /** Una reseña mala es la unica señal negativa, y tiene que poder hundir a su propio tipo. */
    @Test
    void recomendar_reseniaMala_bajaEseTipoDeActividad() {
        conIntereses(yoga, running);
        when(reseniaRepository.findByAlumnoIdConDetalle(alumnoId))
                .thenReturn(List.of(resenia(actYoga, 1, AHORA.minusSeconds(86_400))));

        List<String> orden = nombres(service.recomendar(alumnoId, null, null, 3));

        assertThat(orden.indexOf("Running en el parque")).isLessThan(orden.indexOf("Yoga al amanecer"));
    }

    @Test
    void recomendar_reseniaExcelente_subeEseTipoDeActividad() {
        conIntereses(yoga, running);
        when(reseniaRepository.findByAlumnoIdConDetalle(alumnoId))
                .thenReturn(List.of(resenia(actYoga, 5, AHORA.minusSeconds(86_400))));

        assertThat(nombres(service.recomendar(alumnoId, null, null, 3)).get(0)).isEqualTo("Yoga al amanecer");
    }

    /** Señal de V27: lo que el alumno mira tambien cuenta, aunque pese menos que lo que hace. */
    @Test
    void recomendar_vistaDeActividad_cuentaComoSenal() {
        when(interaccionRepository.ultimasDelAlumno(any(), any()))
                .thenReturn(List.of(interaccionVista(actNatacion, AHORA.minusSeconds(3600))));

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, 3);

        assertThat(respuesta.sinSenales()).isFalse();
        assertThat(nombres(respuesta)).first().isEqualTo("Natación para adultos");
    }

    /** La otra señal de V27: el termino buscado se cruza contra los nombres del catalogo. */
    @Test
    void recomendar_busquedaSinTildes_cuentaComoSenalDelTipoBuscado() {
        when(interaccionRepository.ultimasDelAlumno(any(), any()))
                .thenReturn(List.of(interaccionBusqueda("natacion", AHORA.minusSeconds(3600))));

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, 3);

        assertThat(respuesta.sinSenales()).isFalse();
        assertThat(nombres(respuesta)).first().isEqualTo("Natación para adultos");
    }

    /** Un termino que no matchea ningun nombre del catalogo no es señal de nada. */
    @Test
    void recomendar_busquedaQueNoCoincideConElCatalogo_noEsSenal() {
        when(interaccionRepository.ultimasDelAlumno(any(), any()))
                .thenReturn(List.of(interaccionBusqueda("esgrima medieval", AHORA.minusSeconds(3600))));

        assertThat(service.recomendar(alumnoId, null, null, 3).sinSenales()).isTrue();
    }

    /** Con coordenadas del cliente, entre dos actividades iguales gana la mas cercana. */
    @Test
    void recomendar_conCoordenadas_priorizaLaMasCercanaYLoDice() {
        conIntereses(running, natacion);
        actRunning.setLatitud(-32.8908);
        actRunning.setLongitud(-68.8272);
        actNatacion.setLatitud(-34.6037);
        actNatacion.setLongitud(-58.3816);

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, -32.8895, -68.8458, 3);

        assertThat(nombres(respuesta)).first().isEqualTo("Running en el parque");
        assertThat(respuesta.recomendadas().get(0).motivos()).anyMatch(m -> m.endsWith("km tuyo"));
    }

    /** Sin coordenadas la cercania no participa: no se inventa una distancia. */
    @Test
    void recomendar_sinCoordenadas_noMencionaDistancia() {
        conIntereses(running);
        actRunning.setLatitud(-32.8908);
        actRunning.setLongitud(-68.8272);

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, 3);

        assertThat(respuesta.recomendadas().get(0).motivos()).noneMatch(m -> m.endsWith("km tuyo"));
    }

    /** El precio tipico sale de lo que ya pago: una actividad diez veces mas cara no encabeza. */
    @Test
    void recomendar_precioMuyLejosDelHabitual_quedaMasAbajo() {
        conIntereses(running, natacion);
        actNatacion.setPrecio(new BigDecimal("99999"));
        when(inscripcionRepository.findByAlumnoIdConDetalle(any(), isNull()))
                .thenReturn(List.of(inscripcion(actYoga, AHORA.minusSeconds(86_400), EstadoInscripcion.INSCRIPTO, "4000")));

        List<String> orden = nombres(service.recomendar(alumnoId, null, null, 5));

        assertThat(orden.indexOf("Running en el parque")).isLessThan(orden.indexOf("Natación para adultos"));
    }

    /** Una actividad sin proxima clase no se puede cursar: baja, pero no desaparece. */
    @Test
    void recomendar_conProximaClaseConCupo_quedaArribaDeLaQueNoTieneClases() {
        conIntereses(running, natacion);
        when(claseRepository.findByActividadIdInAndEstadoNotInOrderByFechaHoraAsc(anyList(), anyList()))
                .thenReturn(List.of(clase(actNatacion, AHORA.plusSeconds(86_400), 10, 2)));

        List<String> orden = nombres(service.recomendar(alumnoId, null, null, 5));

        assertThat(orden.indexOf("Natación para adultos")).isLessThan(orden.indexOf("Running en el parque"));
    }

    /** Ya haberla hecho no la descarta, pero la corre hacia abajo. */
    @Test
    void recomendar_actividadYaCursada_quedaPenalizadaYLoAclara() {
        when(inscripcionRepository.findByAlumnoIdConDetalle(any(), isNull()))
                .thenReturn(List.of(inscripcion(actYoga, AHORA.minusSeconds(86_400 * 5), EstadoInscripcion.INSCRIPTO, "4000")));

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, 5);

        assertThat(respuesta.recomendadas()).anyMatch(r -> r.nombre().equals("Yoga al amanecer")
                && r.motivos().contains("Ya la hiciste antes"));
    }

    /** El limite es del pedido, pero con un tope duro: es una lista curada, no un catalogo. */
    @Test
    void recomendar_limiteMayorAlTope_seRecorta() {
        conIntereses(yoga, running, natacion);

        assertThat(service.recomendar(alumnoId, null, null, 500).recomendadas()).hasSizeLessThanOrEqualTo(20);
        assertThat(service.recomendar(alumnoId, null, null, null).recomendadas()).hasSizeLessThanOrEqualTo(3);
        assertThat(service.recomendar(alumnoId, null, null, 1).recomendadas()).hasSize(1);
    }

    /** Sin nada positivo que decir, no se recomienda: un puntaje negativo no se muestra. */
    @Test
    void recomendar_todaLaEvidenciaEnContra_devuelveListaVacia() {
        when(reseniaRepository.findByAlumnoIdConDetalle(alumnoId))
                .thenReturn(List.of(
                        resenia(actYoga, 1, AHORA.minusSeconds(3600)),
                        resenia(actRunning, 1, AHORA.minusSeconds(3600)),
                        resenia(actNatacion, 1, AHORA.minusSeconds(3600))));

        RecomendarActividadesResponse respuesta = service.recomendar(alumnoId, null, null, 5);

        assertThat(respuesta.sinSenales()).isFalse();
        assertThat(respuesta.recomendadas()).isEmpty();
    }

    /** Lo reciente pesa mas que lo viejo: los gustos cambian. */
    @Test
    void recomendar_senalVieja_pesaMenosQueUnaReciente() {
        when(inscripcionRepository.findByAlumnoIdConDetalle(any(), isNull()))
                .thenReturn(List.of(
                        inscripcion(actYoga, AHORA.minusSeconds(86_400L * 200), EstadoInscripcion.INSCRIPTO, "4000"),
                        inscripcion(actRunning, AHORA.minusSeconds(86_400L * 2), EstadoInscripcion.INSCRIPTO, "4000")));

        List<String> orden = nombres(service.recomendar(alumnoId, null, null, 5));

        assertThat(orden.indexOf("Running en el parque")).isLessThan(orden.indexOf("Yoga al amanecer"));
    }

    private InteraccionAlumno interaccionVista(Actividad actividad, Instant cuando) {
        InteraccionAlumno i = InteraccionAlumno.vista(alumno, actividad);
        ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(i, "createdAt", cuando);
        return i;
    }

    private InteraccionAlumno interaccionBusqueda(String termino, Instant cuando) {
        InteraccionAlumno i = InteraccionAlumno.busqueda(alumno, termino);
        ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(i, "createdAt", cuando);
        return i;
    }

    /** Evita que el compilador se queje del generico crudo en el stub de findAll. */
    @SuppressWarnings("unused")
    private static List<Actividad> vacio() {
        return new ArrayList<>();
    }
}
