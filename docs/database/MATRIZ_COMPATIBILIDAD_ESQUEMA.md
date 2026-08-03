# Matriz de compatibilidad aplicación–esquema

| Release de aplicación | Java | PostgreSQL | Flyway | Esquema mínimo | Esquema objetivo | Upgrade soportado |
|---|---:|---:|---:|---:|---:|---|
| 1.0.0 académico | 17 | 16 | 10.8.1 | V4 | V5 | V4 a V5 y base vacía a V5 |

## Política

- Flyway es el único ejecutor DDL en runtime.
- La aplicación inicia con `ddl-auto=validate` y falla ante drift.
- Las migraciones aplicadas no se modifican; toda corrección crea una versión posterior.
- PostgreSQL se respalda antes de actualizar. El rollback de esquema no se automatiza: se restaura el backup o se aplica un roll-forward probado.
- Los datos demo se cargan únicamente con el perfil `demo` desde `db/demo`.
- Cada release debe actualizar esta matriz y conservar una prueba desde su esquema mínimo soportado.

