package com.activehub.domain.actividad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link ActividadSpecifications#conTexto} tiene que ejecutarse de verdad contra Postgres.
 *
 * <p>Los tests de cada usecase son unitarios con mocks, así que la Specification se arma pero
 * nunca se traduce a SQL: un error de sintaxis o una función inexistente pasaría todos esos
 * tests y recién explotaría en el buscador del navegador. Acá importa el {@code translate()}
 * que hace la búsqueda insensible a tildes: es una función de SQL con tres argumentos armada
 * con {@code cb.function}, y lo que se prueba es que la consulta <b>corre</b>.
 *
 * <p>Por eso no se afirma nada sobre las filas devueltas: el contenido de la base de
 * desarrollo cambia, y lo que está bajo prueba es la consulta, no el catálogo.
 *
 * <p>Levanta el contexto completo, así que se conecta a la base configurada en {@code .env}.
 */
@SpringBootTest
class BusquedaSinTildesTest {

    @Autowired
    private ActividadRepository actividadRepository;

    @ParameterizedTest
    @ValueSource(strings = {"natacion", "Natación", "NATACION", "niño", "nino", "formacion tecnica"})
    void conTexto_seEjecutaEnLaBase(String termino) {
        assertThatCode(() -> actividadRepository.findAll(ActividadSpecifications.conTexto(termino)))
                .doesNotThrowAnyException();
    }

    /**
     * Un término en blanco devuelve {@code null}, que es como el resto de las specs de esta
     * clase dicen "no filtres": {@code ListarActividadesService} descarta los nulos al armar
     * la consulta. Si esto devolviera un {@code LIKE '%%'} la búsqueda vacía dejaría afuera
     * las actividades sin nombre y, peor, el patrón se volvería parte del SQL sin necesidad.
     */
    @Test
    void conTexto_enBlanco_noDevuelveSpec() {
        assertThat(ActividadSpecifications.conTexto("  ")).isNull();
        assertThat(ActividadSpecifications.conTexto(null)).isNull();
    }
}
