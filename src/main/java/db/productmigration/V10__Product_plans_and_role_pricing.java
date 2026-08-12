package db.productmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V10__Product_plans_and_role_pricing extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        MigrationSupport.execute(context, "db/product-support/plans_role_pricing.sql");
    }
}