package com.activehub.usecases.crearrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.activehub.domain.permiso.ConfiguracionRol;
import com.activehub.domain.permiso.ConfiguracionRolRepository;
import com.activehub.domain.permiso.Permiso;
import com.activehub.domain.permiso.PermisoRepository;
import com.activehub.domain.usuario.Rol;
import com.activehub.domain.usuario.RolRepository;
import com.activehub.shared.audit.AuditAccion;
import com.activehub.shared.audit.AuditService;
import com.activehub.shared.error.RolDuplicadoException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrearRolServiceTest {

    @Mock private RolRepository rolRepository;
    @Mock private PermisoRepository permisoRepository;
    @Mock private ConfiguracionRolRepository configuracionRolRepository;
    @Mock private AuditService auditService;

    private CrearRolService service;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        service = new CrearRolService(rolRepository, permisoRepository, configuracionRolRepository, auditService);
        actorId = UUID.randomUUID();

        when(rolRepository.saveAndFlush(any(Rol.class))).thenAnswer(inv -> {
            Rol rol = inv.getArgument(0);
            ReflectionTestUtils.setField(rol, "id", UUID.randomUUID());
            return rol;
        });
        when(permisoRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of(permiso("a"), permiso("b")));
    }

    private Permiso permiso(String clave) {
        Permiso permiso = new Permiso();
        permiso.setClave(clave);
        ReflectionTestUtils.setField(permiso, "id", UUID.randomUUID());
        return permiso;
    }

    @Test
    void crear_rolNuevo_naceSinPermisosYNoEsDelSistema() {
        var response = service.crear(new CrearRolRequest("Soporte", "Atiende consultas"), actorId);

        assertThat(response.nombre()).isEqualTo("Soporte");
        assertThat(response.sistema()).isFalse();
        assertThat(response.usuarios()).isZero();

        // E4Ad-HU08 criterio 3: se crean las filas de la matriz, todas apagadas.
        ArgumentCaptor<ConfiguracionRol> captor = ArgumentCaptor.forClass(ConfiguracionRol.class);
        verify(configuracionRolRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).noneMatch(ConfiguracionRol::isHabilitado);
    }

    @Test
    void crear_recortaElNombreYAudita() {
        var response = service.crear(new CrearRolRequest("  Soporte  ", null), actorId);

        assertThat(response.nombre()).isEqualTo("Soporte");
        assertThat(response.descripcion()).isNull();
        verify(auditService).registrar(eq(actorId), eq(AuditAccion.ROL_CREADO), eq("Rol"), any(), eq("Soporte"));
    }

    @Test
    void crear_nombreDuplicado_lanzaYNoGuardaNada() {
        when(rolRepository.existsByNombreIgnoreCaseAndDeletedFalse("Soporte")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(new CrearRolRequest("Soporte", null), actorId))
                .isInstanceOf(RolDuplicadoException.class);

        verify(rolRepository, never()).saveAndFlush(any());
        verify(configuracionRolRepository, never()).save(any());
    }
}
