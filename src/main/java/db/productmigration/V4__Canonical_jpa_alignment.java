package db.productmigration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V4__Canonical_jpa_alignment extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        MigrationSupport.execute(context, "db/clean-schema/V17__clean_jpa_alignment.sql");
        MigrationSupport.execute(context, "db/product-support/canonical_alignment.sql");
    }
}
