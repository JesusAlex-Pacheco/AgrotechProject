# AgroTech API

API REST del proyecto **AgroTech — Del campo a tu mesa, sin intermediarios**.

Asignatura: Desarrollo de Software · Docente: Willian Díaz Villegas · ITM

Integrantes: **Jesús Pacheco** · **Sebastián Galeano**

---

## 1. Qué incluye esta entrega

| Criterio evaluado | Dónde se cumple |
|---|---|
| Implementación de APIs | 3 controladores y 14 endpoints: `ProductoController` (CRUD), `CategoriaController` (CRUD), `PedidoController` (casos de negocio) |
| Uso de métodos HTTP | POST, GET, PUT, PATCH, DELETE |
| Arquitectura n-capas | Paquetes `controller` → `service` → `repository` → `domain` |
| Separación entre capas con interfaces | Servicios (`service/` + `service/impl/`) y repositorios (`repository/` + `repository/jdbc/`) se usan siempre a través de su interfaz |
| Principios SOLID | Ver sección 3.1 |
| Clean Code | Ver sección 3.2 |
| Base de datos relacional con JDBC y SQL | `repository/jdbc` — `Connection`, `PreparedStatement`, SQL escrito a mano |
| Casos de negocio | Registrar una venta (transacción con descuento de inventario), ciclo de vida del pedido (pago, despacho, entrega, cancelación con devolución de inventario) e historial de compras del cliente |
| Documentación de la API | Swagger UI (springdoc-openapi) en `/swagger-ui.html`, con descripción de cada endpoint, sus respuestas y ejemplos |
| Pruebas | 33 pruebas unitarias de las reglas de negocio (`src/test`) |
| Trabajo en equipo | Commits de ambos integrantes |
| Estrategia de ramas | Git Flow simplificado — ver sección 12 |

## 2. Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java (JDK) | 25 | Lenguaje |
| Spring Boot | 4.1.1 | Framework web (Spring MVC + Tomcat embebido) |
| Gradle (wrapper) | 9.7.1 | Construcción y dependencias |
| MySQL | 8.0.16 o superior | Base de datos relacional |
| JDBC + HikariCP | — | Acceso a datos con SQL escrito a mano |
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
                   calcula totales, controla el ciclo de vida del pedido.
      |  depende de la interfaz
repository/        Contratos de persistencia (interfaces).
repository/jdbc/   Implementación con JDBC puro y SQL.
      |
domain/            Entidades y enumeraciones. No conocen la base de datos.
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
| **O** — Abierto/cerrado | Un error nuevo se agrega como un método más del manejador global, sin tocar los controladores. Las transiciones del pedido viven en `EstadoPedido.puedeCambiarA` |
| **L** — Sustitución de Liskov | Cualquier implementación de `ProductoRepository` (la JDBC o una simulada en las pruebas) funciona igual para el servicio |
| **I** — Segregación de interfaces | Interfaces pequeñas por entidad. `AgricultorRepository` y `ClienteRepository` solo exponen `existe(id)`, que es lo único que el negocio necesita |
| **D** — Inversión de dependencias | Controlador → interfaz de servicio → interfaz de repositorio. Ninguna capa conoce la clase concreta de la siguiente |

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

Las entidades del paquete `domain` usan **Lombok** (`@Getter`, `@Setter`,
`@NoArgsConstructor`) para evitar el código repetitivo de los accesores.

`dto/` transporta los datos entre el front end y la API, de modo que las
entidades del dominio no queden expuestas directamente.

`exception/` centraliza el manejo de errores: `ManejadorGlobalErrores`
traduce las excepciones a respuestas HTTP (400, 404, 405, 409, 500) con el
formato único `ErrorResponse`, sin exponer trazas técnicas al cliente.

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

Spring Boot crea el pool de conexiones (HikariCP) y los repositorios de
`repository/jdbc` piden conexiones a ese pool para ejecutar el SQL.

La conexión se abre en la **primera consulta**, no al arrancar: la aplicación
puede iniciar bien aunque MySQL esté apagado, y el error aparece al llamar a
un endpoint.

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
- **Cancelar** devuelve el inventario y deja `RECHAZADO` el pago pendiente.
- Todo ocurre en una sola transacción. El `UPDATE` del estado incluye
  `WHERE estado = <anterior>`: si dos personas cambian el mismo pedido al
  tiempo, la segunda recibe 409 en lugar de pisar el cambio de la primera.

## 8. Códigos de respuesta

| Código | Cuándo |
|---|---|
| 200 | Consulta o actualización correcta |
| 201 | Recurso creado |
| 204 | Baja lógica o eliminación aplicada |
| 400 | Datos inválidos, JSON mal formado, parámetro con tipo incorrecto o faltante |
| 404 | El recurso no existe (producto, categoría, pedido, agricultor, cliente o ruta) |
| 405 | Método HTTP no permitido en esa ruta |
| 409 | Se violó una regla de negocio (sin existencias, transición no permitida, nombre repetido…) |
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

Los endpoints están agrupados en **Productos**, **Categorías** y **Pedidos**.
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

33 pruebas de las reglas de negocio, sin base de datos: los repositorios se
reemplazan por simulaciones con Mockito.

| Clase de prueba | Qué verifica |
|---|---|
| `EstadoPedidoTest` | Transiciones permitidas y prohibidas del ciclo de vida |
| `ProductoServiceImplTest` | Agricultor y categoría inexistentes, producto sin stock, cambio de agricultor |
| `CategoriaServiceImplTest` | Nombre repetido, renombrar con el mismo nombre, eliminar con y sin productos |
| `PedidoServiceImplTest` | Cálculo del total, falta de existencias, cliente inexistente, reglas de pago por método, cancelación e historial |

El reporte queda en `build/reports/tests/test/index.html`.

## 10. Estructura del repositorio

```
AgrotechProject/
├── db/                          Scripts SQL de esquema y datos
├── src/main/java/edu/itm/agrotech/
│   ├── AgrotechApplication.java Punto de entrada de Spring Boot
│   ├── config/                  Configuración de Swagger (OpenAPI)
│   ├── controller/              Capa de presentación
│   ├── service/                 Contratos de negocio (interfaces)
│   │   └── impl/                Reglas de negocio
│   ├── repository/              Contratos de persistencia (interfaces)
│   │   └── jdbc/                Implementación JDBC y EjecutorJdbc
│   ├── domain/                  Entidades y enumeraciones
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




