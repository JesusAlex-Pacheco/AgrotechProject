# AgroTech API

API REST del proyecto **AgroTech — Del campo a tu mesa, sin intermediarios**.

Asignatura: Desarrollo de Software · Docente: Willian Díaz Villegas · ITM

Integrantes: **Jesús Pacheco** · **Sebastián Galeano**

---

## 1. Qué incluye esta entrega

| Criterio evaluado | Dónde se cumple |
|---|---|
| API REST completa de una entidad (Create, Read, Update, List, Delete) | `ProductoController` — 5 endpoints |
| Uso de métodos HTTP | POST, GET, PUT, DELETE |
| Arquitectura n-capas | Paquetes `controller` → `service` → `repository` → `domain` |
| Base de datos relacional con JDBC y SQL | `repository/jdbc` — `Connection`, `PreparedStatement`, SQL escrito a mano |
| Interacción de negocio | `POST /api/pedidos` — registro de una venta con descuento de inventario |
| Documentación de la API | Swagger UI (springdoc-openapi) en `/swagger-ui.html` |
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
controller/   Capa de presentación. Expone los servicios REST.
              No contiene reglas de negocio ni SQL.
      |
service/      Capa de negocio. Valida existencias, congela precios,
              calcula totales y orquesta la operación.
      |
repository/   Contratos de persistencia (interfaces).
repository/jdbc/  Implementación con JDBC puro y SQL.
      |
domain/       Entidades y enumeraciones. No conocen la base de datos.
```

Los repositorios se declaran como **interfaces** para que la capa de negocio
dependa del contrato y no de la implementación. Cambiar de motor de base de
datos no obliga a tocar los servicios.

Las entidades del paquete `domain` usan **Lombok** (`@Getter`, `@Setter`,
`@NoArgsConstructor`) para evitar el código repetitivo de los accesores.

`dto/` transporta los datos entre el front end y la API, de modo que las
entidades del dominio no queden expuestas directamente.

`exception/` centraliza el manejo de errores: `ManejadorGlobalErrores`
traduce las excepciones a respuestas HTTP (400, 404, 409, 500) sin exponer
trazas técnicas al cliente.

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
| POST | `/api/productos` | Crear un producto | 201 Created |
| GET | `/api/productos` | Listar el catálogo (filtros: `idCategoria`, `nombre`) | 200 OK |
| GET | `/api/productos/{id}` | Consultar un producto | 200 OK / 404 |
| PUT | `/api/productos/{id}` | Actualizar un producto | 200 OK / 404 |
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

`idAgricultor` e `idCategoria` deben existir en la base de datos.

### Pedido — interacción de negocio

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/pedidos` | Registrar una venta |
| GET | `/api/pedidos/{id}` | Consultar un pedido con sus líneas y su pago |

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

1. Valida que cada producto exista y tenga existencias suficientes.
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

## 8. Códigos de respuesta

| Código | Cuándo |
|---|---|
| 200 | Consulta o actualización correcta |
| 201 | Recurso creado |
| 204 | Baja lógica aplicada |
| 400 | Datos inválidos (validación de entrada) |
| 404 | El recurso no existe |
| 409 | Se violó una regla de negocio (por ejemplo, sin existencias) |
| 500 | Error de persistencia (por ejemplo, MySQL apagado) |

## 9. Probar la API

### 9.1 Swagger UI 

Con la API en ejecución, abrir en el navegador:

- **<http://localhost:8080/swagger-ui.html>**: interfaz para probar los endpoints.
- <http://localhost:8080/v3/api-docs>: especificación OpenAPI en JSON.

En cada endpoint: **Try it out** → completar los parámetros o el JSON →
**Execute**. Swagger muestra la respuesta y el código HTTP.

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

## 10. Estructura del repositorio

```
AgrotechProject/
├── db/                          Scripts SQL de esquema y datos
├── src/main/java/edu/itm/agrotech/
│   ├── AgrotechApplication.java Punto de entrada de Spring Boot
│   ├── controller/              Capa de presentación
│   ├── service/                 Capa de negocio
│   ├── repository/              Contratos de persistencia
│   │   └── jdbc/                Implementación JDBC
│   ├── domain/                  Entidades y enumeraciones
│   ├── dto/                     Objetos de transporte
│   └── exception/               Excepciones y manejador global
├── src/main/resources/
│   └── application.properties   Configuración (BD y puerto)
├── requests.http                Pruebas de la API
├── build.gradle                 Dependencias y configuración de Gradle
└── settings.gradle
```

## 11. Comandos útiles

| Comando | Para qué |
|---|---|
| `./gradlew build` | Compila, ejecuta las pruebas y genera el `.jar` en `build/libs/` |
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




