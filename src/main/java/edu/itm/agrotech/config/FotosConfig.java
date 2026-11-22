package edu.itm.agrotech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Publica la carpeta de fotos en /api/fotos/**. Cada foto subida tiene un
 * nombre unico, asi que el navegador puede guardarla en cache sin riesgo de
 * mostrar una version vieja.
 */
@Configuration
public class FotosConfig implements WebMvcConfigurer {

    private final String ubicacion;

    public FotosConfig(@Value("${agrotech.fotos.directorio:fotos}") String directorio) {
        String uri = Path.of(directorio).toAbsolutePath().normalize().toUri().toString();
        // Spring exige que una carpeta termine en "/"
        this.ubicacion = uri.endsWith("/") ? uri : uri + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/api/fotos/**")
                .addResourceLocations(ubicacion)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
