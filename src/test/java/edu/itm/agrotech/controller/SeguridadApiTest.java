package edu.itm.agrotech.controller;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import edu.itm.agrotech.config.SeguridadConfig;
import edu.itm.agrotech.domain.Categoria;
import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.domain.Rol;
import edu.itm.agrotech.domain.SesionUsuario;
import edu.itm.agrotech.repository.EntregaRepository;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import edu.itm.agrotech.security.ControlAcceso;
import edu.itm.agrotech.security.ErroresDeSeguridad;
import edu.itm.agrotech.security.ServicioTokens;
import edu.itm.agrotech.service.CategoriaService;
import edu.itm.agrotech.service.FotoProductoService;
import edu.itm.agrotech.service.PedidoService;
import edu.itm.agrotech.service.ProductoService;
import edu.itm.agrotech.service.ResenaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la seguridad de la API a traves de HTTP, como la veria el front end
 * o alguien que llame la API directamente: sin token responde 401, con el rol
 * equivocado o a nombre de otro responde 403, y con el permiso correcto pasa.
 *
 * <p>Los tokens se emiten con el ServicioTokens real, asi que se prueba toda la
 * cadena (firma, vencimiento, rol y ControlAcceso). Los servicios son simulados:
 * no hace falta la base de datos.
 */
@WebMvcTest(controllers = {
        ProductoController.class, CategoriaController.class, PedidoController.class, ResenaController.class})
@Import({SeguridadConfig.class, ServicioTokens.class, ControlAcceso.class, ErroresDeSeguridad.class})
class SeguridadApiTest {

    private static final long ID_AGRICULTOR = 7L;
    private static final long ID_CLIENTE = 5L;

    private static final String PRODUCTO_VALIDO = """
            {"idAgricultor": %d, "idCategoria": 1, "nombre": "Mango de azucar",
             "precio": 4200, "unidadMedida": "kg", "stock": 50}""";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ServicioTokens servicioTokens;
    @Autowired
    private JwtEncoder codificador;

    @MockitoBean
    private ProductoService productoService;
    @MockitoBean
    private FotoProductoService fotoProductoService;
    @MockitoBean
    private CategoriaService categoriaService;
    @MockitoBean
    private PedidoService pedidoService;
    @MockitoBean
    private ResenaService resenaService;
    // Los usa ControlAcceso para saber de quien es cada producto o pedido
    @MockitoBean
    private PedidoRepository pedidoRepository;
    @MockitoBean
    private ProductoRepository productoRepository;
    @MockitoBean
    private EntregaRepository entregaRepository;

    // ---------------------------------------------------------------
    // Tokens de prueba
    // ---------------------------------------------------------------

    private String tokenDe(Rol rol, Long idPerfil) {
        return servicioTokens.emitir(new SesionUsuario(99L, "Prueba", "prueba@agrotech.co", rol, idPerfil)).valor();
    }

    private String agricultor() {
        return tokenDe(Rol.AGRICULTOR, ID_AGRICULTOR);
    }

    private String cliente(long idCliente) {
        return tokenDe(Rol.CLIENTE, idCliente);
    }

    private String admin() {
        return tokenDe(Rol.ADMIN, null);
    }

    private ResultActions enviar(MockHttpServletRequestBuilder solicitud, String token) throws Exception {
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mvc.perform(solicitud);
    }

    private static MockHttpServletRequestBuilder postJson(String ruta, String cuerpo) {
        return post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Rutas publicas")
    class RutasPublicas {

        @Test
        @DisplayName("El catalogo se consulta sin iniciar sesion")
        void catalogoSinSesion() throws Exception {
            when(productoService.listar(null, null)).thenReturn(List.of());

            enviar(get("/api/productos"), null).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Las categorias se consultan sin iniciar sesion")
        void categoriasSinSesion() throws Exception {
            when(categoriaService.listar()).thenReturn(List.of());

            enviar(get("/api/categorias"), null).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Sin sesion valida responde 401")
    class SinSesion {

        @Test
        @DisplayName("Publicar un producto sin token")
        void publicarSinToken() throws Exception {
            enviar(postJson("/api/productos", PRODUCTO_VALIDO.formatted(ID_AGRICULTOR)), null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").isNotEmpty());

            verify(productoService, never()).crear(any());
        }

        @Test
        @DisplayName("Un token falsificado (firmado con otra clave) aunque diga ADMIN")
        void tokenFalsificado() throws Exception {
            JwtEncoder otraClave = new NimbusJwtEncoder(
                    new ImmutableSecret<>(new SecretKeySpec("una-clave-que-no-es-la-de-agrotech!".getBytes(), "HmacSHA256")));
            String falso = new ServicioTokens(otraClave, Duration.ofHours(1))
                    .emitir(new SesionUsuario(1L, "Intruso", "x@x.co", Rol.ADMIN, null)).valor();

            enviar(get("/api/pedidos"), falso).andExpect(status().isUnauthorized());

            verify(pedidoService, never()).listarTodos(any());
        }

        @Test
        @DisplayName("Un token vencido")
        void tokenVencido() throws Exception {
            // Firmado con la clave correcta, pero vencido hace una hora
            // (el validador solo tolera 60 segundos de diferencia de reloj)
            Instant haceDosHoras = Instant.now().minus(Duration.ofHours(2));
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(ServicioTokens.EMISOR)
                    .issuedAt(haceDosHoras)
                    .expiresAt(haceDosHoras.plus(Duration.ofHours(1)))
                    .subject("1")
                    .claim(ServicioTokens.CLAIM_ROL, Rol.ADMIN.name())
                    .build();
            String vencido = codificador.encode(
                    JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

            enviar(get("/api/pedidos"), vencido).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Un texto cualquiera como token")
        void tokenBasura() throws Exception {
            enviar(get("/api/pedidos"), "esto-no-es-un-token").andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Consultar las resenas de un cliente sin token")
        void resenasSinToken() throws Exception {
            enviar(get("/api/clientes/" + ID_CLIENTE + "/resenas"), null).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Con el rol equivocado o a nombre de otro responde 403")
    class SinPermiso {

        @Test
        @DisplayName("Un cliente no puede publicar productos")
        void clientePublicaProducto() throws Exception {
            enviar(postJson("/api/productos", PRODUCTO_VALIDO.formatted(ID_AGRICULTOR)), cliente(ID_CLIENTE))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensaje").isNotEmpty());

            verify(productoService, never()).crear(any());
        }

        @Test
        @DisplayName("Un agricultor no puede publicar a nombre de otra finca")
        void agricultorPublicaComoOtro() throws Exception {
            enviar(postJson("/api/productos", PRODUCTO_VALIDO.formatted(ID_AGRICULTOR + 1)), agricultor())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensaje").value("Solo puede actuar a nombre de su propia finca"));

            verify(productoService, never()).crear(any());
        }

        @Test
        @DisplayName("Un cliente no puede crear categorias")
        void clienteCreaCategoria() throws Exception {
            enviar(postJson("/api/categorias", "{\"nombre\": \"Frutas\"}"), cliente(ID_CLIENTE))
                    .andExpect(status().isForbidden());

            verify(categoriaService, never()).crear(any());
        }

        @Test
        @DisplayName("Un agricultor no puede comprar")
        void agricultorCompra() throws Exception {
            enviar(postJson("/api/pedidos", "{\"idCliente\": 1, \"metodoPago\": \"PSE\", \"items\": []}"), agricultor())
                    .andExpect(status().isForbidden());

            verify(pedidoService, never()).registrarVenta(any());
        }

        @Test
        @DisplayName("Un cliente no puede ver todos los pedidos")
        void clienteVeTodosLosPedidos() throws Exception {
            enviar(get("/api/pedidos"), cliente(ID_CLIENTE)).andExpect(status().isForbidden());

            verify(pedidoService, never()).listarTodos(any());
        }

        @Test
        @DisplayName("Un cliente no puede ver las compras de otro cliente")
        void clienteVeComprasDeOtro() throws Exception {
            enviar(get("/api/pedidos").param("idCliente", String.valueOf(ID_CLIENTE + 1)), cliente(ID_CLIENTE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Un cliente no puede ver las resenas de otro cliente")
        void clienteVeResenasDeOtro() throws Exception {
            enviar(get("/api/clientes/" + (ID_CLIENTE + 1) + "/resenas"), cliente(ID_CLIENTE))
                    .andExpect(status().isForbidden());

            verify(resenaService, never()).listarPorCliente(any());
        }
    }

    @Nested
    @DisplayName("Con el permiso correcto la solicitud pasa")
    class ConPermiso {

        @Test
        @DisplayName("El agricultor publica un producto de su finca (201)")
        void agricultorPublica() throws Exception {
            Producto creado = new Producto();
            creado.setId(30L);
            creado.setIdAgricultor(ID_AGRICULTOR);
            creado.setNombre("Mango de azucar");
            creado.setPrecio(new BigDecimal("4200"));
            when(productoService.crear(any())).thenReturn(creado);

            enviar(postJson("/api/productos", PRODUCTO_VALIDO.formatted(ID_AGRICULTOR)), agricultor())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(30));
        }

        @Test
        @DisplayName("El administrador publica a nombre de cualquier finca (201)")
        void adminPublica() throws Exception {
            Producto creado = new Producto();
            creado.setId(31L);
            when(productoService.crear(any())).thenReturn(creado);

            enviar(postJson("/api/productos", PRODUCTO_VALIDO.formatted(ID_AGRICULTOR + 1)), admin())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("El agricultor crea una categoria (201)")
        void agricultorCreaCategoria() throws Exception {
            Categoria categoria = new Categoria();
            categoria.setId(4L);
            categoria.setNombre("Frutas");
            when(categoriaService.crear(any())).thenReturn(categoria);

            enviar(postJson("/api/categorias", "{\"nombre\": \"Frutas\"}"), agricultor())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Frutas"));
        }

        @Test
        @DisplayName("El cliente ve sus propias compras")
        void clienteVeSusCompras() throws Exception {
            when(pedidoService.listarPorCliente(ID_CLIENTE, null)).thenReturn(List.of());

            enviar(get("/api/pedidos").param("idCliente", String.valueOf(ID_CLIENTE)), cliente(ID_CLIENTE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("El cliente ve sus propias resenas")
        void clienteVeSusResenas() throws Exception {
            when(resenaService.listarPorCliente(ID_CLIENTE)).thenReturn(List.of());

            enviar(get("/api/clientes/" + ID_CLIENTE + "/resenas"), cliente(ID_CLIENTE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("El administrador ve todos los pedidos")
        void adminVeTodo() throws Exception {
            when(pedidoService.listarTodos(null)).thenReturn(List.of());

            enviar(get("/api/pedidos"), admin()).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Con permiso pero datos invalidos responde 400 con el detalle de cada campo")
    void datosInvalidos() throws Exception {
        enviar(postJson("/api/productos", "{\"idAgricultor\": " + ID_AGRICULTOR + "}"), agricultor())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").isNotEmpty());

        verify(productoService, never()).crear(any());
    }
}
