package db.productmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V36__evidencia_archivo_snapshot_unico extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        MigrationSupport.execute(context, "db/product-support/v36_evidencia_archivo_snapshot_unico.sql");
    }
}
