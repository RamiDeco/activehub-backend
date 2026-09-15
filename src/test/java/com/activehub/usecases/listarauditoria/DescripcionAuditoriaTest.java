package com.activehub.usecases.listarauditoria;

import static org.assertj.core.api.Assertions.assertThat;

import com.activehub.shared.audit.AuditAccion;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Reportado: la columna "Detalle" de la trazabilidad mostraba
 * {@code "cb2ce9c9-… · -roles.configurar"}. Es trazable pero ilegible.
 */
class DescripcionAuditoriaTest {

    private static final UUID ROL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Map<String, String> PERMISOS = Map.of(
            "roles.configurar", "Configurar roles y permisos",
            "usuarios.gestionar", "Ver, editar y suspender cuentas");
    private static final Map<UUID, String> ROLES = Map.of(ROL_ID, "ADMIN");

    private String describir(AuditAccion accion, String metadata) {
        return DescripcionAuditoria.describir(accion, "Ana López", metadata, ROL_ID, PERMISOS, ROLES);
    }

    @Test
    void permisosActualizados_nombraElPermisoYElRol() {
        assertThat(describir(AuditAccion.PERMISOS_ACTUALIZADOS, "-roles.configurar"))
                .isEqualTo("Ana López quitó el permiso «Configurar roles y permisos» al rol ADMIN.");
    }

    @Test
    void permisosActualizados_distingueLoQueSeDioDeLoQueSeQuito() {
        String texto = describir(AuditAccion.PERMISOS_ACTUALIZADOS, "+usuarios.gestionar, -roles.configurar");

        assertThat(texto)
                .contains("agregó el permiso «Ver, editar y suspender cuentas»")
                .contains("quitó «Configurar roles y permisos»")
                .contains("al rol ADMIN");
    }

    @Test
    void permisosActualizados_claveDesconocida_usaLaClaveCruda() {
        // Si alguien agrega una clave al codigo y se olvida de la migracion, la descripcion no
        // puede romperse: muestra la clave y sigue.
        assertThat(describir(AuditAccion.PERMISOS_ACTUALIZADOS, "+algo.nuevo")).contains("«algo.nuevo»");
    }

    @Test
    void permisosActualizados_sinCambios_loDice() {
        assertThat(describir(AuditAccion.PERMISOS_ACTUALIZADOS, "sin cambios"))
                .isEqualTo("Ana López guardó los permisos del rol ADMIN sin cambios.");
    }

    @Test
    void accesoDenegado_incluyeLaRutaQueSeIntento() {
        assertThat(describir(AuditAccion.ACCESO_DENEGADO, "GET /api/admin/roles"))
                .isEqualTo("Ana López intentó acceder a algo para lo que no tiene permiso: GET /api/admin/roles.");
    }

    @Test
    void claseCreadaPorLaAgenda_seDistingueDeUnaCreadaAMano() {
        assertThat(describir(AuditAccion.CLASE_CREADA, "AGENDA")).contains("El sistema generó");
        assertThat(describir(AuditAccion.CLASE_CREADA, null)).isEqualTo("Ana López creó una clase.");
    }

    @Test
    void denunciaResuelta_traduceLaAccionDeResolucion() {
        assertThat(describir(AuditAccion.DENUNCIA_RESUELTA, "SUSPENDER")).contains("se suspendió al instructor");
    }

    /**
     * El switch es exhaustivo sobre el enum (no tiene {@code default}), asi que agregar un
     * {@link AuditAccion} nuevo sin describirlo no compila. Este test cubre lo otro: que
     * ninguna rama devuelva algo vacio, y que ninguna filtre un UUID crudo al texto — que era
     * justamente el problema reportado.
     */
    @ParameterizedTest
    @EnumSource(AuditAccion.class)
    void todaAccionTieneUnaDescripcionUtil(AuditAccion accion) {
        String texto = describir(accion, null);

        assertThat(texto).isNotBlank();
        assertThat(texto).doesNotContain(ROL_ID.toString());
        assertThat(texto).endsWith(".");
    }
}
