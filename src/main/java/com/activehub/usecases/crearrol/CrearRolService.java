package com.activehub.usecases.crearrol;

import com.activehub.domain.permiso.ConfiguracionRol;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.permiso.Permiso;
import com.activehub.domain.permiso.PermisoRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.RolDuplicadoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4Ad-HU08 criterio 3: el rol nuevo nace <b>sin permisos asignados</b>. Se le crean igual
 * las filas de {@code ConfiguracionRol} en false para que la matriz de la pantalla no tenga
 * huecos y el primer "Guardar cambios" no dependa de que existan.
 *
 * <p>Un rol creado aca no se le puede asignar todavia a una cuenta: el motor de seguridad
 * solo entiende los tres roles del sistema (ver {@code Rol.getNombreSistema()}). Sirve para
 * preparar la configuracion; asignarlo es trabajo aparte y hoy no esta pedido.
 */
@Service
public class CrearRolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final ConfiguracionRolRepository configuracionRolRepository;
    private final AuditService auditService;

    public CrearRolService(
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
    public CrearRolResponse crear(CrearRolRequest request, UUID actorId) {
        String nombre = request.nombre().trim();
        // Criterio 5: sin duplicados, case-insensitive contra las filas no borradas.
        if (rolRepository.existsByNombreIgnoreCaseAndDeletedFalse(nombre)) {
            throw new RolDuplicadoException();
        }

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(request.descripcion() == null || request.descripcion().isBlank()
                ? null : request.descripcion().trim());
        rol.setSistema(false);
        rol = rolRepository.saveAndFlush(rol);

        for (Permiso permiso : permisoRepository.findAllByOrderByOrdenAsc()) {
            ConfiguracionRol config = new ConfiguracionRol();
            config.setRol(rol);
            config.setPermiso(permiso);
            // Criterio 3: nace sin permisos. Los implicitos son la excepcion — no son un
            // permiso que se conceda, son la linea de base de cualquier rol.
            config.setHabilitado(Permiso.IMPLICITOS.contains(permiso.getClave()));
            configuracionRolRepository.save(config);
        }

        auditService.registrar(actorId, AuditAccion.ROL_CREADO, "Rol", rol.getId(), nombre);

        return new CrearRolResponse(rol.getId(), rol.getNombre(), rol.getDescripcion(), false, 0);
    }
}
