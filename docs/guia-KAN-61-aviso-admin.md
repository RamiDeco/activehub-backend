# KAN-61 — Lo que queda para vos: avisar a la administración cuando un cobro no entra

> **Estado: HECHO** (2026-10-10, bitácora ENTRADA 024). Se conserva como registro de cómo se
> armó. Sigue pendiente solo la prueba en sandbox del final.

## Contexto en 30 segundos

`FinalizarClasesVencidasService.capturarPagosPendientes()` intenta cobrar en Mercado Pago cada
5 minutos los pagos `Retenido` de clases finalizadas (o con la autorización por vencer). Si
falla, `registrarFalla(...)` ya hace todo esto:

- suma 1 a `pago.intentosCaptura` (columna nueva, V32) y lo guarda;
- audita `PAGO_CAPTURA_FALLIDA` con `"intento N · TipoDeExcepcion"`;
- cuando `intento == VentanaPagos.MAX_INTENTOS_CAPTURA` (6, o sea media hora fallando) llama a
  **`avisarAdministracion(pago)`**, que hoy **solo loguea**.

Tu tarea: que `avisarAdministracion` le mande una notificación a cada administrador. Es el
punto 4 del ticket ("si falla N veces, notificar a un admin"). Sin esto, un pago que no puede
cobrarse (por ejemplo, porque el instructor desvinculó su cuenta de MP) queda en un `log.error`
que nadie lee, y la autorización vence sola a los 5 días.

Tiempo estimado: 1 a 2 horas. Toca 4 archivos y un test que ya está escrito.

## Paso 1 — Elegir qué permiso define "administración"

No se pregunta por el rol ADMIN (RN-19, y `GuardasDePermisoTest` prohíbe `hasRole`). Hay que
elegir una de las 18 claves de permiso. Candidatas:

| Clave | A favor | En contra |
|---|---|---|
| `auditoria.ver` | quien la tiene puede ir a Auditoría y ver las `PAGO_CAPTURA_FALLIDA` | no es una clave "de pagos" |
| `denuncias.resolver` | ya es quien decide reintegros | mezcla dos temas |
| clave nueva `pagos.supervisar` | la más prolija | hay que crear una migración, sumarla a la pantalla de roles y conseguir que `GuardasDePermisoTest` pase (toda clave tiene que gatear algún endpoint, y hoy no habría ninguno) |

**Recomendación: `auditoria.ver`.** Es la que menos toca y la que lleva a una pantalla donde se
ve el problema. Si elegís otra, cambiá el string en el test del paso 5.

## Paso 2 — Una consulta que devuelva los usuarios con ese permiso

Ya existe la mitad: `ConfiguracionRolRepository.rolesConPermiso(String clave)` devuelve los
ids de los roles que tienen la clave encendida (lo usa `ListarUsuariosAdminService` para
calcular `puedeDarClases`, si querés ver un ejemplo).

Te falta la otra mitad en `domain/usuario/UsuarioRepository.java`:

```java
/** KAN-61: ids de las cuentas vivas y activas con alguno de esos roles (avisos a la administración). */
@Query("SELECT u.id FROM Usuario u WHERE u.rol.id IN :rolIds AND u.deleted = false "
        + "AND u.estado = com.activehub.domain.usuario.EstadoUsuario.ACTIVO")
List<UUID> findIdsActivosPorRol(List<UUID> rolIds);
```

Ojo con dos cosas:
- **Si `rolIds` está vacía, no llames a la consulta.** `IN ()` con una lista vacía da error en
  algunos motores; cortá antes con un `if (roles.isEmpty()) return;`.
- `u.deleted = false` es redundante con el `@SQLRestriction` de `BaseEntity`, pero el resto del
  repositorio lo escribe igual. Seguí el estilo.

## Paso 3 — Un tipo de notificación nuevo

En `shared/notificacion/TipoNotificacion.java`, agregá un bloque para el administrador:

```java
    // Administrador
    CAPTURA_PAGO_FALLIDA
```

No necesita migración: la columna `tipo` no tiene CHECK (está explicado en el javadoc del enum).

Del lado del frontend, el tipo nuevo cae en el ícono por defecto. Si querés uno propio, está en
`activehub-frontend/src/lib/notificaciones.ts`. Es opcional.

## Paso 4 — Implementar `avisarAdministracion`

En `usecases/finalizarclasesvencidas/FinalizarClasesVencidasService.java`:

1. Inyectá por constructor `ConfiguracionRolRepository`, `UsuarioRepository` y
   `NotificacionService`. Son de `domain/` y de `shared/`, así que se puede: no es inyectar el
   Service de otro usecase.
2. Reemplazá el cuerpo del método:

```java
private void avisarAdministracion(Pago pago) {
    log.error(...); // dejá el log: el aviso puede no tener destinatarios

    List<UUID> roles = configuracionRolRepository.rolesConPermiso("auditoria.ver");
    if (roles.isEmpty()) {
        return;
    }
    var clase = pago.getInscripcion().getClase();
    String mensaje = "No se pudo cobrar en Mercado Pago un pago de la clase de "
            + clase.getActividad().getNombre() + " del "
            + NotificacionMensajes.formatFechaHora(clase.getFechaHora())
            + " después de " + VentanaPagos.MAX_INTENTOS_CAPTURA + " intentos. "
            + "Revisá que el instructor tenga su cuenta de Mercado Pago vinculada.";
    for (UUID adminId : usuarioRepository.findIdsActivosPorRol(roles)) {
        notificacionService.notificar(adminId, TipoNotificacion.CAPTURA_PAGO_FALLIDA, mensaje,
                pago.getId(), Destino.ninguno());
    }
}
```

¿Por qué esas decisiones? (las reglas salen del CLAUDE.md):
- **El mensaje lleva actividad y fecha/hora con `NotificacionMensajes.formatFechaHora`.** Es
  regla de negocio para toda notificación sobre una clase.
- **`Destino.ninguno()`**: el admin no tiene una pantalla de pagos a la que mandarlo, y un click
  que rebota al home es peor que ninguno. Si preferís, `Destino.clase(clase.getId())` también
  es defendible. Pero esa pantalla es la del roster del instructor y el admin no la puede abrir:
  verificalo antes de elegirla.
- **`entidadId` = el id del pago**: es la trazabilidad, coincide con el `entidadId` de la fila
  `PAGO_CAPTURA_FALLIDA` de Auditoría.
- **No metas el nombre del alumno ni el monto** si no hace falta: la notificación queda guardada
  en la base.

3. Arreglá el `setUp` de `FinalizarClasesVencidasServiceTest`, que construye el Service a mano (el constructor cambió). Agregá los tres `@Mock` y pasalos al `new`.

## Paso 5 — Activar el test que ya está escrito

En `FinalizarClasesVencidasServiceTest`, buscá
`capturarPagosPendientes_alLlegarAlTopeDeIntentos_avisaALaAdministracion`:

1. Sacale el `@Disabled`.
2. Descomentá los `when(...)` y los `verify(...)`.
3. Borrá la línea `Assertions.fail(...)` del final.
4. Corré solo esa clase:
   ```
   JAVA_HOME="$HOME/.jdks/ms-21.0.7" ./mvnw -q test -Dtest=FinalizarClasesVencidasServiceTest
   ```

Después sumá vos dos tests más. Siguen el mismo molde, así que son cortos:
- **`noVuelveAAvisarDespuesDelTope`**: con `intentosCaptura = MAX_INTENTOS_CAPTURA` (ya avisado),
  otra falla no notifica (`verify(notificacionService, never()).notificar(...)`). Es lo que
  garantiza el `==` en vez de `>=`.
- **`sinRolesConElPermiso_noFalla`**: `rolesConPermiso` devuelve lista vacía → no se llama a
  `findIdsActivosPorRol` ni a `notificar`, y no explota.

## Paso 6 — Cerrar

1. Suite completa (los `@SpringBootTest` van contra Supabase):
   ```
   JAVA_HOME="$HOME/.jdks/ms-21.0.7" ./mvnw test
   ```
   Esperado: 970 pruebas (las 968 de hoy, que ya cuentan la deshabilitada, más tus 2 nuevas)
   y 2 omitidas (las del sandbox).
2. Agregá una entrada a `docs/BITACORA-INFORME.txt` **en los dos repos**: el texto literal del
   mensaje de la notificación, el permiso elegido y por qué, y el número de pruebas que dé el
   comando.

## El otro pendiente del ticket: la prueba en sandbox (criterio 4)

Esto no es código, es la verificación manual. Los cuatro criterios automatizados ya están:

1. Inscribite con Mercado Pago a una clase de un instructor con cuenta de prueba vinculada.
2. Forzá la clase a pasada: en Supabase, `UPDATE clase SET fecha_hora = now() - interval '2 hours' WHERE id = '...'`.
3. Esperá la corrida de `FinalizarClasesVencidasScheduler` (5 min, o reiniciá el backend: corre al arrancar).
4. Verificá que `pago.capturado_at` tenga fecha y que en la cuenta del **vendedor** de prueba
   el pago figure **acreditado**. Para que pase a `Liberado`, poné la clase 25 h atrás y esperá
   `LiberarPagosScheduler` (15 min).
5. Comisión: hoy `pago.comision` es siempre 0 (decisión de MP-01, la plataforma no cobra
   comisión), así que en el panel no va a haber descuento de la plataforma, solo el de MP.
   Anotalo así en la bitácora: el criterio del ticket asumía una comisión que se decidió no cobrar.
