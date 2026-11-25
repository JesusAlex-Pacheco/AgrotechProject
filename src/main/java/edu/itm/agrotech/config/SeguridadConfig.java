package edu.itm.agrotech.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import edu.itm.agrotech.security.ErroresDeSeguridad;
import edu.itm.agrotech.security.ServicioTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Seguridad de la API.
 *
 * <p>Al iniciar sesion (POST /api/sesiones) el usuario recibe un token JWT
 * firmado con su id, su rol y el id de su perfil. Las rutas que modifican
 * datos exigen ese token en la cabecera {@code Authorization: Bearer ...};
 * sin el, o con uno vencido o alterado, responden 401. Con token pero sin
 * el rol que exige la ruta, responden 403.
 *
 * <p>Las reglas de esta clase solo miran el rol. Que el recurso sea del
 * usuario (su producto, su pedido) lo verifica ControlAcceso en cada controlador.
 */
@Configuration
public class SeguridadConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SeguridadConfig.class);
    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private static final String AGRICULTOR = "AGRICULTOR";
    private static final String CLIENTE = "CLIENTE";
    private static final String ADMIN = "ADMIN";

    /**
     * BCrypt: hash lento y con sal aleatoria, de modo que la clave nunca se
     * guarda en texto plano y dos claves iguales producen hashes distintos.
     * Los servicios dependen de la interfaz PasswordEncoder, no de BCrypt.
     */
    @Bean
    public PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http,
                                                 JwtAuthenticationConverter convertidorDeRoles,
                                                 ErroresDeSeguridad errores) throws Exception {
        http
                // API sin estado: no hay sesion de servidor ni cookies, asi que
                // no aplica CSRF; cada solicitud trae su propio token.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(rutas -> rutas
                        // Documentacion y pagina de error
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error")
                        .permitAll()

                        // Publico: iniciar sesion y registrarse. El registro solo crea
                        // clientes, agricultores y transportadores; nunca un ADMIN.
                        .requestMatchers(HttpMethod.POST,
                                "/api/sesiones", "/api/clientes", "/api/agricultores", "/api/transportadores")
                        .permitAll()

                        // Publico: navegar el catalogo con sus fotos, las categorias, las
                        // resenas y el perfil del agricultor, sin haber iniciado sesion.
                        .requestMatchers(HttpMethod.GET,
                                "/api/productos/**", "/api/categorias/**", "/api/agricultores/*", "/api/fotos/**")
                        .permitAll()

                        // Categorias: las gestionan los agricultores y el administrador
                        .requestMatchers("/api/categorias/**").hasAnyRole(AGRICULTOR, ADMIN)

                        // Resenar: solo un cliente (que el producto lo haya recibido lo valida el servicio)
                        .requestMatchers(HttpMethod.POST, "/api/productos/*/resenas").hasRole(CLIENTE)

                        // Publicar, editar y retirar productos
                        .requestMatchers("/api/productos/**").hasAnyRole(AGRICULTOR, ADMIN)

                        // Comprar: solo un cliente
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole(CLIENTE)

                        // Logistica: programar entregas y elegir el transportador
                        .requestMatchers(HttpMethod.POST, "/api/pedidos/*/entrega").hasAnyRole(AGRICULTOR, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/transportadores").hasAnyRole(AGRICULTOR, ADMIN)

                        // Lo demas (consultar pedidos, cambiar su estado, agendas...) exige
                        // sesion; ControlAcceso decide si el recurso es del usuario.
                        .anyRequest().authenticated())

                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeRoles))
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores));

        return http.build();
    }

    // ---------------------------------------------------------------
    // Tokens JWT
    // ---------------------------------------------------------------

    /**
     * Clave HMAC con la que se firman y verifican los tokens. Se lee de
     * JWT_SECRET; si no esta definida se genera una aleatoria, que nunca se
     * escribe en el codigo ni en el log.
     */
    @Bean
    public SecretKey claveDeFirma(@Value("${agrotech.seguridad.jwt-secreto:}") String secreto) {
        if (secreto == null || secreto.isBlank()) {
            LOG.warn("JWT_SECRET no esta definida: se usa una clave aleatoria y las sesiones "
                    + "abiertas se cerraran al reiniciar el backend");
            byte[] aleatoria = new byte[LONGITUD_MINIMA_SECRETO];
            new SecureRandom().nextBytes(aleatoria);
            return new SecretKeySpec(aleatoria, "HmacSHA256");
        }

        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder codificadorDeTokens(SecretKey claveDeFirma) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveDeFirma));
    }

    /** Verifica la firma, la fecha de vencimiento y que el token lo haya emitido AgroTech. */
    @Bean
    public JwtDecoder decodificadorDeTokens(SecretKey claveDeFirma) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveDeFirma)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(ServicioTokens.EMISOR));
        return decodificador;
    }

    /** El claim "rol" del token se convierte en la autoridad ROLE_<rol> que usan las reglas. */
    @Bean
    public JwtAuthenticationConverter convertidorDeRoles() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(ServicioTokens.CLAIM_ROL);
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }
}
