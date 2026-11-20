// 애플리케이션을 기동하지 않고 동일 의존성과 SQL로 배포 대상 DB를 마이그레이션한다.
package com.sopt.nearby.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;

public final class DatabaseMigration {
    private DatabaseMigration() {
    }

    public static void main(String[] args) {
        String url = required("FLYWAY_URL");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("FLYWAY_URL must use PostgreSQL JDBC");
        }
        Flyway flyway = Flyway.configure()
                .dataSource(url, required("FLYWAY_USER"), required("FLYWAY_PASSWORD"))
                .locations("classpath:db/migration", "classpath:db/migration-postgresql")
                .schemas("public")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .outOfOrder(false)
                .validateOnMigrate(true)
                // 새 migration은 허용하되 현재 코드에 없는 미래 버전은 무시하지 않는다.
                .ignoreMigrationPatterns("*:pending")
                .connectRetries(3)
                .load();
        for (MigrationInfo info : flyway.info().all()) {
            System.out.printf("%s %s %s%n", info.getVersion(), info.getDescription(), info.getState());
        }
        flyway.validate();
        var result = flyway.migrate();
        System.out.printf("Migration completed: count=%d, schemaVersion=%s%n",
                result.migrationsExecuted, result.targetSchemaVersion);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing environment variable: " + name);
        }
        return value;
    }
}
