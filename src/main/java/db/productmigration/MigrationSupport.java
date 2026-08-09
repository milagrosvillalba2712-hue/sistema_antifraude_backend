package db.productmigration;

import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.sql.Statement;

final class MigrationSupport {

    private MigrationSupport() {
    }

    static void execute(Context context, String resource) throws Exception {
        ClassPathResource sqlResource = new ClassPathResource(resource);
        String sql = sqlResource.getContentAsString(StandardCharsets.UTF_8);
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute(sql);
        }
    }
}
