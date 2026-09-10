package com.activehub.usecases.actualizarpermisosrol;

import com.activehub.domain.permiso.ConfiguracionRol;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.permiso.Permiso;
import com.activehub.domain.permiso.PermisoRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolNombre;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.NoEncontradoException;
import com.activehub.shared.error.ValidacionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4Ad-HU08 criterios 2, 6 y 7. Guarda la matriz completa de un rol de una: lo que no viene
 * en la lista queda deshabilitado, asi el "Guardar cambios" de la pantalla es idempotente y
 * no depende de saber que checkbox se toco.
 */
@Service
public class ActualizarPermisosRolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final ConfiguracionRolRepository configuracionRolRepository;
    private final AuditService auditService;

    public ActualizarPermisosRolService(
            RolRepository rolRepository,
            PermisoRepository permisoRepository,
            ConfiguracionRolRepository configuracionRolRepository,
            AuditService auditService
    ) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.configuracionRolRepository = configuracionRolRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ActualizarPermisosRolResponse actualizar(UUID rolId, ActualizarPermisosRolRequest request, UUID actorId) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new NoEncontradoException("Rol no encontrado."));

        List<Permiso> catalogo = permisoRepository.findAllByOrderByOrdenAsc();
        Map<String, Permiso> porClave = catalogo.stream()
                .collect(Collectors.toMap(Permiso::getClave, Function.identity()));

        Set<String> habilitadas = new HashSet<>(request.permisos());
        // Los implicitos no son una decision del administrador: se fuerzan encendidos venga lo
        // que venga del cliente. Si dependieran del payload, un rol nuevo se quedaria sin
        // `catalogo.explorar` (y sin favoritos) solo porque la pantalla ya no lo muestra.
        habilitadas.addAll(Permiso.IMPLICITOS);
        for (String clave : habilitadas) {
            if (!porClave.containsKey(clave)) {
                throw new ValidacionException("El permiso '" + clave + "' no existe.");
            }
        }

        // Criterio 6: dejar al Administrador sin sus permisos criticos deja la plataforma sin
        // gobierno, y no habria como volver atras desde la propia pantalla. Se rechaza.
        if (RolNombre.ADMIN.name().equals(rol.getNombre())) {
            boolean pierdeAlgoCritico = catalogo.stream()
                    .anyMatch(p -> p.isCritico() && !habilitadas.contains(p.getClave()));
            if (pierdeAlgoCritico) {
                throw new ValidacionException(
                        "Quitar este permiso dejaría al rol Administrador sin capacidad de gestión. "
                                + "Esta acción no está permitida.");
            }
        }

        Map<String, ConfiguracionRol> existentes = configuracionRolRepository.findByRolId(rolId).stream()
                .collect(Collectors.toMap(c -> c.getPermiso().getClave(), Function.identity()));

        List<String> cambios = new ArrayList<>();
        for (Permiso permiso : catalogo) {
            boolean debeEstar = habilitadas.contains(permiso.getClave());
            ConfiguracionRol config = existentes.get(permiso.getClave());
            if (config == null) {
                // Rol nuevo o permiso agregado despues: la fila se crea al primer guardado.
                config = new ConfiguracionRol();
                config.setRol(rol);
                config.setPermiso(permiso);
                config.setHabilitado(debeEstar);
                configuracionRolRepository.save(config);
                if (debeEstar) {
                    cambios.add("+" + permiso.getClave());
                }
                continue;
            }
            if (config.isHabilitado() != debeEstar) {
                config.setHabilitado(debeEstar);
                configuracionRolRepository.save(config);
                cambios.add((debeEstar ? "+" : "-") + permiso.getClave());
            }
        }

        // Criterio 7: queda en auditoria con autor, fecha y detalle de lo que cambio.
        auditService.registrar(
                actorId, AuditAccion.PERMISOS_ACTUALIZADOS, "Rol", rolId,
                cambios.isEmpty() ? "sin cambios" : String.join(", ", cambios));

        List<String> finales = catalogo.stream()
                .map(Permiso::getClave)
                .filter(habilitadas::contains)
                .toList();

        return new ActualizarPermisosRolResponse(rol.getId(), rol.getNombre(), finales);
    }
}
