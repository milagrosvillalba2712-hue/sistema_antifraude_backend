-- Alineacion final JPA <-> esquema limpio antifraude.
-- Estas columnas no reintroducen tablas legacy; formalizan campos declarados por entidades activas.

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'accion',
        'actuacion',
        'caso',
        'cliente_observado',
        'cliente_pep',
        'comentario_caso',
        'documento',
        'evidencia',
        'historial_estado_caso',
        'lista_regulatoria',
        'perfil_cliente',
        'reglas_riesgo',
        'servicio_externo',
        'sujeto_riesgo',
        'sujeto_riesgo_alias',
        'sujeto_riesgo_documento',
        'sujeto_riesgo_relacion'
    ]
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true', t);
        EXECUTE format('COMMENT ON COLUMN %I.activo IS %L', t, 'Estado funcional usado por las entidades JPA activas; true indica registro vigente.');
    END LOOP;
END $$;

ALTER TABLE caso ADD COLUMN IF NOT EXISTS score integer;
COMMENT ON COLUMN caso.score IS 'Puntaje agregado del caso AML usado para priorizacion operativa y reportes.';

ALTER TABLE empresa ADD COLUMN IF NOT EXISTS email_contacto varchar(180);
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS telefono_contacto varchar(60);
COMMENT ON COLUMN empresa.email_contacto IS 'Correo administrativo de contacto del tenant para comunicaciones de licenciamiento y soporte.';
COMMENT ON COLUMN empresa.telefono_contacto IS 'Telefono administrativo de contacto del tenant para comunicaciones de licenciamiento y soporte.';

ALTER TABLE fuente_datos_riesgo ADD COLUMN IF NOT EXISTS tipo varchar(40);
ALTER TABLE fuente_datos_riesgo ADD COLUMN IF NOT EXISTS cobertura text;
ALTER TABLE fuente_datos_riesgo ADD COLUMN IF NOT EXISTS permite_consumo text;
ALTER TABLE fuente_datos_riesgo ADD COLUMN IF NOT EXISTS permite_edicion text;
ALTER TABLE fuente_datos_riesgo ADD COLUMN IF NOT EXISTS formatos varchar(120);
ALTER TABLE fuente_datos_riesgo ADD COLUMN IF NOT EXISTS recomendacion_bd text;
ALTER TABLE fuente_datos_riesgo ADD COLUMN IF NOT EXISTS fecha_revision date;
COMMENT ON COLUMN fuente_datos_riesgo.cobertura IS 'Alcance geografico, tematico o regulatorio declarado para la fuente de riesgo.';
COMMENT ON COLUMN fuente_datos_riesgo.permite_consumo IS 'Resumen de terminos de uso respecto al consumo de la fuente dentro de Regula AML.';
COMMENT ON COLUMN fuente_datos_riesgo.permite_edicion IS 'Resumen de terminos de uso respecto a transformacion o edicion de datos derivados.';

CREATE TABLE IF NOT EXISTS elemento_lista (
    id bigserial PRIMARY KEY,
    lista_regulatoria_id bigint NOT NULL REFERENCES lista_regulatoria(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    tipo_elemento varchar(20) NOT NULL,
    valor_identificador varchar(150) NOT NULL,
    fecha_incorporacion date NOT NULL DEFAULT CURRENT_DATE,
    fecha_baja date,
    activo boolean NOT NULL DEFAULT true,
    fecha_hora_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_hora_modificacion timestamptz,
    usuario_creacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    usuario_modificacion_id uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE (lista_regulatoria_id, valor_identificador)
);
COMMENT ON TABLE elemento_lista IS 'Elementos simples de listas regulatorias o internas usados por el motor de reglas para coincidencias por persona, entidad, pais, cuenta o documento.';
COMMENT ON COLUMN elemento_lista.lista_regulatoria_id IS 'Lista regulatoria o interna a la que pertenece el elemento.';
COMMENT ON COLUMN elemento_lista.tipo_elemento IS 'Clasificacion del elemento: PERSONA, EMPRESA, PAIS, ENTIDAD, CUENTA o DOCUMENTO.';
COMMENT ON COLUMN elemento_lista.valor_identificador IS 'Valor normalizado usado para comparar contra transacciones, documentos o participantes.';

ALTER TABLE evidencia_alerta ALTER COLUMN evidencia_id DROP NOT NULL;
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS nombre varchar(180);
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS descripcion text;
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS tipo varchar(60);
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS extension varchar(20);
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS mime_type varchar(120);
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS tamano_bytes bigint;
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS hash_archivo varchar(128);
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS referencia_archivo varchar(500);
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS estado varchar(30) DEFAULT 'CARGADA';
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS cargado_por uuid REFERENCES usuarios(id) ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE evidencia_alerta ADD COLUMN IF NOT EXISTS fecha_carga timestamptz DEFAULT now();
COMMENT ON COLUMN evidencia_alerta.descripcion IS 'Descripcion funcional de la evidencia cargada durante la investigacion de la alerta.';
COMMENT ON COLUMN evidencia_alerta.referencia_archivo IS 'Referencia logica o ruta controlada del archivo de evidencia; el binario puede almacenarse fuera de BD.';
