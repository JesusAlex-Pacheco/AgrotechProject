package edu.itm.agrotech.security;

import edu.itm.agrotech.config.SeguridadConfig;
import edu.itm.agrotech.domain.Rol;
import edu.itm.agrotech.domain.SesionUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Usa el codificador y el decodificador reales de SeguridadConfig, sin levantar Spring. */
class ServicioTokensTest {

    private final SeguridadConfig config = new SeguridadConfig();
    private final SecretKey clave = config.claveDeFirma("una-clave-de-prueba-de-al-menos-32-caracteres");
    private final JwtDecoder decodificador = config.decodificadorDeTokens(clave);

    private ServicioTokens servicio(Duration duracion) {
        return new ServicioTokens(config.codificadorDeTokens(clave), duracion);
    }

    @Test
    @DisplayName("El token lleva el usuario, el rol y el perfil, y el decodificador lo acepta")
    void emiteYVerifica() {
        ServicioTokens.TokenEmitido token = servicio(Duration.ofHours(8))
                .emitir(new SesionUsuario(7L, "Marta", "marta@agrotech.co", Rol.CLIENTE, 3L));

        Jwt jwt = decodificador.decode(token.valor());

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("CLIENTE");
        assertThat(((Number) jwt.getClaim("idPerfil")).longValue()).isEqualTo(3L);
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expira());
    }

    @Test
    @DisplayName("El ADMIN no tiene perfil: el token no lleva idPerfil")
    void adminSinPerfil() {
        ServicioTokens.TokenEmitido token = servicio(Duration.ofHours(8))
                .emitir(new SesionUsuario(1L, "Admin", "admin@agrotech.co", Rol.ADMIN, null));

        assertThat(decodificador.decode(token.valor()).hasClaim("idPerfil")).isFalse();
    }

    @Test
    @DisplayName("Rechaza un token firmado con otra clave (alterado o falsificado)")
    void rechazaOtraFirma() {
        SecretKey otraClave = config.claveDeFirma("");   // aleatoria
        String falsificado = new ServicioTokens(config.codificadorDeTokens(otraClave), Duration.ofHours(8))
                .emitir(new SesionUsuario(1L, "Intruso", "x@x.co", Rol.ADMIN, null))
                .valor();

        assertThatThrownBy(() -> decodificador.decode(falsificado)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Rechaza un token vencido")
    void rechazaVencido() {
        // Vencido hace 2 minutos: mas que el margen de 60 s que tolera el validador
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ServicioTokens.EMISOR)
                .subject("7")
                .claim("rol", "CLIENTE")
                .issuedAt(ahora.minus(Duration.ofHours(8)))
                .expiresAt(ahora.minus(Duration.ofMinutes(2)))
                .build();
        String vencido = config.codificadorDeTokens(clave)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decodificador.decode(vencido)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Un JWT_SECRET corto se rechaza al arrancar")
    void secretoCorto() {
        assertThatThrownBy(() -> config.claveDeFirma("corta"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
