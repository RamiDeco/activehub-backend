package com.activehub.domain.penalizacion;

/**
 * Umbrales de negocio de las penalizaciones.
 *
 * <p>El minimo de 15 dias para una Suspension temporal lo fijo el usuario: una suspension
 * mas corta no cambia nada en la practica (el instructor pierde a lo sumo una clase) y
 * termina siendo un tiron de orejas sin efecto. Vale tanto para el alta manual del admin
 * como para la suspension que sale de resolver una denuncia.
 */
public final class VentanaPenalizacion {

    public static final int MINIMO_DIAS_SUSPENSION = 15;

    private VentanaPenalizacion() {
    }
}
