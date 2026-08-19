package db.productmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V18__Rls_empty_tenant_hardening extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        MigrationSupport.execute(context, "db/product-support/rls_empty_tenant_hardening.sql");
    }
}
