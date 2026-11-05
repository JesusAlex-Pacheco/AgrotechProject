# AgroTech API

API REST del proyecto **AgroTech — Del campo a tu mesa, sin intermediarios**.

Asignatura: Desarrollo de Software · Docente: Willian Díaz Villegas · ITM

Integrantes: **Jesús Pacheco** · **Sebastián Galeano**

---

## 1. Qué incluye esta entrega

| Criterio evaluado | Dónde se cumple |
|---|---|
| Implementación de APIs | 6 controladores y 26 endpoints: `UsuarioController`, `ProductoController` (CRUD), `CategoriaController` (CRUD), `PedidoController`, `EntregaController` y `ResenaController` |
| Uso de métodos HTTP | POST, GET, PUT, PATCH, DELETE |
| Arquitectura n-capas | Paquetes `controller` → `service` → `repository` → `domain` / `entity` |
| Separación entre capas con interfaces | Servicios (`service/` + `service/impl/`) y repositorios (`repository/`) se usan siempre a través de su interfaz |
| Principios SOLID | Ver sección 3.1 |
| Clean Code | Ver sección 3.2 |
| DAO con SQL | Catálogo y ventas en `repository/jdbc`: `PreparedStatement`, SQL escrito a mano, **sin `SELECT *`** y **todas las conexiones cerradas** con `try-with-resources` — ver sección 3.3 |
| DAO con JPA | Usuarios, entregas y reseñas en `entity/` con `@OneToOne`, `@ManyToOne` y `@OneToMany`, y repositorios Spring Data — ver sección 3.3 |
| Casos de negocio | Registro de usuarios por rol, venta con descuento de inventario, ciclo de vida del pedido, programación y seguimiento de entregas, historial de compras y reseñas de productos recibidos |
| Documentación de la API | Swagger UI (springdoc-openapi) en `/swagger-ui.html`: las 26 operaciones con descripción, códigos de respuesta, ejemplos y formato de error |
| Manejo de errores | `ManejadorGlobalErrores`: un formato único (`ErrorResponse`) para 400, 404, 405, 409 y 500 |
| Pruebas | 98 pruebas unitarias; cubren todos los métodos públicos de los servicios (`src/test`) |
| Trabajo en equipo | Commits de ambos integrantes |
| Estrategia de ramas | Git Flow simplificado — ver sección 12 |

## 2. Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java (JDK) | 25 | Lenguaje |
| Spring Boot | 4.1.1 | Framework web (Spring MVC + Tomcat embebido) |
| Gradle (wrapper) | 9.7.1 | Construcción y dependencias |
| MySQL | 8.0.16 o superior | Base de datos relacional |
| JDBC + HikariCP | — | Acceso a datos con SQL escrito a mano (catálogo y ventas) |
| Spring Data JPA + Hibernate | 7.4 | Acceso a datos con entidades y relaciones (usuarios, entregas, reseñas) |
| Spring Security Crypto | — | Cifrado BCrypt de las claves de los usuarios |
| Jakarta Validation | — | Validación de los datos de entrada |
| Lombok | — | Getters, setters y constructores de las entidades |
| springdoc-openapi | 3.1.1 | Swagger UI para documentar y probar la API |

## 3. Arquitectura

```
controller/        Capa de presentación. Expone los servicios REST.
                   No contiene reglas de negocio ni SQL.
      |  depende de la interfaz
service/           Contratos de negocio (interfaces).
service/impl/      Reglas de negocio: valida existencias, congela precios,
                   calcula totales, controla el ciclo de vida del pedido,
                   las entregas, los registros y las reseñas.
      |  depende de la interfaz
repository/        Contratos de persistencia (interfaces).
                   - Producto, Categoria, Pedido: los implementa repository/jdbc/.
                   - Usuario, Agricultor, Cliente, Transportador, Entrega,
                     Resena: extienden JpaRepository; Spring Data genera la
                     implementación.
repository/jdbc/   Implementación con JDBC puro y SQL.
      |
domain/            Entidades del catálogo y las ventas (persistidas con JDBC)
                   y enumeraciones. No conocen la base de datos.
entity/            Entidades JPA con sus relaciones mapeadas.
```

Cada capa depende de la **interfaz** de la capa de abajo, nunca de su
implementación. Spring inyecta la implementación por constructor. Gracias a
esto:

- Cambiar de motor de base de datos no obliga a tocar los servicios.
- Las reglas de negocio se prueban sin base de datos, reemplazando los
  repositorios por simulaciones (Mockito).

### 3.1 Principios SOLID

| Principio | Cómo se aplica |
|---|---|
| **S** — Responsabilidad única | Cada capa tiene un solo trabajo: el controlador traduce HTTP, el servicio aplica reglas, el repositorio ejecuta SQL. `EjecutorJdbc` concentra el manejo de conexiones y `ManejadorGlobalErrores` el de errores |
| **O** — Abierto/cerrado | `PedidoServiceImpl` avisa cada cambio de estado a la interfaz `SeguimientoPedido`; la entrega reacciona con `SeguimientoEntrega` sin que el servicio de pedidos la conozca. Una reacción nueva es una clase nueva, no un cambio en el servicio. Igual con los errores: uno nuevo es un método más del manejador global |
| **L** — Sustitución de Liskov | Cualquier implementación de un repositorio (la JDBC, la que genera Spring Data o una simulada en las pruebas) funciona igual para el servicio |
| **I** — Segregación de interfaces | Un contrato de servicio por caso de uso (`UsuarioService`, `EntregaService`, `ResenaService`…) y repositorios con solo las consultas que el negocio usa |
| **D** — Inversión de dependencias | Controlador → interfaz de servicio → interfaz de repositorio. Los servicios usan `PasswordEncoder` (interfaz), no BCrypt directamente. Ninguna capa conoce la clase concreta de la siguiente |

### 3.2 Clean Code

- **Sin código repetido en la persistencia:** `EjecutorJdbc` concentra el
  patrón *abrir conexión → preparar → ejecutar → cerrar → traducir error* y
  las transacciones (`enTransaccion`). Cada repositorio solo escribe su SQL,
  sus parámetros y su mapeo.
- **Reglas en el dominio:** `Pago.pendiente()`, `pago.aprobar()`,
  `pago.rechazar()` y `estadoPedido.puedeCambiarA()` expresan el negocio con
  nombres del negocio.
- **Métodos cortos con nombre propio:** `validarTransicion`,
  `aplicarEfectosSobrePago`, `validarClienteExiste`, `aplicarDatos`.
- **Un solo formato de error** (`ErrorResponse`) para todas las respuestas,
  incluidos JSON mal formado, parámetros inválidos y rutas inexistentes.
- **Errores del servidor registrados en el log** con su causa real, sin
  exponer detalles técnicos al cliente.
- **Entidades JPA con comportamiento:** `Entrega.programar()`,
  `entrega.despachar()`, `entrega.confirmar()`, `cliente.agregarResena()`
  (mantiene sincronizados los dos lados de la relación).
- **Sin `@Data` en las entidades JPA:** solo `@Getter`/`@Setter`, para evitar
  ciclos infinitos en `toString`/`equals` con relaciones bidireccionales.
- **DTOs anidados reutilizables:** `DatosUsuarioRequest` se reutiliza en los
  tres registros en lugar de repetir nombre, correo y clave.

Las entidades usan **Lombok** (`@Getter`, `@Setter`, `@NoArgsConstructor`)
para evitar el código repetitivo de los accesores.

`dto/` transporta los datos entre el front end y la API, de modo que las
entidades del dominio no queden expuestas directamente.

`exception/` centraliza el manejo de errores: `ManejadorGlobalErrores`
traduce las excepciones a respuestas HTTP (400, 404, 405, 409, 500) con el
formato único `ErrorResponse`, sin exponer trazas técnicas al cliente.

### 3.3 Persistencia: una parte con SQL y otra con JPA

Las dos tecnologías comparten el mismo pool de conexiones (HikariCP) y la
misma base. Cada tabla tiene **un solo dueño**:

| Con SQL (`repository/jdbc`) | Con JPA (`entity/` + Spring Data) |
|---|---|
| `producto`, `categoria`, `pedido`, `detalle_pedido`, `pago` | `usuario`, `agricultor`, `cliente`, `transportador`, `entrega`, `resena` |
| La venta y el cambio de estado necesitan transacciones a mano y `UPDATE` condicionales para controlar la concurrencia | Registros y consultas con relaciones, donde JPA ahorra código |

**SQL — conexiones cerradas y sin `SELECT *`.** Todo el acceso pasa por
`EjecutorJdbc`, que abre la conexión, la sentencia y el `ResultSet` en
bloques `try-with-resources`: se cierran siempre, incluso si hay error. Las
transacciones hacen `rollback` ante cualquier fallo. Cada consulta nombra
sus columnas.

**JPA — relaciones mapeadas:**

| Relación | Dónde | Detalle |
|---|---|---|
| `@OneToOne` | `Agricultor`, `Cliente`, `Transportador` → `Usuario` | `id_usuario` es `UNIQUE`. `cascade = PERSIST`: el usuario se guarda junto con su perfil |
| `@ManyToOne` | `Resena` → `Cliente`, `Entrega` → `Transportador` | Lado dueño de la relación (tiene la llave foránea) |
| `@OneToMany` | `Cliente.resenas`, `Transportador.entregas` | Lado inverso (`mappedBy`). `GET /api/clientes/{id}/resenas` recorre `Cliente.resenas` |

- Todas las relaciones son `LAZY` y `spring.jpa.open-in-view=false`: cada
  consulta carga con `JOIN FETCH` solo lo que se va a mostrar.
- `spring.jpa.hibernate.ddl-auto=validate`: al arrancar, Hibernate comprueba
  que las entidades coincidan con las tablas de `db/01_esquema_mysql.sql`.
  Nunca crea ni borra tablas.
- Donde una entidad JPA apunta a una tabla de SQL (`Entrega.idPedido`,
  `Resena.idProducto`) se guarda solo el id, no una relación, para que cada
  tecnología siga siendo dueña de sus tablas.
- Ninguna operación escribe con las dos tecnologías en la misma transacción.
  La entrega se actualiza en su propia transacción JPA, después de confirmada
  la transacción JDBC del pedido.

## 4. Requisitos

- **JDK 25**
- **MySQL 8.0.16 o superior** (servidor en ejecución en el puerto 3306)
- Gradle **no** hace falta instalarlo: se usa el wrapper (`gradlew` / `gradlew.bat`)

## 5. Base de datos

### 5.1 Cómo se conecta la API

La conexión se configura en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:agrotech}?...
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD:root}
```

La sintaxis `${VARIABLE:valor}` significa: usar la variable de entorno si
existe y, si no, el valor por defecto. Sin configurar nada, la API se conecta a:

| Parámetro | Valor por defecto |
|---|---|
| Servidor | `localhost:3306` |
| Base de datos | `agrotech` |
| Usuario | `root` |
| Contraseña | `root` |

Spring Boot crea el pool de conexiones (HikariCP). Los repositorios de
`repository/jdbc` y Hibernate (JPA) piden conexiones a ese mismo pool.

**MySQL debe estar encendido y con las tablas creadas antes de arrancar la
API:** al iniciar, Hibernate valida que las entidades coincidan con las
tablas. Si MySQL está apagado o falta una tabla, la aplicación no arranca y
el log indica la causa.

### 5.2 Scripts

| Script | Qué hace |
|---|---|
| `db/01_esquema_mysql.sql` | Crea la base `agrotech` y todas las tablas, llaves foráneas y restricciones |
| `db/02_datos_iniciales.sql` | Inserta datos de prueba: 1 agricultor, 1 cliente, 4 categorías y 3 productos |

Se ejecutan **una sola vez y en orden**. El esquema no borra tablas
existentes: para reiniciar la base desde cero, ejecutar antes
`DROP DATABASE agrotech;`.

Datos que quedan disponibles para las pruebas:

| Tabla | id | Registro |
|---|---|---|
| agricultor | 1 | Finca La Esperanza (Hernando Restrepo) |
| cliente | 1 | Laura Gomez |
| categoria | 1, 2, 3, 4 | Frutas, Verduras, Tuberculos, Hierbas |
| producto | 1, 2, 3 | Aguacate Hass, Cilantro, Papa criolla |

Los datos iniciales no traen transportadores: antes de programar una entrega
hay que registrar uno con `POST /api/transportadores`.

Las claves de los usuarios de ejemplo son un texto de relleno. Los usuarios
que se registran por la API quedan con un hash BCrypt real.

### 5.3 Opción A: con MySQL Workbench (sin comandos)

1. Instalar **MySQL Community Server** y **MySQL Workbench** desde
   <https://dev.mysql.com/downloads/installer/>. Durante la instalación se
   define la contraseña del usuario `root`; si se usa `root`, coincide con la
   configuración por defecto de la API.
2. Abrir Workbench y entrar a la conexión **Local instance MySQL**.
3. `File → Open SQL Script…` → abrir `db/01_esquema_mysql.sql` → botón del
   rayo ⚡ (*Execute*).
4. Repetir con `db/02_datos_iniciales.sql`.
5. En el panel **Schemas**, actualizar (🔄): debe aparecer `agrotech` con sus
   tablas.

### 5.4 Opción B: desde la terminal

Bash:

```bash
mysql -u root -p < db/01_esquema_mysql.sql
mysql -u root -p < db/02_datos_iniciales.sql
```

PowerShell (no admite `<`, se usa `source`):

```powershell
mysql -u root -p -e "source db/01_esquema_mysql.sql"
mysql -u root -p -e "source db/02_datos_iniciales.sql"
```

### 5.5 Contraseña distinta de `root`

Las credenciales no se suben al repositorio. Si la contraseña de MySQL es
otra, definirla como variable de entorno antes de ejecutar la API:

```bash
export DB_USER=root
export DB_PASSWORD=tu_clave
```

```powershell
$env:DB_USER="root"; $env:DB_PASSWORD="tu_clave"
```

Variables disponibles: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD` y `PORT` (puerto de la API).

## 6. Ejecutar la API

Desde VS Code: abrir `AgrotechApplication.java` y pulsar **Run** sobre el
método `main`.

Desde la terminal:

```bash
./gradlew bootRun          # Linux / macOS
.\gradlew.bat bootRun      # Windows
```

La API queda disponible en `http://localhost:8080`. La aplicación sigue en
ejecución esperando peticiones hasta que se detiene (Stop o `Ctrl+C`).

En la consola, estas líneas confirman que arrancó bien:

```
Tomcat started on port 8080 (http) with context path '/'
Started AgrotechApplication in X seconds
```

## 7. Endpoints

### Usuarios — registro por rol (JPA)

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/agricultores` | Registrar un agricultor y su finca | 201 / 400 / 409 si el correo existe |
| GET | `/api/agricultores/{id}` | Consultar un agricultor | 200 / 404 |
| POST | `/api/clientes` | Registrar un cliente | 201 / 400 / 409 |
| GET | `/api/clientes/{id}` | Consultar un cliente | 200 / 404 |
| POST | `/api/transportadores` | Registrar un transportador | 201 / 400 / 409 |
| GET | `/api/transportadores/{id}` | Consultar un transportador | 200 / 404 |

Cuerpo de ejemplo (los datos de acceso van en el objeto `usuario`, igual en
los tres registros):

```json
{
  "usuario": {
    "nombre": "Marta Lucia Ospina",
    "correo": "marta@agrotech.co",
    "clave": "Campo2026*",
    "telefono": "3107778899"
  },
  "nombreFinca": "Finca El Roble",
  "municipio": "Rionegro",
  "vereda": "Llanogrande"
}
```

- El correo es único sin importar mayúsculas y se guarda en minúsculas.
- La clave (8 a 72 caracteres) se guarda cifrada con **BCrypt** y nunca
  aparece en las respuestas.
- Usuario y perfil se guardan en una sola transacción: si falla uno, no se
  guarda ninguno.

### Producto — CRUD completo

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/productos` | Crear un producto | 201 Created / 404 si el agricultor o la categoría no existen |
| GET | `/api/productos` | Listar el catálogo (filtros: `idCategoria`, `nombre`) | 200 OK |
| GET | `/api/productos/{id}` | Consultar un producto | 200 OK / 404 |
| PUT | `/api/productos/{id}` | Actualizar un producto | 200 OK / 404 / 409 si se intenta cambiar el agricultor |
| DELETE | `/api/productos/{id}` | Baja lógica del producto | 204 No Content / 404 |

El DELETE es **baja lógica**: marca `disponible = false` en lugar de borrar la
fila, porque el producto puede estar referenciado en pedidos históricos.

Cuerpo de ejemplo para POST y PUT:

```json
{
  "idAgricultor": 1,
  "idCategoria": 1,
  "nombre": "Mango de azucar",
  "descripcion": "Cosecha de la semana",
  "precio": 4200.00,
  "unidadMedida": "kg",
  "stock": 50.00,
  "fotoUrl": null
}
```

`idAgricultor` e `idCategoria` deben existir en la base de datos. Un producto
pertenece siempre al agricultor que lo publicó: el PUT no permite cambiarlo.

### Categoría — CRUD completo

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/categorias` | Crear una categoría | 201 Created / 409 si el nombre existe |
| GET | `/api/categorias` | Listar las categorías | 200 OK |
| GET | `/api/categorias/{id}` | Consultar una categoría | 200 OK / 404 |
| PUT | `/api/categorias/{id}` | Renombrar una categoría | 200 OK / 404 / 409 |
| DELETE | `/api/categorias/{id}` | Eliminar una categoría sin productos | 204 No Content / 404 / 409 |

El nombre no se puede repetir, sin importar mayúsculas. El DELETE es físico
porque la tabla no tiene columna de estado, y se rechaza si la categoría tiene
productos.

### Pedido — casos de negocio

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/pedidos` | Registrar una venta |
| GET | `/api/pedidos/{id}` | Consultar un pedido con sus líneas y su pago |
| GET | `/api/pedidos?idCliente=1&estado=ENTREGADO` | Historial de compras de un cliente (el `estado` es opcional) |
| PATCH | `/api/pedidos/{id}/estado` | Cambiar el estado del pedido: `{ "estado": "PAGADO" }` |

Cuerpo de ejemplo:

```json
{
  "idCliente": 1,
  "metodoPago": "PSE",
  "costoEnvio": 6000.00,
  "items": [
    { "idProducto": 1, "cantidad": 3 },
    { "idProducto": 2, "cantidad": 2 }
  ]
}
```

`metodoPago` acepta `PSE`, `TARJETA` o `CONTRA_ENTREGA`.

`POST /api/pedidos` ejecuta, en una sola transacción JDBC:

1. Valida que el cliente exista y que cada producto exista y tenga existencias suficientes.
2. Congela el precio unitario del momento de la compra.
3. Calcula subtotal, costo de envío y total.
4. Inserta el pedido y sus líneas de detalle.
5. Descuenta el inventario de cada producto.
6. Registra el pago en estado `PENDIENTE`.

Si cualquier paso falla se ejecuta `rollback` y no se persiste nada.

El descuento de inventario usa una sentencia condicional
(`UPDATE ... WHERE stock >= ?`): si otro pedido consumió las existencias entre
la validación y la escritura, la sentencia no afecta ninguna fila y la
transacción se revierte. Es el control de concurrencia de la operación.

#### Ciclo de vida del pedido

```
CREADO ──► PAGADO ──► EN_RUTA ──► ENTREGADO
   │          │
   └──────────┴──────► CANCELADO
```

| Desde | Puede pasar a |
|---|---|
| `CREADO` | `PAGADO`, `EN_RUTA` (solo contra entrega), `CANCELADO` |
| `PAGADO` | `EN_RUTA`, `CANCELADO` |
| `EN_RUTA` | `ENTREGADO` |
| `ENTREGADO`, `CANCELADO` | Nada: son estados finales |

- **PSE o tarjeta:** se paga antes de despachar. Al pasar a `PAGADO`, el pago
  queda `APROBADO`.
- **Contra entrega:** se despacha sin pagar (`CREADO → EN_RUTA`) y se cobra al
  pasar a `ENTREGADO`.
- **Despachar (`EN_RUTA`) exige una entrega programada** (ver *Entregas*).
- **Cancelar** devuelve el inventario y deja `RECHAZADO` el pago pendiente.
- Todo ocurre en una sola transacción. El `UPDATE` del estado incluye
  `WHERE estado = <anterior>`: si dos personas cambian el mismo pedido al
  tiempo, la segunda recibe 409 en lugar de pisar el cambio de la primera.

### Entregas — logística del pedido (JPA)

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/pedidos/{id}/entrega` | Programar la entrega: transportador, dirección y fecha | 201 / 400 / 404 / 409 |
| GET | `/api/pedidos/{id}/entrega` | Consultar la entrega del pedido | 200 / 404 |
| GET | `/api/transportadores/{id}/entregas?estado=PROGRAMADA` | Agenda del transportador (el `estado` es opcional) | 200 / 400 / 404 |

```json
{ "idTransportador": 1, "direccion": "Calle 50 # 40-20, Itagui", "fechaProgramada": "2026-12-01" }
```

- Solo se programa la entrega de un pedido `CREADO` o `PAGADO`, y una sola
  vez por pedido. La fecha no puede estar en el pasado.
- Si no se envía `direccion`, se usa la del cliente; si el cliente tampoco
  tiene, se responde 409.
- La entrega avanza sola con el pedido:

| El pedido pasa a | La entrega queda |
|---|---|
| `EN_RUTA` | `EN_RUTA` |
| `ENTREGADO` | `ENTREGADA`, con la fecha de entrega |
| `CANCELADO` | `FALLIDA` |

### Reseñas — calificación de productos (JPA + SQL)

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/productos/{id}/resenas` | Calificar un producto de 1 a 5 | 201 / 400 / 404 / 409 |
| GET | `/api/productos/{id}/resenas` | Reseñas del producto con promedio y total | 200 / 404 |
| GET | `/api/clientes/{id}/resenas` | Reseñas escritas por el cliente | 200 / 404 |

```json
{ "idCliente": 1, "calificacion": 5, "comentario": "Llego fresco y bien empacado" }
```

- **Solo reseña quien recibió el producto:** el cliente debe tener un pedido
  `ENTREGADO` que lo incluya. Esta consulta se hace con SQL sobre `pedido` y
  `detalle_pedido`.
- Una reseña por cliente y producto.
- El promedio se muestra con un decimal y es `null` si aún no hay reseñas.

## 8. Códigos de respuesta

| Código | Cuándo |
|---|---|
| 200 | Consulta o actualización correcta |
| 201 | Recurso creado |
| 204 | Baja lógica o eliminación aplicada |
| 400 | Datos inválidos, JSON mal formado, parámetro con tipo incorrecto o faltante |
| 404 | El recurso no existe (producto, categoría, pedido, usuario, entrega o ruta) |
| 405 | Método HTTP no permitido en esa ruta |
| 409 | Se violó una regla de negocio (sin existencias, transición no permitida, correo o nombre repetido, despacho sin entrega, reseña sin compra…) o una restricción única de la base |
| 500 | Error de persistencia (por ejemplo, MySQL apagado) |

Todas las respuestas de error tienen el mismo formato:

```json
{
  "fecha": "2026-10-03T10:15:30",
  "estado": 404,
  "mensaje": "No existe el cliente con id 999"
}
```

Los errores de validación agregan el campo `errores` con el mensaje de cada campo.

## 9. Probar la API

### 9.1 Swagger UI 

Con la API en ejecución, abrir en el navegador:

- **<http://localhost:8080/swagger-ui.html>**: interfaz para probar los endpoints.
- <http://localhost:8080/v3/api-docs>: especificación OpenAPI en JSON.

En cada endpoint: **Try it out** → completar los parámetros o el JSON →
**Execute**. Swagger muestra la respuesta y el código HTTP.

Los endpoints están agrupados en **Usuarios**, **Productos**, **Categorías**,
**Pedidos**, **Entregas** y **Reseñas**.
Cada uno tiene su descripción, los códigos de respuesta posibles con su
significado y JSON de ejemplo con datos reales del proyecto. La configuración
general está en `config/OpenApiConfig.java`.

### 9.2 Archivo `requests.http`

Contiene todas las peticiones listas para ejecutar, incluidos los casos de
error 400 y 409. Se usa desde IntelliJ IDEA o desde la extensión
**REST Client** de VS Code (enlace *Send Request* sobre cada petición).
También sirve como evidencia de las pruebas ante el docente.

### 9.3 Terminal

curl:

```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"idCliente":1,"metodoPago":"PSE","costoEnvio":6000,
       "items":[{"idProducto":1,"cantidad":3}]}'
```

PowerShell:

```powershell
$body = @{ idAgricultor=1; idCategoria=1; nombre="Mango de azucar"; precio=4200; unidadMedida="kg"; stock=50 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/productos -ContentType "application/json" -Body $body
```

### 9.4 Pruebas unitarias

```bash
./gradlew test
```

98 pruebas sin base de datos: los repositorios se reemplazan por
simulaciones con Mockito. **Cada método público de cada servicio** tiene al
menos una prueba del caso exitoso y de sus casos de error.

| Clase de prueba | Pruebas | Qué verifica |
|---|---|---|
| `EstadoPedidoTest` | 13 | Transiciones permitidas y prohibidas del ciclo de vida |
| `ProductoServiceImplTest` | 12 | `crear`, `consultar`, `listar`, `actualizar`, `desactivar`: agricultor y categoría inexistentes, stock 0, cambio de agricultor, 404 |
| `CategoriaServiceImplTest` | 11 | `crear`, `consultar`, `listar`, `actualizar`, `eliminar`: nombre repetido o de otra categoría, con y sin productos, 404 |
| `PedidoServiceImplTest` | 17 | `registrarVenta`, `consultar`, `listarPorCliente`, `cambiarEstado`: total, existencias, reglas de pago, cancelación, aviso al seguimiento y bloqueo |
| `UsuarioServiceImplTest` | 12 | Los 3 registros (rol, correo normalizado, clave cifrada, correo repetido) y las 3 consultas |
| `EntregaServiceImplTest` | 13 | `programar` (dirección propia o del cliente, pedido ya despachado, entrega repetida, 404), `consultarPorPedido`, `listarPorTransportador` |
| `SeguimientoEntregaTest` | 10 | Bloqueo del despacho sin entrega y sincronización EN_RUTA / ENTREGADA / FALLIDA |
| `ResenaServiceImplTest` | 10 | `registrar` (sin compra, reseña repetida, 404), `listarPorProducto`, `listarPorCliente` |

El reporte queda en `build/reports/tests/test/index.html`.

## 10. Estructura del repositorio

```
AgrotechProject/
├── db/                          Scripts SQL de esquema y datos
├── src/main/java/edu/itm/agrotech/
│   ├── AgrotechApplication.java Punto de entrada de Spring Boot
│   ├── config/                  Swagger (OpenAPI) y cifrado de claves (BCrypt)
│   ├── controller/              Capa de presentación
│   ├── service/                 Contratos de negocio (interfaces)
│   │   └── impl/                Reglas de negocio
│   ├── repository/              Contratos de persistencia (interfaces JDBC y JPA)
│   │   └── jdbc/                Implementación JDBC y EjecutorJdbc
│   ├── domain/                  Entidades de catálogo y ventas, y enumeraciones
│   ├── entity/                  Entidades JPA con sus relaciones
│   ├── dto/                     Objetos de transporte y ErrorResponse
│   └── exception/               Excepciones y manejador global
├── src/main/resources/
│   └── application.properties   Configuración (BD y puerto)
├── src/test/java/edu/itm/agrotech/
│   ├── domain/                  Pruebas del ciclo de vida
│   └── service/impl/            Pruebas de las reglas de negocio
├── requests.http                Pruebas de la API
├── build.gradle                 Dependencias y configuración de Gradle
└── settings.gradle
```

## 11. Comandos útiles

| Comando | Para qué |
|---|---|
| `./gradlew build` | Compila, ejecuta las pruebas y genera el `.jar` en `build/libs/` |
| `./gradlew test` | Ejecuta solo las pruebas unitarias |
| `./gradlew build --refresh-dependencies` | Igual, pero vuelve a descargar las dependencias |
| `./gradlew bootRun` | Levanta la API en el puerto 8080 |
| `./gradlew clean` | Borra la carpeta `build/` |

En Windows se usa `.\gradlew.bat` en lugar de `./gradlew`.

## 12. Estrategia de ramas y commits

### 12.1 Modelo adoptado: Git Flow simplificado

| Rama | Propósito | Quién escribe en ella |
|---|---|---|
| `main` | Código estable, listo para entregar o desplegar. | Nadie directamente: solo recibe merges desde `develop`. |
| `develop` | Integración del trabajo del equipo. | Solo por Pull Request desde ramas `feature/*`. |
| `feature/<nombre>` | Una tarea concreta. Se crea desde `develop` y se elimina al integrarla. | El integrante asignado a esa tarea. |

Regla: **nunca se hace push directo a `main` ni a `develop`.** Todo entra por
Pull Request, y así queda registro de quién aportó qué.


### 12.2 Reparto de tareas

Se evalúan los commits de cada integrante, así que cada uno trabaja en sus
propias ramas y hace sus propios commits.

| Integrante | Ramas a su cargo |
|---|---|
| Jesús Pacheco | `feature/dominio-y-bd`, `feature/crud-producto`, `feature/manejo-errores` |
| Sebastián Galeano | `feature/capa-persistencia`, `feature/venta-pedido`, `feature/documentacion` |




