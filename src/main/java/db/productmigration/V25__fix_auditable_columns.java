package db.productmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V25__fix_auditable_columns extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        MigrationSupport.execute(context, "db/product-support/v25_fix_auditable_columns.sql");
    }
}
