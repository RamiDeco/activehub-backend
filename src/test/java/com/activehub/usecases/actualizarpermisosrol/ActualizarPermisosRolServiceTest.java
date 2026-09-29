package com.activehub.usecases.actualizarpermisosrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * E4Ad-HU08: guardar la matriz de un rol. Lo que importa acá es que el guardado sea la
 * foto completa (lo que no viene queda apagado) y que el Administrador no pueda quedarse
 * sin sus permisos críticos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActualizarPermisosRolServiceTest {

    @Mock private RolRepository rolRepository;
    @Mock private PermisoRepository permisoRepository;
    @Mock private ConfiguracionRolRepository configuracionRolRepository;
    @Mock private AuditService auditService;

    private ActualizarPermisosRolService service;

    private UUID rolId;
    private UUID actorId;
    private Rol rolInstructor;
    /** Implicito (Permiso.IMPLICITOS): el Service lo enciende venga o no en el payload. */
    private Permiso explorar;
    private Permiso publicar;
    private Permiso cobros;
    private Permiso gestionarUsuarios;

    @BeforeEach
    void setUp() {
        service = new ActualizarPermisosRolService(
                rolRepository, permisoRepository, configuracionRolRepository, auditService);

        rolId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        rolInstructor = rol(RolNombre.INSTRUCTOR.name(), rolId);
        explorar = permiso("catalogo.explorar", false, 10);
        publicar = permiso("actividades.publicar", false, 50);
        cobros = permiso("cobros.confirmar", false, 70);
        gestionarUsuarios = permiso("usuarios.gestionar", true, 80);

        when(rolRepository.findById(rolId)).thenReturn(Optional.of(rolInstructor));
        when(permisoRepository.findAllByOrderByOrdenAsc())
                .thenReturn(List.of(explorar, publicar, cobros, gestionarUsuarios));
        when(configuracionRolRepository.findByRolId(rolId)).thenReturn(new ArrayList<>());
    }

    private Rol rol(String nombre, UUID id) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setSistema(true);
        ReflectionTestUtils.setField(rol, "id", id);
        return rol;
    }

    private Permiso permiso(String clave, boolean critico, int orden) {
        Permiso permiso = new Permiso();
        permiso.setClave(clave);
        permiso.setModulo("Módulo");
        permiso.setAccion("Acción");
        permiso.setCritico(critico);
        permiso.setOrden(orden);
        ReflectionTestUtils.setField(permiso, "id", UUID.randomUUID());
        return permiso;
    }

    private ConfiguracionRol config(Rol rol, Permiso permiso, boolean habilitado) {
        ConfiguracionRol config = new ConfiguracionRol();
        config.setRol(rol);
        config.setPermiso(permiso);
        config.setHabilitado(habilitado);
        config.setId(UUID.randomUUID());
        return config;
    }

    @Test
    void actualizar_sinFilasPrevias_lasCreaConElValorPedido() {
        var response = service.actualizar(
                rolId, new ActualizarPermisosRolRequest(List.of("actividades.publicar")), actorId);

        assertThat(response.permisos()).containsExactly("catalogo.explorar", "actividades.publicar");
        // Cuatro filas: las dos habilitadas (la pedida y el implicito) y las dos apagadas,
        // para que la matriz no tenga huecos.
        verify(configuracionRolRepository, org.mockito.Mockito.times(4)).save(any(ConfiguracionRol.class));
    }

    @Test
    void actualizar_apagaLoQueNoVieneEnLaLista() {
        var existente = config(rolInstructor, publicar, true);
        when(configuracionRolRepository.findByRolId(rolId)).thenReturn(new ArrayList<>(List.of(existente)));

        service.actualizar(rolId, new ActualizarPermisosRolRequest(List.of("cobros.confirmar")), actorId);

        assertThat(existente.isHabilitado()).isFalse();
    }

    @Test
    void actualizar_noReescribeLoQueNoCambio() {
        var existente = config(rolInstructor, publicar, true);
        when(configuracionRolRepository.findByRolId(rolId)).thenReturn(new ArrayList<>(List.of(existente)));

        service.actualizar(
                rolId, new ActualizarPermisosRolRequest(List.of("actividades.publicar")), actorId);

        // Solo se guardan las tres filas que faltaban; la que ya estaba en true no se toca.
        verify(configuracionRolRepository, org.mockito.Mockito.times(3)).save(any(ConfiguracionRol.class));
    }

    @Test
    void actualizar_auditaConElDetalleDeLoQueCambio() {
        service.actualizar(rolId, new ActualizarPermisosRolRequest(List.of("cobros.confirmar")), actorId);

        // Criterio 7: autor, fecha y detalle del permiso modificado.
        verify(auditService).registrar(
                eq(actorId), eq(AuditAccion.PERMISOS_ACTUALIZADOS), eq("Rol"), eq(rolId),
                eq("+catalogo.explorar, +cobros.confirmar"));
    }

    @Test
    void actualizar_adminSinPermisoCritico_lanzaValidacion() {
        UUID adminId = UUID.randomUUID();
        Rol rolAdmin = rol(RolNombre.ADMIN.name(), adminId);
        when(rolRepository.findById(adminId)).thenReturn(Optional.of(rolAdmin));
        when(configuracionRolRepository.findByRolId(adminId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service.actualizar(
                adminId, new ActualizarPermisosRolRequest(List.of("actividades.publicar")), actorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("sin capacidad de gestión");

        verify(configuracionRolRepository, never()).save(any());
    }

    @Test
    void actualizar_adminConSusCriticos_siGuarda() {
        UUID adminId = UUID.randomUUID();
        Rol rolAdmin = rol(RolNombre.ADMIN.name(), adminId);
        when(rolRepository.findById(adminId)).thenReturn(Optional.of(rolAdmin));
        when(configuracionRolRepository.findByRolId(adminId)).thenReturn(new ArrayList<>());

        var response = service.actualizar(
                adminId, new ActualizarPermisosRolRequest(List.of("usuarios.gestionar")), actorId);

        assertThat(response.permisos()).containsExactly("catalogo.explorar", "usuarios.gestionar");
    }

    @Test
    void actualizar_otroRolPuedeQuedarseSinPermisosCriticos() {
        // La guarda es solo para el Administrador: un rol nuevo puede no tener ninguno.
        var response = service.actualizar(rolId, new ActualizarPermisosRolRequest(List.of()), actorId);

        assertThat(response.permisos()).containsExactly("catalogo.explorar");
    }

    @Test
    void actualizar_permisoImplicito_quedaEncendidoAunqueNoVengaEnElPayload() {
        // `catalogo.explorar` no es una decision del administrador: lo tiene todo rol y la
        // pantalla ya no lo ofrece como checkbox (V22). Si dependiera del payload, un rol
        // nuevo se quedaria sin favoritos solo porque el frontend dejo de mandarlo.
        var response = service.actualizar(
                rolId, new ActualizarPermisosRolRequest(List.of("cobros.confirmar")), actorId);

        assertThat(response.permisos()).contains("catalogo.explorar");
    }

    @Test
    void actualizar_claveInexistente_lanzaValidacion() {
        assertThatThrownBy(() -> service.actualizar(
                rolId, new ActualizarPermisosRolRequest(List.of("no.existe")), actorId))
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("no existe");

        verify(configuracionRolRepository, never()).save(any());
    }

    @Test
    void actualizar_rolInexistente_lanzaNoEncontrado() {
        UUID otro = UUID.randomUUID();
        when(rolRepository.findById(otro)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(otro, new ActualizarPermisosRolRequest(List.of()), actorId))
                .isInstanceOf(NoEncontradoException.class);
    }
}
