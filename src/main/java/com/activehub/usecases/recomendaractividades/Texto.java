package com.activehub.usecases.recomendaractividades;

/**
 * Normaliza un texto para comparar: minusculas y sin tildes.
 *
 * <p>Existe porque el termino de una busqueda es texto libre y hay que cruzarlo contra los
 * nombres del catalogo: en castellano, buscar "natacion" tiene que encontrar "Natación", y
 * {@code toLowerCase().contains(...)} compara code points, asi que no lo hace.
 *
 * <p>Es una copia deliberada del mapeo de {@code ActividadSpecifications}, que hace lo mismo
 * pero <b>en SQL</b> (con {@code translate()}) y sobre otra cosa (el nombre de la actividad
 * contra el buscador). Compartir una constante entre una Specification y un usecase seria
 * acoplar el dominio a un slice para ahorrar dos lineas. <b>Si se toca una, mirar la otra.</b>
 */
final class Texto {

    private static final String ACENTOS = "áàäâãéèëêíìïîóòöôõúùüûñçÁÀÄÂÃÉÈËÊÍÌÏÎÓÒÖÔÕÚÙÜÛÑÇ";
    private static final String SIN_ACENTOS = "aaaaaeeeeiiiiooooouuuuncAAAAAEEEEIIIIOOOOOUUUUNC";

    private Texto() {
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String minusculas = texto.trim().toLowerCase();
        StringBuilder salida = new StringBuilder(minusculas.length());
        for (char c : minusculas.toCharArray()) {
            int i = ACENTOS.indexOf(c);
            salida.append(i >= 0 ? SIN_ACENTOS.charAt(i) : c);
        }
        return salida.toString();
    }
}
