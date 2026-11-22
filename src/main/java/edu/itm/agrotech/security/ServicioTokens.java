package edu.itm.agrotech.security;

import edu.itm.agrotech.domain.SesionUsuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Emite el token de sesion (JWT firmado con HMAC-SHA256) que el front end
 * envia en cada solicitud como {@code Authorization: Bearer <token>}.
 * El token lleva el id del usuario, su rol y el id de su perfil; como esta
 * firmado, el cliente no puede cambiarlos sin invalidarlo.
 */
@Component
public class ServicioTokens {

    public static final String EMISOR = "agrotech";
    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_ID_PERFIL = "idPerfil";

    private final JwtEncoder codificador;
    private final Duration duracion;

    public ServicioTokens(JwtEncoder codificador,
                          @Value("${agrotech.seguridad.duracion-sesion:8h}") Duration duracion) {
        this.codificador = codificador;
        this.duracion = duracion;
    }

    public TokenEmitido emitir(SesionUsuario sesion) {
        // El JWT guarda las fechas en segundos: se truncan para que "expira"
        // en la respuesta coincida exactamente con el vencimiento del token.
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expira = ahora.plus(duracion);

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(String.valueOf(sesion.idUsuario()))
                .claim("nombre", sesion.nombre())
                .claim(CLAIM_ROL, sesion.rol().name());
        if (sesion.idPerfil() != null) {
            claims.claim(CLAIM_ID_PERFIL, sesion.idPerfil());
        }

        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificador.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
        return new TokenEmitido(valor, expira);
    }

    /** Token firmado y el momento en que deja de ser valido. */
    public record TokenEmitido(String valor, Instant expira) {
    }
}
