-- Seed: usuarios iniciales
-- Password hash de "password" (BCrypt)
INSERT INTO usuarios (nombre, email, password_hash, rol, activo)
SELECT 'Admin', 'admin@antifraude.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMINISTRADOR', true
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'admin@antifraude.com');

INSERT INTO usuarios (nombre, email, password_hash, rol, activo)
SELECT 'Analista', 'analista@antifraude.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ANALISTA', true
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE email = 'analista@antifraude.com');
