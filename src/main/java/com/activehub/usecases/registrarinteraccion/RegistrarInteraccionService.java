package com.activehub.usecases.registrarinteraccion;

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
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda una señal de comportamiento del alumno (V27): que actividad miro, que busco.
 *
 * <h2>Por que hay un endpoint en vez de deducirlo del trafico</h2>
 *
 * Ver una actividad ya pega a {@code GET /api/actividades/{id}}, pero ese endpoint es
 * <b>publico</b> y lo usa cualquiera sin sesion; registrar ahi obligaria a meter una escritura
 * dentro de una lectura del catalogo y a decidir, en el mismo lugar, si hay alumno o no.
 * Buscar directamente no pega a ningun endpoint: el filtrado de Explorar es todo del lado del
 * cliente. Un endpoint propio deja las dos señales explicitas y opcionales.
 *
 * <h2>Nunca falla la pantalla</h2>
 *
 * Es telemetria de producto, no una operacion del usuario: si algo sale mal, el alumno no tiene
 * que enterarse. Por eso el Controller devuelve 204 siempre que el pedido sea valido, y las
 * unicas dos cosas que rechaza son un tipo desconocido y un pedido sin el contenido que ese
 * tipo necesita -- errores de programacion del cliente, no del usuario.
 *
 * <h2>Antirrebote</h2>
 *
 * {@link #VENTANA_ANTIRREBOTE} evita una fila por cada repintado del detalle o por cada tecla
 * del buscador: abrir diez veces la misma actividad no puede convertirla en el gusto dominante
 * del alumno. La deduplicacion es por (alumno, tipo, contenido) dentro de la ventana.
 */
@Service
public class RegistrarInteraccionService {

    /** Dos interacciones identicas dentro de esta ventana cuentan como una sola. */
    static final Duration VENTANA_ANTIRREBOTE = Duration.ofMinutes(10);

    /** Debajo de esto no es una busqueda, es alguien tipeando. */
    static final int LARGO_MINIMO_TERMINO = 3;

    private final UsuarioRepository usuarioRepository;
    private final ActividadRepository actividadRepository;
    private final InteraccionAlumnoRepository interaccionRepository;
    private final Clock clock;

    public RegistrarInteraccionService(
            UsuarioRepository usuarioRepository,
            ActividadRepository actividadRepository,
            InteraccionAlumnoRepository interaccionRepository,
            Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.actividadRepository = actividadRepository;
        this.interaccionRepository = interaccionRepository;
        this.clock = clock;
    }

    @Transactional
    public void registrar(UUID alumnoId, RegistrarInteraccionRequest request) {
        TipoInteraccion tipo = parsear(request.tipo());
        Usuario alumno = usuarioRepository.findById(alumnoId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        if (tipo == TipoInteraccion.VISTA_ACTIVIDAD) {
            registrarVista(alumno, request.actividadId());
        } else {
            registrarBusqueda(alumno, request.termino());
        }
    }

    private void registrarVista(Usuario alumno, UUID actividadId) {
        if (actividadId == null) {
            throw new ValidacionException("Falta la actividad de la interacción.");
        }
        // Una actividad dada de baja no existe para el catalogo, asi que tampoco como señal.
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new NoEncontradoException("Actividad no encontrada."));

        if (interaccionRepository.existeVistaReciente(alumno.getId(), actividadId, desde())) {
            return;
        }
        interaccionRepository.save(InteraccionAlumno.vista(alumno, actividad));
    }

    private void registrarBusqueda(Usuario alumno, String termino) {
        if (termino == null || termino.isBlank()) {
            throw new ValidacionException("Falta el término de la búsqueda.");
        }
        String normalizado = termino.trim();
        if (normalizado.length() < LARGO_MINIMO_TERMINO) {
            // No es un error del cliente: es una busqueda que no dice nada. Se descarta en
            // silencio para que la pantalla no tenga que replicar este umbral.
            return;
        }
        if (interaccionRepository.existeBusquedaReciente(alumno.getId(), normalizado, desde())) {
            return;
        }
        interaccionRepository.save(InteraccionAlumno.busqueda(alumno, normalizado));
    }

    private Instant desde() {
        return clock.instant().minus(VENTANA_ANTIRREBOTE);
    }

    private TipoInteraccion parsear(String tipo) {
        try {
            return TipoInteraccion.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacionException("Tipo de interacción desconocido.");
        }
    }
}
