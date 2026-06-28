-- V3: Add new tables for profile, availability, assignment history, and workload stats

-- 1. perfil_usuario
CREATE TABLE IF NOT EXISTS perfil_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT UNIQUE NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre_visible VARCHAR(150),
    imagen_perfil TEXT,
    estado VARCHAR(30) DEFAULT 'DISPONIBLE',
    estado_personalizado VARCHAR(100),
    ultima_actualizacion_estado TIMESTAMP,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. disponibilidad_usuario
CREATE TABLE IF NOT EXISTS disponibilidad_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo_estado VARCHAR(50) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP,
    es_programado BOOLEAN DEFAULT FALSE,
    motivo TEXT,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_disponibilidad_usuario ON disponibilidad_usuario(usuario_id, activo);

-- 3. historial_asignacion
CREATE TABLE IF NOT EXISTS historial_asignacion (
    id BIGSERIAL PRIMARY KEY,
    alerta_id BIGINT NOT NULL REFERENCES alertas(id) ON DELETE CASCADE,
    usuario_origen BIGINT REFERENCES usuarios(id),
    usuario_destino BIGINT NOT NULL REFERENCES usuarios(id),
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    motivo TEXT,
    tipo VARCHAR(30) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_historial_asignacion_alerta ON historial_asignacion(alerta_id);

-- 4. estadistica_carga_analista
CREATE TABLE IF NOT EXISTS estadistica_carga_analista (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    alertas_asignadas INT DEFAULT 0,
    alertas_resueltas INT DEFAULT 0,
    alertas_pendientes INT DEFAULT 0,
    tiempo_promedio_resolucion BIGINT,
    ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(usuario_id, fecha)
);
CREATE INDEX IF NOT EXISTS idx_carga_analista_fecha ON estadistica_carga_analista(usuario_id, fecha);

-- 5. Add fecha_asignacion to alertas
ALTER TABLE alertas ADD COLUMN IF NOT EXISTS fecha_asignacion TIMESTAMP;
