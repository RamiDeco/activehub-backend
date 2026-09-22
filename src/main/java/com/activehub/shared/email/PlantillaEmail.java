package com.activehub.shared.email;

import com.activehub.domain.usuario.PropositoVerificacion;

/**
 * El HTML de los mails, con la identidad visual de la app.
 *
 * <h2>Por qué es HTML a mano y no una plantilla Thymeleaf</h2>
 *
 * Un mail no es una página. No hay motor de layout moderno del otro lado: Outlook renderiza
 * con Word, Gmail borra las hojas de estilo y el {@code <head>} entero, y ninguno soporta
 * flexbox ni grid de forma confiable. Por eso esto es deliberadamente "HTML de 2003" —
 * tablas anidadas, ancho fijo, atributos de presentación y **todo el CSS inline**. Agregar
 * Thymeleaf sólo para interpolar tres variables no cambiaría nada de eso y sumaría una
 * dependencia y un directorio de plantillas.
 *
 * <h2>Los colores son los del sistema</h2>
 *
 * Los mismos hex que usa el frontend ({@code lib/style.ts} y el resto de las pantallas):
 * azul {@code #0E2A47}, teal {@code #12B5A5}, naranja {@code #FF6A2B}, grises
 * {@code #54697E}/{@code #7A8C9E}, fondo {@code #F4F7FA}. El logo es el mismo cuadrado con
 * la "A" del header, reconstruido con una celda de tabla porque no se puede depender de que
 * el cliente cargue una imagen remota (Gmail las bloquea hasta que el usuario acepta).
 *
 * <p>El código va en un bloque grande con {@code letter-spacing}: es lo único que el usuario
 * tiene que leer y copiar, así que es lo único que compite por la atención.
 */
public final class PlantillaEmail {

    private static final String AZUL = "#0E2A47";
    private static final String TEAL = "#12B5A5";
    private static final String NARANJA = "#FF6A2B";
    private static final String TEXTO = "#54697E";
    private static final String TENUE = "#7A8C9E";
    private static final String FONDO = "#F4F7FA";
    private static final String BORDE = "#E7EDF3";

    private PlantillaEmail() {
    }

    /** Asunto. El código va también acá: se ve en la bandeja sin abrir el mail. */
    public static String asunto(PropositoVerificacion proposito, String codigo) {
        return switch (proposito) {
            case CAMBIO_EMAIL -> codigo + " — Confirmá tu nuevo correo en ActiveHub";
            case RECUPERACION_PASSWORD -> codigo + " — Recuperá tu contraseña de ActiveHub";
            case REGISTRO -> codigo + " — Tu código de verificación de ActiveHub";
        };
    }

    public static String cuerpo(PropositoVerificacion proposito, String nombre, String codigo, int minutos) {
        // El switch es exhaustivo sobre el enum, sin `default`: sumar un propósito sin darle
        // texto propio no compila, en vez de mandar el mail de alta por error.
        String titulo = switch (proposito) {
            case CAMBIO_EMAIL -> "Confirmá tu nuevo correo";
            case RECUPERACION_PASSWORD -> "Recuperá tu contraseña";
            case REGISTRO -> "Confirmá tu correo";
        };
        String bajada = switch (proposito) {
            case CAMBIO_EMAIL -> "Pediste cambiar el correo de tu cuenta de ActiveHub por esta dirección. "
                    + "Ingresá este código en la app para confirmar el cambio.";
            case RECUPERACION_PASSWORD -> "Pediste recuperar la contraseña de tu cuenta de ActiveHub. "
                    + "Ingresá este código en la app y elegí una contraseña nueva.";
            case REGISTRO -> "¡Bienvenido a ActiveHub! Ingresá este código en la app para activar tu cuenta "
                    + "y empezar a inscribirte a clases.";
        };
        String cierre = switch (proposito) {
            case CAMBIO_EMAIL -> "Hasta que ingreses el código, tu cuenta sigue funcionando con el correo anterior. "
                    + "Si no pediste este cambio, ignorá este mail y avisanos: alguien con acceso a tu "
                    + "cuenta lo intentó.";
            case RECUPERACION_PASSWORD -> "Tu contraseña actual sigue funcionando hasta que ingreses el código y "
                    + "elijas una nueva. Si no pediste recuperarla, ignorá este mail: sin el código nadie "
                    + "puede cambiarla.";
            case REGISTRO -> "Si no creaste ninguna cuenta en ActiveHub, podés ignorar este mail. "
                    + "Sin el código, nadie queda registrado con tu correo.";
        };

        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%1$s</title>
                </head>
                <body style="margin:0;padding:0;background:%7$s;">
                  <!-- Preheader: el resumen que muestra la bandeja al lado del asunto. Oculto en el cuerpo. -->
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;">Tu código es %3$s y vence en %4$d minutos.</div>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%7$s;padding:32px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="560" cellpadding="0" cellspacing="0" border="0" style="width:560px;max-width:100%%;background:#ffffff;border:1px solid %8$s;border-radius:18px;overflow:hidden;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">

                          <tr>
                            <td style="padding:26px 32px 0 32px;">
                              <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                  <td width="40" height="40" align="center" valign="middle" style="width:40px;height:40px;background:%5$s;border-radius:11px;color:#ffffff;font-size:21px;font-weight:bold;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">A</td>
                                  <td style="padding-left:11px;font-size:21px;font-weight:bold;color:%5$s;letter-spacing:-.3px;">Active<span style="color:%6$s;">Hub</span></td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:24px 32px 0 32px;">
                              <h1 style="margin:0 0 10px 0;font-size:24px;line-height:1.25;color:%5$s;letter-spacing:-.5px;">%1$s</h1>
                              <p style="margin:0;font-size:15px;line-height:1.6;color:%9$s;">Hola %2$s, %10$s</p>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:24px 32px 0 32px;">
                              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#F1FBF9;border:1px solid #CBEDE7;border-radius:14px;">
                                <tr>
                                  <td align="center" style="padding:22px 16px;">
                                    <div style="font-size:11px;font-weight:bold;letter-spacing:1.2px;text-transform:uppercase;color:#0C8576;margin-bottom:10px;">Tu código</div>
                                    <div style="font-size:38px;font-weight:bold;letter-spacing:9px;color:%5$s;font-family:'Consolas','Courier New',monospace;">%3$s</div>
                                    <div style="font-size:12.5px;color:%11$s;margin-top:10px;">Vence en %4$d minutos</div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:22px 32px 0 32px;">
                              <p style="margin:0;font-size:13.5px;line-height:1.6;color:%11$s;">%12$s</p>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:24px 32px 26px 32px;">
                              <div style="height:1px;background:#EEF2F6;margin-bottom:16px;"></div>
                              <p style="margin:0;font-size:12px;line-height:1.55;color:%11$s;">
                                Este código es de un solo uso y personal: <strong style="color:%9$s;">no lo compartas con nadie</strong>.
                                ActiveHub nunca te va a pedir tu código por teléfono ni por chat.
                              </p>
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:16px 32px;background:%7$s;border-top:1px solid %8$s;">
                              <p style="margin:0;font-size:11.5px;color:%11$s;">ActiveHub · Actividades deportivas y recreativas en Mendoza</p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .formatted(
                        escapar(titulo),        // %1$s
                        escapar(nombre),        // %2$s
                        escapar(codigo),        // %3$s
                        minutos,                // %4$d
                        AZUL,                   // %5$s
                        NARANJA,                // %6$s
                        FONDO,                  // %7$s
                        BORDE,                  // %8$s
                        TEXTO,                  // %9$s
                        escapar(bajada),        // %10$s
                        TENUE,                  // %11$s
                        escapar(cierre));       // %12$s
    }

    /**
     * El nombre lo escribió el usuario en el registro y termina dentro de un documento HTML.
     * Los DTO ya lo validan con {@code @SinHtml}, pero esto es defensa en profundidad: la
     * validación protege lo que entra por la API y el escapado protege lo que sale al mail.
     */
    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
