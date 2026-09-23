-- ============================================================
-- Datos minimos para probar la API
-- ============================================================

USE agrotech;

INSERT INTO usuario (nombre, correo, clave_hash, telefono, rol) VALUES
    ('Hernando Restrepo', 'hernando@agrotech.co', '$2a$10$demoHashNoUsarEnProduccion', '3001112233', 'AGRICULTOR'),
    ('Laura Gomez',       'laura@agrotech.co',    '$2a$10$demoHashNoUsarEnProduccion', '3004445566', 'CLIENTE');

INSERT INTO agricultor (id_usuario, nombre_finca, municipio, vereda) VALUES
    (1, 'Finca La Esperanza', 'Medellin', 'San Antonio de Prado');

INSERT INTO cliente (id_usuario, direccion) VALUES
    (2, 'Calle 50 # 40-20, Itagui');

INSERT INTO categoria (nombre) VALUES
    ('Frutas'), ('Verduras'), ('Tuberculos'), ('Hierbas');

INSERT INTO producto (id_agricultor, id_categoria, nombre, descripcion, precio, unidad_medida, stock, disponible) VALUES
    (1, 1, 'Aguacate Hass',  'Cosechado en San Antonio de Prado', 3000.00, 'kg', 120.00, TRUE),
    (1, 2, 'Cilantro',       'Manojo fresco',                      1500.00, 'manojo', 60.00, TRUE),
    (1, 3, 'Papa criolla',   'Recien cosechada',                   2800.00, 'kg',  80.00, TRUE);
