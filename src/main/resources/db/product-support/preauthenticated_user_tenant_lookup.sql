DROP POLICY IF EXISTS tenant_isolation_usuario_empresa ON usuario_empresa;
CREATE POLICY tenant_isolation_usuario_empresa ON usuario_empresa
USING (
    empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid
    OR usuario_id = nullif(current_setting('app.current_usuario_id', true), '')::uuid
)
WITH CHECK (
    empresa_id = nullif(current_setting('app.current_empresa_id', true), '')::uuid
);

COMMENT ON POLICY tenant_isolation_usuario_empresa ON usuario_empresa IS
    'Permite descubrir el tenant solo de un usuario ya autenticado; las escrituras siempre requieren empresa_id.';
