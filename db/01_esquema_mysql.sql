-- ============================================================
-- AgroTech - Esquema de base de datos (MySQL 8)
-- ============================================================

CREATE DATABASE IF NOT EXISTS agrotech
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agrotech;

CREATE TABLE usuario (
    id_usuario     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    correo         VARCHAR(120) NOT NULL UNIQUE,
    clave_hash     VARCHAR(255) NOT NULL,
    telefono       VARCHAR(20),
    rol            VARCHAR(20)  NOT NULL,
    fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_usuario_rol
        CHECK (rol IN ('AGRICULTOR','CLIENTE','TRANSPORTADOR','ADMIN'))
) ENGINE = InnoDB;

CREATE TABLE agricultor (
    id_agricultor BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario    BIGINT       NOT NULL UNIQUE,
    nombre_finca  VARCHAR(120) NOT NULL,
    municipio     VARCHAR(80)  NOT NULL,
    vereda        VARCHAR(80),
    CONSTRAINT fk_agricultor_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE = InnoDB;

CREATE TABLE cliente (
    id_cliente BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario BIGINT NOT NULL UNIQUE,
    direccion  VARCHAR(200),
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE = InnoDB;

CREATE TABLE transportador (
    id_transportador BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario       BIGINT      NOT NULL UNIQUE,
    placa            VARCHAR(10) NOT NULL,
    tipo_vehiculo    VARCHAR(40),
    CONSTRAINT fk_transportador_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
) ENGINE = InnoDB;

CREATE TABLE categoria (
    id_categoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(60) NOT NULL UNIQUE
) ENGINE = InnoDB;

CREATE TABLE producto (
    id_producto   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_agricultor BIGINT        NOT NULL,
    id_categoria  BIGINT        NOT NULL,
    nombre        VARCHAR(120)  NOT NULL,
    descripcion   VARCHAR(500),
    precio        DECIMAL(12,2) NOT NULL,
    unidad_medida VARCHAR(20)   NOT NULL,
    stock         DECIMAL(12,2) NOT NULL DEFAULT 0,
    disponible    BOOLEAN       NOT NULL DEFAULT TRUE,
    foto_url      VARCHAR(300),
    CONSTRAINT fk_producto_agricultor FOREIGN KEY (id_agricultor) REFERENCES agricultor (id_agricultor),
    CONSTRAINT fk_producto_categoria  FOREIGN KEY (id_categoria)  REFERENCES categoria (id_categoria),
    CONSTRAINT ck_producto_precio CHECK (precio > 0),
    CONSTRAINT ck_producto_stock  CHECK (stock >= 0)
) ENGINE = InnoDB;

CREATE TABLE pedido (
    id_pedido   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_cliente  BIGINT        NOT NULL,
    fecha       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado      VARCHAR(20)   NOT NULL DEFAULT 'CREADO',
    subtotal    DECIMAL(12,2) NOT NULL DEFAULT 0,
    costo_envio DECIMAL(12,2) NOT NULL DEFAULT 0,
    total       DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT ck_pedido_estado
        CHECK (estado IN ('CREADO','PAGADO','EN_RUTA','ENTREGADO','CANCELADO'))
) ENGINE = InnoDB;

CREATE TABLE detalle_pedido (
    id_detalle      BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_pedido       BIGINT        NOT NULL,
    id_producto     BIGINT        NOT NULL,
    cantidad        DECIMAL(12,2) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_detalle_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedido (id_pedido),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0)
) ENGINE = InnoDB;

CREATE TABLE pago (
    id_pago             BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_pedido           BIGINT        NOT NULL UNIQUE,
    metodo              VARCHAR(20)   NOT NULL,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    referencia_pasarela VARCHAR(80),
    monto               DECIMAL(12,2) NOT NULL,
    fecha_pago          DATETIME,
    CONSTRAINT fk_pago_pedido FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT ck_pago_metodo CHECK (metodo IN ('PSE','TARJETA','CONTRA_ENTREGA')),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO'))
) ENGINE = InnoDB;

CREATE TABLE entrega (
    id_entrega       BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_pedido        BIGINT       NOT NULL UNIQUE,
    id_transportador BIGINT,
    direccion        VARCHAR(200) NOT NULL,
    estado           VARCHAR(20)  NOT NULL DEFAULT 'PROGRAMADA',
    fecha_programada DATE,
    fecha_entrega    DATE,
    CONSTRAINT fk_entrega_pedido        FOREIGN KEY (id_pedido)        REFERENCES pedido (id_pedido),
    CONSTRAINT fk_entrega_transportador FOREIGN KEY (id_transportador) REFERENCES transportador (id_transportador),
    CONSTRAINT ck_entrega_estado
        CHECK (estado IN ('PROGRAMADA','EN_RUTA','ENTREGADA','FALLIDA'))
) ENGINE = InnoDB;

CREATE TABLE resena (
    id_resena    BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_cliente   BIGINT       NOT NULL,
    id_producto  BIGINT       NOT NULL,
    calificacion INT          NOT NULL,
    comentario   VARCHAR(500),
    fecha        DATE         NOT NULL,
    CONSTRAINT fk_resena_cliente  FOREIGN KEY (id_cliente)  REFERENCES cliente (id_cliente),
    CONSTRAINT fk_resena_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT ck_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5),
    CONSTRAINT uq_resena_cliente_producto UNIQUE (id_cliente, id_producto)
) ENGINE = InnoDB;

CREATE INDEX ix_producto_agricultor ON producto (id_agricultor);
CREATE INDEX ix_producto_categoria  ON producto (id_categoria);
CREATE INDEX ix_pedido_cliente      ON pedido (id_cliente);
CREATE INDEX ix_detalle_pedido      ON detalle_pedido (id_pedido);
