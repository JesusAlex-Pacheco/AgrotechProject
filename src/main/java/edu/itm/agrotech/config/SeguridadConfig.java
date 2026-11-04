package edu.itm.agrotech.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeguridadConfig {

    /**
     * BCrypt: hash lento y con sal aleatoria, de modo que la clave nunca se
     * guarda en texto plano y dos claves iguales producen hashes distintos.
     * Los servicios dependen de la interfaz PasswordEncoder, no de BCrypt.
     */
    @Bean
    public PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }
}
