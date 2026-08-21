package com.activehub.shared.error;

public class CategoriaEnUsoException extends ApiException {

    public CategoriaEnUsoException() {
        super(ApiErrorCode.CATEGORIA_EN_USO, "No se puede eliminar: hay tipos de actividad que usan esta categoría.");
    }
}
