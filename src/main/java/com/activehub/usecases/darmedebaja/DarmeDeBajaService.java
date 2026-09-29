package com.activehub.usecases.darmedebaja;

import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E3A-HU12 criterio 8: el usuario da de baja su propia cuenta.
 *
 * <p>Es baja LOGICA (RN-13): el historial de inscripciones, pagos, reseñas y auditoria se
 * conserva. El indice unico de email es parcial (WHERE deleted = false), asi que el correo
 * queda liberado para registrarse de nuevo.
 *
 * <p>No se cancelan las inscripciones vigentes: eso afectaria cupos y pagos de terceros sin
 * que nadie lo decida. En cambio se bloquea la baja si quedan compromisos activos, para que
 * el usuario los cancele primero y el instructor no se entere tarde.
 *
 * <p><b>El ultimo ADMIN no puede darse de baja.</b> Es el mismo agujero que ya tapaban
 * {@code asignarrolusuario} ("no se puede dejar la plataforma sin su ultimo ADMIN") y la
 * guarda anti-auto-suspension de {@code actualizarestadousuario}, y era el unico camino que
 * quedaba abierto para dejar el sistema sin gobierno: una vez borrada esa cuenta no hay
 * ninguna pantalla desde la cual devolverle el rol ADMIN a nadie, y la unica salida es tocar
 * la base a mano. Se cuentan las cuentas VIVAS del rol, no las activas: un admin suspendido
 * sigue pudiendo volver (la suspension se levanta), uno borrado no.
 */
@Service
public class DarmeDeBajaService {

    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public DarmeDeBajaService(UsuarioRepository usuarioRepository, AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void darDeBaja(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        if (usuario.isDeleted()) {
            throw new ValidacionException("Tu cuenta ya está dada de baja.");
        }

        if (RolNombre.ADMIN.name().equals(usuario.getRol().getNombre())
                && usuarioRepository.countByRolIdAndDeletedFalse(usuario.getRol().getId()) <= 1) {
            throw new ValidacionException(
                    "Sos el único administrador de la plataforma: asigná el rol Administrador a otra cuenta "
                            + "antes de darte de baja.");
        }

        usuario.marcarBorrado();
        usuarioRepository.save(usuario);

        auditService.registrar(usuarioId, AuditAccion.CUENTA_DADA_DE_BAJA, "Usuario", usuarioId, null);
    }
}
