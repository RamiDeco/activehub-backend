package com.activehub.usecases.levantarsuspensionesvencidas;

import com.activehub.domain.penalizacion.Penalizacion;
import com.activehub.domain.penalizacion.PenalizacionRepository;
import com.activehub.domain.penalizacion.TipoPenalizacion;
import com.activehub.domain.usuario.EstadoUsuario;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4Ad-HU06 criterio 4: una Suspension temporal restringe el acceso del usuario
 * <em>durante ese periodo</em>. Este job devuelve a ACTIVO a los usuarios cuya suspension ya
 * vencio; sin el, "temporal" seria un nombre de fantasia y toda suspension duraria hasta que
 * un admin la levantara a mano.
 *
 * <p>Solo reactiva si el usuario NO tiene ninguna otra suspension vigente: alguien puede
 * acumular sanciones superpuestas y la mas larga tiene que seguir valiendo.
 */
@Service
public class LevantarSuspensionesVencidasService {

    private static final ZoneId ZONA_AR = ZoneId.of("America/Argentina/Buenos_Aires");

    private final PenalizacionRepository penalizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final Clock clock;

    public LevantarSuspensionesVencidasService(
            PenalizacionRepository penalizacionRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.penalizacionRepository = penalizacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public int levantar() {
        LocalDate hoy = LocalDate.ofInstant(clock.instant(), ZONA_AR);
        List<Penalizacion> vencidas =
                penalizacionRepository.findSuspensionesVencidas(TipoPenalizacion.SUSPENSION_TEMPORAL, hoy);

        int levantadas = 0;
        for (Penalizacion p : vencidas) {
            Usuario usuario = p.getUsuario();
            if (usuario.getEstado() != EstadoUsuario.SUSPENDIDO) {
                continue;
            }
            if (tieneOtraSuspensionVigente(usuario, hoy)) {
                continue;
            }
            usuario.setEstado(EstadoUsuario.ACTIVO);
            usuarioRepository.save(usuario);
            auditService.registrar(
                    null, AuditAccion.SUSPENSION_LEVANTADA, "Usuario", usuario.getId(), "vigencia cumplida");
            levantadas++;
        }
        return levantadas;
    }

    private boolean tieneOtraSuspensionVigente(Usuario usuario, LocalDate hoy) {
        return penalizacionRepository.findAllConDetalle().stream()
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()))
                .filter(p -> p.getTipo() == TipoPenalizacion.SUSPENSION_TEMPORAL)
                .filter(p -> p.getFechaFin() != null)
                .anyMatch(p -> !hoy.isAfter(p.getFechaFin()));
    }
}
