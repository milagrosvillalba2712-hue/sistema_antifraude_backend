package db.productmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V12__Auth_user_lifecycle extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        MigrationSupport.execute(context, "db/product-support/auth_user_lifecycle.sql");
    }
}