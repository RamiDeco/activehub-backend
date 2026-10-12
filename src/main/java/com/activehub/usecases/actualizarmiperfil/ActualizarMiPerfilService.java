package com.activehub.usecases.actualizarmiperfil;

import com.activehub.domain.usuario.Usuario;
import com.activehub.domain.usuario.UsuarioRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.DniEnUsoException;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E3A-HU12 criterio 2 (y E2I-HU12 criterio 4): el usuario edita sus propios datos.
 *
 * <p>Hasta ahora no existia ningun endpoint para esto: la pantalla de Perfil llamaba a una
 * funcion del frontend que solo mutaba un array mock, asi que "Guardar cambios" no persistia
 * nada y al recargar volvia todo atras.
 *
 * <p>Sirve para los tres roles — el instructor no verificado tambien puede editar sus datos
 * (es lo unico que la spec le habilita mientras espera la validacion).
 */
@Service
public class ActualizarMiPerfilService {

    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public ActualizarMiPerfilService(UsuarioRepository usuarioRepository, AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarMiPerfilResponse actualizar(UUID usuarioId, ActualizarMiPerfilRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado."));

        // **El correo NO se cambia acá.** Este endpoint editaba `usuario.email` directamente,
        // que desde V26 es un agujero: el correo es la credencial verificada y lo que reserva
        // la dirección, así que cambiarlo sin confirmar dejaría tomar cualquier casilla ajena
        // con un PUT. El cambio real vive en `solicitarcambioemail` + `verificaremail`, que
        // mandan un código a la dirección nueva.
        //
        // Se rechaza en vez de ignorarlo en silencio: un formulario que dice haber guardado
        // un correo que no guardó es peor que un error.
        String emailNuevo = request.email().trim().toLowerCase();
        if (!emailNuevo.equalsIgnoreCase(usuario.getEmail())) {
            throw new ValidacionException(
                    "El correo se cambia desde \"Correo electrónico\", confirmando un código.",
                    Map.of("email", "Para cambiar tu correo usá la opción \"Cambiar mi correo\"."));
        }

        // El DNI sí se puede cargar acá, y es la única forma de hacerlo después del alta: quien
        // se registró con Google nunca pasó por un formulario que lo pidiera. Sigue siendo
        // clave de unicidad, así que no puede pisar el de otra cuenta.
        String dni = request.dni() != null && !request.dni().isBlank() ? request.dni().trim() : null;
        if (dni != null && usuarioRepository.existsByDniAndIdNotAndDeletedFalse(dni, usuarioId)) {
            throw new DniEnUsoException();
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setDni(dni);
        usuario.setTelefono(request.telefono().trim());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuarioRepository.save(usuario);

        auditService.registrar(usuarioId, AuditAccion.PERFIL_ACTUALIZADO, "Usuario", usuarioId, null);

        return new ActualizarMiPerfilResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getFechaNacimiento(),
                usuario.getDni());
    }
}
