package edu.itm.agrotech.config;

import edu.itm.agrotech.dto.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuracion de la documentacion OpenAPI que publica Swagger UI en
 * /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_ERROR = "ErrorResponse";

    @Bean
    public OpenAPI agrotechOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AgroTech API")
                        .version("1.0.0")
                        .description("""
                                Del campo a tu mesa, sin intermediarios. API que conecta a los \
                                agricultores con los clientes: catalogo de productos, categorias, \
                                registro de ventas y ciclo de vida de los pedidos.

                                Todas las respuestas de error usan el mismo formato: \
                                `fecha`, `estado`, `mensaje` y, en errores de validacion, `errores`.""")
                        .contact(new Contact().name("Jesus Pacheco y Sebastian Galeano - ITM")))
                // Grupos de Swagger UI, en el orden en que se muestran. Los
                // controladores los referencian por nombre con @Tag.
                .tags(List.of(
                        new Tag().name("Productos")
                                .description("Catalogo de productos publicados por los agricultores"),
                        new Tag().name("Categorias")
                                .description("Clasificacion de los productos del catalogo"),
                        new Tag().name("Pedidos")
                                .description("Ventas directas del agricultor al cliente y su ciclo de vida")));
    }

    /**
     * Registra el esquema ErrorResponse y lo asigna a todas las respuestas
     * 4xx y 5xx declaradas en los controladores, para no repetirlo en cada
     * endpoint. Se hace aqui porque springdoc arma los componentes despues
     * de crear el bean OpenAPI.
     */
    @Bean
    public OpenApiCustomizer respuestasDeErrorConFormatoComun() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(ESQUEMA_ERROR,
                    ModelConverters.getInstance().readAllAsResolvedSchema(ErrorResponse.class).schema);

            Schema<?> referencia = new Schema<>().$ref("#/components/schemas/" + ESQUEMA_ERROR);

            openApi.getPaths().values().forEach(ruta ->
                    ruta.readOperations().forEach(operacion ->
                            operacion.getResponses().forEach((codigo, respuesta) -> {
                                if (codigo.startsWith("4") || codigo.startsWith("5")) {
                                    respuesta.setContent(new Content().addMediaType(
                                            "application/json", new MediaType().schema(referencia)));
                                }
                            })));
        };
    }
}
