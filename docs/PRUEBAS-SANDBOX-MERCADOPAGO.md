# Pruebas manuales de Mercado Pago en sandbox

Matriz de KAN-64 (MP-15), más las verificaciones que quedaron pendientes en KAN-62, KAN-63 y KAN-66. Las pruebas automatizadas usan Mercado Pago simulado; esto es lo único que comprueba que la plata se mueve de verdad en MP.

**Regla de la bitácora:** un caso se marca *Probado* solo si se ejecutó, con la fecha y la entrada de `docs/BITACORA-INFORME.txt` que lo registra. Al probar un caso, actualizá esta tabla y agregá la entrada.

## Antes de empezar

- Backend y frontend corriendo, con las variables de Mercado Pago cargadas (ver `.env.example`). Después de cambiar código, **reiniciá el backend**.
- Cuentas de prueba de MP: **vendedor** (la vincula el instructor), **comprador** (su correo va en `MP_SANDBOX_PAYER_EMAIL`) y la **aplicación** creada por un usuario de prueba "Integrador".
- Un instructor de ActiveHub con la cuenta de MP vinculada, desde la tarjeta "Mercado Pago" de su Panel.
- Tarjetas de prueba de la documentación de MP. **El titular define el resultado**: `APRO` aprobado, `OTHE` rechazado, `CONT` pendiente.
- **Para simular que pasó el tiempo,** se corre la fecha de la clase en Supabase:
  - `UPDATE clase SET fecha_hora = now() - interval '2 hours' WHERE id = '<clase>';` → la clase ya empezó. El proceso automático, cada 5 min o al reiniciar, la finaliza y cobra.
  - `... interval '25 hours' ...` → pasó el período de denuncias. El proceso de liberación corre cada 15 min.
- Consulta para ver el estado de un pago:
  ```sql
  SELECT c.estado AS clase, i.estado AS inscripcion, p.estado AS pago, p.capturado_at, p.intentos_captura, p.status_detail
  FROM clase c JOIN inscripcion i ON i.clase_id = c.id JOIN pago p ON p.inscripcion_id = i.id
  WHERE c.id = '<clase>';
  ```
- La clase tiene que empezar **dentro de los próximos 4 días**: si falta más, es preinscripción y no se paga.

## Matriz

| # | Caso | Resultado esperado | Estado |
|---|---|---|---|
| 1 | El instructor vincula su cuenta → el alumno paga con `APRO` | Pago `Retenido` en ActiveHub; orden **autorizada, no acreditada** en MP | **Probado** (ENTRADA 019, KAN-58) |
| 2 | La clase finaliza → corre el proceso | `capturado_at` con fecha y pago **acreditado** en la cuenta del vendedor; a las 24 h sin denuncias, `Liberado`. Comisión: no aplica (la plataforma cobra 0) | **Parcial**: captura probada (ENTRADA 025); el paso a `Liberado` no se ejecutó |
| 3 | El alumno paga con `OTHE` | Rechazado: mensaje "Tu banco rechazó el pago. Probá con otra tarjeta.", sin inscripción y con el cupo intacto | Pendiente |
| 4 | El alumno paga con `CONT` | Pago `EnRevision`, inscripción `PagoPendiente`; cuando MP decide, el webhook la confirma o la cancela y el alumno recibe la notificación | Pendiente |
| 5 | Pago retenido sin cobrar → el instructor cancela la clase | Pago `Cancelado` sin `capturado_at`; autorización **cancelada** en MP; el alumno recibe "Se liberó la reserva del pago en tu tarjeta: no se te va a cobrar." (cierra **KAN-62**) | Pendiente |
| 6 | Pago retenido sin cobrar → el alumno cancela su inscripción | Lo mismo que el caso 5, desde "Mis clases" del alumno | Pendiente |
| 7 | Pago capturado → denuncia resuelta con Reintegrar | Pago `Reembolsado`; **reembolso** visible en MP | **Probado** (ENTRADA 025) |
| 7b | Repetir el 7 con la corrección de KAN-66 | **Una sola** fila "Se reembolsó en Mercado Pago un pago ya cobrado." en Auditoría, con el admin como actor (cierra **KAN-66**) | Pendiente |
| 8 | El instructor notifica su ausencia | Todos los inscriptos con su pago devuelto y la notificación con el detalle del pago | Pendiente |
| 9 | Clase de un instructor **sin** cuenta vinculada | El checkout solo ofrece Efectivo; forzar Mercado Pago responde 409 | Pendiente |
| 10 | Webhook con firma inválida | Respuesta 401 y nada procesado (se puede probar con la simulación del panel de MP sin la clave correcta) | Pendiente (cubierto por pruebas automatizadas) |
| 11 | Doble clic en "Pagar" | Un solo cobro (la clave de idempotencia evita la segunda orden) | Pendiente |
| 12 | Pago en efectivo | `PagoPendiente` → el instructor confirma el cobro → `Inscripto`, igual que antes | Pendiente |
| 13 | "Mis pagos" del alumno con los datos de estas pruebas | Cada estado con su etiqueta y su frase: "Retenido hasta la clase", "Cobrado", "Reembolsado", "Retención liberada"… (cierra **KAN-63**) | Pendiente |
| 14 | Detalle de una clase paga con MP, visto por el instructor | Columnas "Pago" y "A recibir" con el neto correcto (hoy, el precio) y el total al pie (cierra **KAN-63**) | Pendiente |
| 15 | El instructor desvincula su cuenta sin pagos retenidos ni en revisión (botón "Desvincular" del Panel → "Sí, desvincular") | La tarjeta vuelve a mostrar "Vincular Mercado Pago"; en la base la cuenta queda `DESVINCULADA` sin tokens; en Auditoría, "<actor> desvinculó su cuenta de Mercado Pago." (cierra **KAN-67**) | Pendiente |
| 16 | Lo mismo, con un pago `Retenido` de un alumno | El diálogo muestra "No podés desvincular tu cuenta de Mercado Pago todavía: hay 1 pago de un alumno…" y la tarjeta sigue igual (cierra **KAN-67**) | Pendiente |
| 17 | Cuenta en `TOKEN_VENCIDO` (forzar en la base `expires_at` en el pasado y que MP rechace la renovación, o poner el estado a mano) | La tarjeta ofrece "Volver a vincular" y "Desvincular"; la notificación de vinculación vencida se ve en la campana con el ícono de alerta y sin link (cierra **KAN-67**; si se fuerza `expires_at` con un refresh token válido, el job lo renueva y cierra el criterio 4 de **KAN-55**) | Pendiente |

## Al terminar

1. Actualizar esta tabla y agregar una entrada en la bitácora (los dos repos) con lo que se vio en cada caso.
2. Pasar a Finalizada en Jira las tareas que cierra cada caso: KAN-62 (5), KAN-63 (13 y 14), KAN-66 (7b).
3. KAN-64 se cierra cuando estén los 12 casos de la matriz original y las subtareas MP-01 a MP-14 estén en Listo (falta KAN-55). Después se mueve KAN-2 a Listo.
