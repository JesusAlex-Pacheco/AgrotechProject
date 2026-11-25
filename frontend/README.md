# AgroTech - Frontend

Interfaz web de AgroTech, construida sobre la plantilla gratuita
[Mantis](https://github.com/codedthemes/mantis-free-react-admin-template)
(React 19, Vite y Material UI).

## Requisitos

- Node.js 22 o superior (incluye npm).
- El backend Spring Boot corriendo en `http://localhost:8080`.

## Cómo ejecutarlo

Lo más fácil en Windows: doble clic en `iniciar.bat` (raíz del proyecto), que
arranca el backend y luego el frontend. A mano:

```bash
cd frontend
npm install     # solo la primera vez, o cuando cambie package.json
npm start       # abre http://localhost:3000 (puerto: FRONTEND_PORT en .env)
```

En otra terminal, desde la raíz del proyecto, se levanta el backend:

```bash
./gradlew bootRun
```

Otros comandos:

| Comando | Qué hace |
|---|---|
| `npm run build` | Genera la versión de producción en `dist/` |
| `npm run lint` | Revisa el código con ESLint |
| `npm test` | Ejecuta las pruebas (archivos `*.test.mjs`) |
| `npm run imagenes` | Regenera las versiones livianas de los logos y la imagen del inicio (ver abajo) |

## Pruebas

Se usan las herramientas de prueba que ya trae Node (`node:test`), sin
instalar nada adicional. No necesitan el backend: `fetch` se reemplaza por
uno simulado.

| Archivo | Qué verifica |
|---|---|
| `src/api/agrotech.test.mjs` | El token viaja en cada petición, una sesión vencida (401) se cierra y la consulta se repite sin token, y los errores del backend llegan con su mensaje |
| `src/utils/utils.test.mjs` | Formato de pesos, cantidades y fechas (sin correrse al día anterior), promedio de reseñas y permisos por rol |

## Conexión con el backend

En desarrollo, Vite reenvía todas las peticiones que empiezan por `/api` al
backend (ver `proxy` en `vite.config.mjs`). Así el navegador ve un solo
origen y no hace falta configurar CORS en Spring Boot.

La dirección del backend se puede cambiar con la variable `VITE_BACKEND_URL`
en el archivo `.env`.

## Estructura

| Carpeta | Contenido |
|---|---|
| `src/api/agrotech.js` | Llamadas al backend y hooks de datos (SWR) |
| `src/pages/` | Pantallas: `inicio`, `productos`, `categorias`, `carrito`, `pedidos`, `entregas`, `resenas` y `auth` (iniciar sesión y registro) |
| `src/sections/` | Partes de cada pantalla (formularios, diálogos, filas) |
| `src/utils/` | Formato de pesos y fechas, permisos por rol |
| `src/menu-items/` | Opciones del menú lateral |
| `src/routes/MainRoutes.jsx` | Rutas de las pantallas |
| `src/components/` | Componentes reutilizables (`MainCard`, `FotoProducto`, logo, etc.) |

## Logos e imágenes

Los originales están en `src/assets/images`: `logo-claro.png`, `logo-oscuro.png`,
`inicio.png`, `Icono-claro.png` e `Icono-oscuro.png`. La aplicación no los usa
directamente porque pesan más de 1 MB cada uno; usa versiones livianas
generadas a partir de ellos:

| Archivo generado | Dónde se ve |
|---|---|
| `src/assets/images/logo-claro-app.webp` / `logo-oscuro-app.webp` | Menú lateral abierto, iniciar sesión y registro |
| `src/assets/images/inicio-app.webp` | Imagen grande de la página de inicio |
| `favicon-claro.png` / `favicon-oscuro.png` | Pestaña del navegador |

Si cambian alguno de los originales (mismo nombre de archivo), regeneren las
versiones livianas con:

```bash
npm run imagenes
```

El script (`scripts/optimizar-imagenes.mjs`) quita el fondo blanco de los
logos y de la imagen del inicio y las esquinas negras de los íconos, borra
restos sueltos en los bordes, recorta los márgenes y reduce el tamaño. Guarda
en WebP, que conserva la transparencia y pesa una fracción del PNG. Usa la
librería `sharp` (dependencia de desarrollo) y funciona en Windows, Mac y Linux.

El ícono del menú cerrado (la "o" con hoja del logo) no es una imagen: está
dibujado en SVG en `src/components/logo/LogoIcon.jsx`.

## Nota sobre npm

La plantilla original usa Yarn. Aquí se usa npm, y el archivo `.npmrc`
(`legacy-peer-deps=true`) evita un conflicto de versiones entre ESLint y uno
de sus complementos al instalar.
