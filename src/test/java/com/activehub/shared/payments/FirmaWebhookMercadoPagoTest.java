package com.activehub.shared.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Los valores esperados se calcularon aparte con la receta de la documentacion de Mercado Pago
 * (no con esta clase), asi el test no puede "acertar" copiando un error de la implementacion.
 */
class FirmaWebhookMercadoPagoTest {

    private static final String SECRETO = "secreto-del-webhook-de-prueba";
    private static final String DATA_ID = "ORDTST01M4BS8870WW9PB1GGQPY92B6N";
    private static final String REQUEST_ID = "2d1f2c4a-6b7e-4f3a-9a5b-0c1d2e3f4a5b";
    private static final String TS = "1742505638683";

    /** HMAC-SHA256 de "id:ordtst01m4bs8870ww9pb1ggqpy92b6n;request-id:2d1f…4a5b;ts:1742505638683;". */
    private static final String V1 = "359997898692dba529010469aa0c0474a4b1b84b1d24de56d5447767bae7423e";
    /** El mismo, pero el manifest sin la parte de request-id: "id:ordtst…;ts:1742505638683;". */
    private static final String V1_SIN_REQUEST_ID = "fa1316be3e42ee152e81a20a440aa14547a39fef4be505ce9cfc19f6bccb8082";

    private final FirmaWebhookMercadoPago firma = new FirmaWebhookMercadoPago(SECRETO);

    @Test
    void firmaCorrecta_esValida() {
        assertThat(firma.esValida("ts=" + TS + ",v1=" + V1, REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    void elDataIdSeUsaEnMinusculas() {
        // MP manda el data.id en mayusculas, pero el manifest lo lleva en minusculas.
        assertThat(firma.esValida("ts=" + TS + ",v1=" + V1, REQUEST_ID, DATA_ID.toLowerCase())).isTrue();
    }

    @Test
    void elOrdenDeLasPartesDelHeaderNoImporta() {
        assertThat(firma.esValida("v1=" + V1 + ",ts=" + TS, REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    void sinRequestId_esaParteNoVaEnElManifest() {
        assertThat(firma.esValida("ts=" + TS + ",v1=" + V1_SIN_REQUEST_ID, null, DATA_ID)).isTrue();
    }

    @Test
    void firmaAlterada_noEsValida() {
        String alterada = V1.substring(0, V1.length() - 1) + "0";
        assertThat(firma.esValida("ts=" + TS + ",v1=" + alterada, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    void otraOrden_noEsValida() {
        // Firma valida de OTRA notificacion: no sirve para "aprobar" esta orden.
        assertThat(firma.esValida("ts=" + TS + ",v1=" + V1, REQUEST_ID, "ORDTST01OTRAORDEN")).isFalse();
    }

    @Test
    void otroTimestamp_noEsValida() {
        assertThat(firma.esValida("ts=1742505638684,v1=" + V1, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    void otroSecreto_noEsValida() {
        var conOtroSecreto = new FirmaWebhookMercadoPago("otro-secreto");
        assertThat(conOtroSecreto.esValida("ts=" + TS + ",v1=" + V1, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    void secretoSinConfigurar_rechazaTodo() {
        // Sin secreto no hay forma de verificar: nunca se acepta algo no verificado.
        assertThat(new FirmaWebhookMercadoPago("").esValida("ts=" + TS + ",v1=" + V1, REQUEST_ID, DATA_ID)).isFalse();
        assertThat(new FirmaWebhookMercadoPago(null).esValida("ts=" + TS + ",v1=" + V1, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    void headerAusenteOMalFormado_noEsValida() {
        for (String header : new String[] {null, "", "basura", "ts=" + TS, "v1=" + V1, "ts=,v1=", "ts=" + TS + ",v1="}) {
            assertThat(firma.esValida(header, REQUEST_ID, DATA_ID)).as("header=%s", header).isFalse();
        }
    }
}
