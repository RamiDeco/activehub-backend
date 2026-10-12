package com.activehub.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * El catálogo público tiene que responder SIN sesión.
 *
 * <p>Los {@code @WebMvcTest} de cada slice corren con {@code addFilters = false}, así que no
 * ven la cadena de seguridad: un endpoint que se olvidó de la lista {@code permitAll} de
 * {@link SecurityConfig} pasa todos sus tests y recién falla en el navegador. Pasó al sumar
 * {@code /api/niveles-intensidad} (E4Ad-HU05): el frontend lo pide en el mismo
 * {@code Promise.all} que categorías, tipos y actividades al arrancar, antes de que haya
 * sesión — y como {@code Promise.all} se rechaza entero si una sola promesa falla, ese 401
 * dejaba la landing en estado de error, con el catálogo entero invisible para cualquier
 * visitante.
 *
 * <p>Por eso la aserción es "no es 401/403", y no "es 200": lo que se está probando es la
 * guarda, no el contenido. Un 404 por id inexistente sería una respuesta perfectamente
 * válida para esta prueba.
 *
 * <p>Este test levanta el contexto completo (es la única forma de ejercitar la cadena de
 * filtros) y por lo tanto se conecta a la base configurada en {@code .env}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogoPublicoTest {

    @Autowired
    private MockMvcTester mvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/categorias",
            "/api/tipos-actividad",
            "/api/niveles-intensidad",
            "/api/actividades",
    })
    void catalogo_respondeSinSesion(String ruta) {
        int status = mvc.get().uri(ruta).exchange().getResponse().getStatus();

        assertThat(status)
                .as("%s lo pide el frontend antes del login: no puede exigir sesión", ruta)
                .isNotIn(401, 403);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/me",
            "/api/alumno/favoritos",
            "/api/admin/roles",
            "/api/admin/niveles-intensidad",
            "/api/instructor/mercadopago",
            "/api/instructor/mercadopago/vinculo",
    })
    void loQueNoEsCatalogo_sigueExigiendoSesion(String ruta) {
        // La contraparte: que la lista de permitAll no se haya ido de mano.
        assertThat(mvc.get().uri(ruta).exchange())
                .as("%s no es catálogo público", ruta)
                .hasStatus(401);
    }

    @Test
    void callbackDeMercadoPago_respondeSinSesionPeroRechazaUnStateAjeno() {
        // Lo llama el navegador al volver de MP, sin JWT: si exigiera sesión el OAuth no podría
        // terminar nunca. Sin un state firmado por nosotros tiene que ser 400, no 401 ni 302.
        assertThat(mvc.get().uri("/api/instructor/mercadopago/callback?code=TG-x&state=inventado").exchange())
                .hasStatus(400);
    }

    @Test
    void webhookDeMercadoPago_respondeSinSesionPeroRechazaUnaFirmaAjena() {
        // Lo llama Mercado Pago, sin JWT: si exigiera sesion nunca llegaria una notificacion.
        // Sin una x-signature valida tiene que ser el 401 del webhook, SIN cuerpo; el 401 de la
        // cadena de seguridad (falta de sesion) trae un ApiError en el cuerpo.
        var respuesta = mvc.post().uri("/api/instructor/mercadopago/webhook?data.id=ORDTST01X&type=order")
                .header("x-signature", "ts=1,v1=inventada").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{}").exchange();
        assertThat(respuesta).hasStatus(401);
        assertThat(respuesta.getResponse().getContentLength()).isZero();
    }
}
