package edu.itm.agrotech.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoPedidoTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "CREADO, PAGADO",
            "CREADO, EN_RUTA",
            "CREADO, CANCELADO",
            "PAGADO, EN_RUTA",
            "PAGADO, CANCELADO",
            "EN_RUTA, ENTREGADO"
    })
    @DisplayName("Permite las transiciones del ciclo de vida")
    void permiteTransicionesValidas(EstadoPedido desde, EstadoPedido hacia) {
        assertThat(desde.puedeCambiarA(hacia)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "CREADO, ENTREGADO",
            "PAGADO, CREADO",
            "EN_RUTA, CANCELADO",
            "EN_RUTA, PAGADO",
            "ENTREGADO, CANCELADO",
            "CANCELADO, CREADO"
    })
    @DisplayName("Rechaza los saltos que no estan en el ciclo de vida")
    void rechazaTransicionesInvalidas(EstadoPedido desde, EstadoPedido hacia) {
        assertThat(desde.puedeCambiarA(hacia)).isFalse();
    }

    @Test
    @DisplayName("ENTREGADO y CANCELADO son estados finales")
    void estadosFinales() {
        for (EstadoPedido destino : EstadoPedido.values()) {
            assertThat(EstadoPedido.ENTREGADO.puedeCambiarA(destino)).isFalse();
            assertThat(EstadoPedido.CANCELADO.puedeCambiarA(destino)).isFalse();
        }
    }
}
