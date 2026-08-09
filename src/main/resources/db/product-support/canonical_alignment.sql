ALTER TABLE pais ALTER COLUMN codigo_iso TYPE varchar(2);
ALTER TABLE pais ALTER COLUMN codigo_iso3 TYPE varchar(3);
ALTER TABLE pais ADD COLUMN IF NOT EXISTS codigo_numerico varchar(3);
ALTER TABLE pais ADD COLUMN IF NOT EXISTS nombre_oficial varchar(220);
ALTER TABLE pais ADD COLUMN IF NOT EXISTS continente varchar(40);
ALTER TABLE pais ADD COLUMN IF NOT EXISTS region varchar(120);
ALTER TABLE pais ADD COLUMN IF NOT EXISTS subregion varchar(120);
ALTER TABLE pais ADD COLUMN IF NOT EXISTS miembro_onu boolean;
ALTER TABLE pais ADD COLUMN IF NOT EXISTS independiente boolean;
ALTER TABLE pais ADD COLUMN IF NOT EXISTS fuente varchar(120);
ALTER TABLE pais ADD COLUMN IF NOT EXISTS fecha_hora_creacion timestamptz NOT NULL DEFAULT now();
ALTER TABLE pais ADD COLUMN IF NOT EXISTS fecha_hora_modificacion timestamptz;
ALTER TABLE pais ADD COLUMN IF NOT EXISTS usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE pais ADD COLUMN IF NOT EXISTS usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE consultas_externas ADD COLUMN IF NOT EXISTS identificador_documento varchar(30);
ALTER TABLE consultas_externas ADD COLUMN IF NOT EXISTS tipo_consulta varchar(50);
ALTER TABLE consultas_externas ADD COLUMN IF NOT EXISTS resultado boolean;

-- Modelo activo consumido por el motor de screening de paises.
ALTER TABLE pais_riesgo
    ADD COLUMN IF NOT EXISTS lista_regulatoria_id bigint,
    ADD COLUMN IF NOT EXISTS nivel_riesgo_id bigint;

ALTER TABLE pais_riesgo
    ALTER COLUMN fecha_inicio SET NOT NULL,
    ALTER COLUMN lista_regulatoria_id SET NOT NULL,
    ALTER COLUMN nivel_riesgo_id SET NOT NULL;

ALTER TABLE pais_riesgo
    ADD CONSTRAINT fk_pais_riesgo_lista_regulatoria
        FOREIGN KEY (lista_regulatoria_id) REFERENCES lista_regulatoria(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT fk_pais_riesgo_nivel_riesgo
        FOREIGN KEY (nivel_riesgo_id) REFERENCES nivel_riesgo(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD CONSTRAINT uq_pais_riesgo_pais_lista_fecha
        UNIQUE (pais_id, lista_regulatoria_id, fecha_inicio);

ALTER TABLE perfil_cliente
    ADD COLUMN IF NOT EXISTS promedio_mensual numeric(18,2),
    ADD COLUMN IF NOT EXISTS horario_habitual_desde time,
    ADD COLUMN IF NOT EXISTS horario_habitual_hasta time,
    ADD COLUMN IF NOT EXISTS ultima_operacion_fecha timestamp(6),
    ADD COLUMN IF NOT EXISTS fecha_calculo timestamp(6) NOT NULL DEFAULT localtimestamp;

ALTER TABLE perfil_usuario
    ADD COLUMN IF NOT EXISTS nombre_visible varchar(150),
    ADD COLUMN IF NOT EXISTS imagen_perfil text,
    ADD COLUMN IF NOT EXISTS estado varchar(30) NOT NULL DEFAULT 'DISPONIBLE',
    ADD COLUMN IF NOT EXISTS estado_personalizado varchar(100),
    ADD COLUMN IF NOT EXISTS ultima_actualizacion_estado timestamp(6),
    ADD COLUMN IF NOT EXISTS fecha_creacion timestamp(6) NOT NULL DEFAULT localtimestamp;

-- Columnas todavía consumidas por el modelo JPA activo. Se mantienen durante la
-- convergencia del dominio para que Hibernate validate sea estricto y reproducible.
ALTER TABLE accion ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE actuacion ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE caso ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE cliente_observado ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE cliente_pep ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE comentario_caso ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE documento ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE evidencia ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE historial_estado_caso ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE lista_regulatoria ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE perfil_cliente ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE servicio_externo ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE sujeto_riesgo ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE sujeto_riesgo_alias ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE sujeto_riesgo_documento ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE sujeto_riesgo_relacion ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;

ALTER TABLE alertas_antifraude
    ADD COLUMN IF NOT EXISTS fecha_asignacion timestamp(6),
    ADD COLUMN IF NOT EXISTS fecha_cierre timestamp(6),
    ADD COLUMN IF NOT EXISTS requiere_aprobacion_supervisor boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS resultado varchar(40);

ALTER TABLE persona
    ADD COLUMN IF NOT EXISTS fecha_nacimiento date,
    ADD COLUMN IF NOT EXISTS nacionalidad_pais_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS primer_apellido varchar(80),
    ADD COLUMN IF NOT EXISTS primer_nombre varchar(80),
    ADD COLUMN IF NOT EXISTS razon_social varchar(200),
    ADD COLUMN IF NOT EXISTS segmento varchar(40),
    ADD COLUMN IF NOT EXISTS segundo_apellido varchar(80),
    ADD COLUMN IF NOT EXISTS segundo_nombre varchar(80);

ALTER TABLE plan_licencia
    ADD COLUMN IF NOT EXISTS limite_consultas_kyc_mensuales integer,
    ADD COLUMN IF NOT EXISTS limite_reportes_mensuales integer,
    ADD COLUMN IF NOT EXISTS limite_transacciones_mensuales integer,
    ADD COLUMN IF NOT EXISTS modulos_incluidos_json jsonb NOT NULL DEFAULT '[]'::jsonb;

CREATE TABLE IF NOT EXISTS producto (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo varchar(20) NOT NULL UNIQUE,
    nombre varchar(80) NOT NULL,
    activo boolean NOT NULL DEFAULT true
);

ALTER TABLE reglas_riesgo
    ADD COLUMN IF NOT EXISTS condicion text,
    ADD COLUMN IF NOT EXISTS creada_por uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS fecha_creacion timestamp(6),
    ADD COLUMN IF NOT EXISTS fecha_modificacion timestamp(6),
    ADD COLUMN IF NOT EXISTS parametros jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS prioridad integer,
    ADD COLUMN IF NOT EXISTS tipo_regla varchar(50);

ALTER TABLE reportes_ros
    ADD COLUMN IF NOT EXISTS alerta_id bigint,
    ADD COLUMN IF NOT EXISTS generado_por uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS nombre_archivo varchar(255);

ALTER TABLE resolucion_alerta
    ADD COLUMN IF NOT EXISTS decision text,
    ADD COLUMN IF NOT EXISTS evidencia_descripcion text,
    ADD COLUMN IF NOT EXISTS fecha_resolucion timestamp(6) NOT NULL DEFAULT localtimestamp,
    ADD COLUMN IF NOT EXISTS movimiento_liberable boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS usuario_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE rol ADD COLUMN IF NOT EXISTS tipo varchar(20) NOT NULL DEFAULT 'EMPRESA';

ALTER TABLE servicio_externo
    ADD COLUMN IF NOT EXISTS reintentos smallint NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS timeout_ms integer NOT NULL DEFAULT 5000;

ALTER TABLE sujeto_riesgo
    ADD COLUMN IF NOT EXISTS categoria varchar(40),
    ADD COLUMN IF NOT EXISTS external_id varchar(160),
    ADD COLUMN IF NOT EXISTS fecha_listado date,
    ADD COLUMN IF NOT EXISTS fecha_revision date,
    ADD COLUMN IF NOT EXISTS fuente_datos_riesgo_id bigint REFERENCES fuente_datos_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS licencia_uso text,
    ADD COLUMN IF NOT EXISTS motivo text,
    ADD COLUMN IF NOT EXISTS nombre_original text,
    ADD COLUMN IF NOT EXISTS programa varchar(160);

ALTER TABLE sujeto_riesgo_alias ADD COLUMN IF NOT EXISTS alias_original text;

ALTER TABLE sujeto_riesgo_documento
    ADD COLUMN IF NOT EXISTS numero_documento varchar(120),
    ADD COLUMN IF NOT EXISTS pais_emision_id bigint REFERENCES pais(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS tipo_documento varchar(60);

ALTER TABLE sujeto_riesgo_relacion
    ADD COLUMN IF NOT EXISTS detalle text,
    ADD COLUMN IF NOT EXISTS nombre_relacionado text,
    ADD COLUMN IF NOT EXISTS relacionado_sujeto_riesgo_id bigint REFERENCES sujeto_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS sujeto_riesgo_id bigint REFERENCES sujeto_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE transaccion_detalle_snapshot
    ADD COLUMN IF NOT EXISTS alerta_id bigint REFERENCES alertas_antifraude(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS detalle_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS fecha_registro timestamp(6) NOT NULL DEFAULT localtimestamp;

ALTER TABLE uso_suscripcion
    ADD COLUMN IF NOT EXISTS anio integer,
    ADD COLUMN IF NOT EXISTS mes integer;

ALTER TABLE usuario_empresa ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS bloqueado_hasta timestamptz,
    ADD COLUMN IF NOT EXISTS intentos_fallidos integer NOT NULL DEFAULT 0;

ALTER TABLE transacciones ALTER COLUMN pan_last4 TYPE varchar(4);

ALTER TABLE disponibilidad_usuario
    ADD COLUMN IF NOT EXISTS fecha_inicio timestamp(6),
    ADD COLUMN IF NOT EXISTS fecha_fin timestamp(6),
    ADD COLUMN IF NOT EXISTS es_programado boolean NOT NULL DEFAULT false;

ALTER TABLE ejecucion_reglas
    ADD COLUMN IF NOT EXISTS regla_id bigint REFERENCES reglas_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS score_regla numeric(8,2),
    ADD COLUMN IF NOT EXISTS condicion_evaluada text,
    ADD COLUMN IF NOT EXISTS tiempo_ejecucion_ms bigint;

ALTER TABLE control_frecuencia
    ADD COLUMN IF NOT EXISTS producto_id bigint REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS unidad_tiempo varchar(10) NOT NULL DEFAULT 'MINUTOS',
    ADD COLUMN IF NOT EXISTS nivel_riesgo_id bigint REFERENCES nivel_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE coincidencia_lista_alerta
    ADD COLUMN IF NOT EXISTS transaccion_id bigint,
    ADD COLUMN IF NOT EXISTS fecha_transaccion timestamp(6),
    ADD COLUMN IF NOT EXISTS fuente_codigo varchar(40),
    ADD COLUMN IF NOT EXISTS parte_transaccion varchar(40),
    ADD COLUMN IF NOT EXISTS campo_evaluado varchar(60),
    ADD COLUMN IF NOT EXISTS valor_evaluado text,
    ADD COLUMN IF NOT EXISTS severidad varchar(20) NOT NULL DEFAULT 'Alta',
    ADD COLUMN IF NOT EXISTS descripcion text;

ALTER TABLE aprobacion_supervisor
    ADD COLUMN IF NOT EXISTS caso_id bigint REFERENCES caso(id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE cliente_observado
    ADD COLUMN IF NOT EXISTS nivel_riesgo_id bigint REFERENCES nivel_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS fecha_inicio date,
    ADD COLUMN IF NOT EXISTS fecha_fin date;

ALTER TABLE cliente_pep
    ADD COLUMN IF NOT EXISTS tipo_documento_id bigint REFERENCES tipo_documento(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS numero_documento varchar(30),
    ADD COLUMN IF NOT EXISTS nivel_riesgo_id bigint REFERENCES nivel_riesgo(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    ADD COLUMN IF NOT EXISTS fuente varchar(30),
    ADD COLUMN IF NOT EXISTS observacion varchar(500);

ALTER TABLE caso
    ADD COLUMN IF NOT EXISTS resultado varchar(30),
    ADD COLUMN IF NOT EXISTS observaciones varchar(1000);

ALTER TABLE actuacion ADD COLUMN IF NOT EXISTS resultado varchar(500);
ALTER TABLE contrato ADD COLUMN IF NOT EXISTS observaciones text;

-- Hibernate no puede combinar IDENTITY con @IdClass. Se conserva la PK
-- compuesta requerida por PostgreSQL, usando una secuencia explícita.
ALTER TABLE transacciones ALTER COLUMN id DROP IDENTITY IF EXISTS;
CREATE SEQUENCE IF NOT EXISTS transacciones_id_seq;
ALTER TABLE transacciones ALTER COLUMN id SET DEFAULT nextval('transacciones_id_seq');
ALTER SEQUENCE transacciones_id_seq OWNED BY transacciones.id;
