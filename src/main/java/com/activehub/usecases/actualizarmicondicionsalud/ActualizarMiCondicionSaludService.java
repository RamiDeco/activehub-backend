package com.activehub.usecases.actualizarmicondicionsalud;

import com.activehub.domain.usuario.PerfilAlumno;
import com.activehub.domain.usuario.PerfilAlumnoRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El alumno edita lo que informó en "¿Padecés alguna enfermedad o lesión?".
 *
 * <p>Hasta acá el dato se cargaba sólo en el alta ({@code registraralumno}) y después no se
 * podía ver ni cambiar: una lesión que se curaba o una nueva quedaba como el día del registro.
 * Importa porque lo usa {@code generarinformeactividad} para el "Asistente de beneficios y
 * prevenciones", y un informe hecho sobre un dato viejo es peor que uno sin el dato.
 *
 * <p>El texto en blanco se guarda como null ("no informó nada"), igual que el comentario de
 * una reseña. La auditoría registra que se actualizó, <b>nunca el contenido</b>: es un dato de
 * salud y la tabla de auditoría la lee el administrador.
 */
@Service
public class ActualizarMiCondicionSaludService {

    private final PerfilAlumnoRepository perfilAlumnoRepository;
    private final AuditService auditService;

    public ActualizarMiCondicionSaludService(PerfilAlumnoRepository perfilAlumnoRepository, AuditService auditService) {
        this.perfilAlumnoRepository = perfilAlumnoRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarMiCondicionSaludResponse actualizar(UUID usuarioId, ActualizarMiCondicionSaludRequest request) {
        PerfilAlumno perfil = perfilAlumnoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Solo un alumno puede editar este dato."));

        perfil.setCondicionSalud(normalizar(request.condicionSalud()));
        perfilAlumnoRepository.save(perfil);

        auditService.registrar(usuarioId, AuditAccion.CONDICION_SALUD_ACTUALIZADA, "PerfilAlumno", perfil.getId(), null);

        return new ActualizarMiCondicionSaludResponse(usuarioId, perfil.getCondicionSalud());
    }

    private static String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
