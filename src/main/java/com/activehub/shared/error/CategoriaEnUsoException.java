package com.activehub.shared.error;

public class CategoriaEnUsoException extends ApiException {

    public CategoriaEnUsoException() {
        super(ApiErrorCode.CATEGORIA_EN_USO, "No se puede eliminar: hay tipos de actividad que usan esta categoría.");
    }

    /**
     * Mensaje propio para el caso que sí bloquea de verdad: alguno de los tipos de la categoría
     * tiene actividades publicadas. El nombre del tipo va en el texto porque, con varios tipos
     * colgando, "está en uso" no le dice al administrador cuál tiene que vaciar primero.
     */
    public CategoriaEnUsoException(String mensaje) {
        super(ApiErrorCode.CATEGORIA_EN_USO, mensaje);
    }
}
