CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    intentos_fallidos INT DEFAULT 0,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reglas_riesgo (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    tipo_regla VARCHAR(50),
    severidad VARCHAR(20),
    condicion TEXT NOT NULL,
    activa BOOLEAN DEFAULT TRUE,
    creada_por BIGINT REFERENCES usuarios(id),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transacciones (
    id BIGSERIAL PRIMARY KEY,
    transaction_uuid UUID NOT NULL,
    identificador_documento VARCHAR(30),
    cuenta_origen TEXT NOT NULL,
    cuenta_destino TEXT NOT NULL,
    monto NUMERIC(18,2) NOT NULL,
    moneda VARCHAR(10),
    canal VARCHAR(30),
    tipo_transaccion VARCHAR(50),
    ip_origen VARCHAR(100),
    pais_origen VARCHAR(100),
    fecha_transaccion TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    score_riesgo NUMERIC(5,2),
    procesada BOOLEAN DEFAULT FALSE,
    fecha_procesamiento TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transacciones_documento ON transacciones(identificador_documento);
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha ON transacciones(fecha_transaccion);
CREATE INDEX IF NOT EXISTS idx_transacciones_score ON transacciones(score_riesgo);

CREATE TABLE IF NOT EXISTS alertas (
    id BIGSERIAL PRIMARY KEY,
    transaccion_id BIGINT REFERENCES transacciones(id),
    regla_id BIGINT REFERENCES reglas_riesgo(id),
    prioridad VARCHAR(20),
    estado VARCHAR(30),
    observacion TEXT,
    asignado_a BIGINT REFERENCES usuarios(id),
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id),
    accion VARCHAR(100) NOT NULL,
    descripcion TEXT,
    direccion_ip VARCHAR(100),
    entidad_afectada VARCHAR(100),
    entidad_id BIGINT,
    fecha_evento TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS estadisticas_cliente (
    id BIGSERIAL PRIMARY KEY,
    identificador_documento VARCHAR(30) UNIQUE,
    monto_promedio_semanal NUMERIC(18,2),
    frecuencia_diaria NUMERIC(10,2),
    horario_habitual_inicio TIME,
    horario_habitual_fin TIME,
    ultima_actualizacion TIMESTAMP
);

CREATE TABLE IF NOT EXISTS consultas_externas (
    id BIGSERIAL PRIMARY KEY,
    identificador_documento VARCHAR(30),
    tipo_consulta VARCHAR(50),
    resultado BOOLEAN,
    usuario_id BIGINT REFERENCES usuarios(id),
    fecha_consulta TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reportes_ros (
    id BIGSERIAL PRIMARY KEY,
    alerta_id BIGINT REFERENCES alertas(id),
    generado_por BIGINT REFERENCES usuarios(id),
    nombre_archivo VARCHAR(255),
    fecha_generacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
