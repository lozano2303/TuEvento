-- Siembra los usuarios de prueba (idempotente) y devuelve sus ids separados por coma.
-- Variables psql: :usuarios
INSERT INTO app_user (user_status_id, role_id, alias, activated, created_at)
SELECT (SELECT user_status_id FROM user_status WHERE code = 'ACTIVE'),
       (SELECT role_id FROM role WHERE code = 'USER'),
       'loadtest_' || lpad(n::text, 3, '0'),
       true,
       now()
FROM generate_series(1, :usuarios) AS n
WHERE NOT EXISTS (
    SELECT 1 FROM app_user u WHERE u.alias = 'loadtest_' || lpad(n::text, 3, '0')
);

SELECT string_agg(user_id::text, ',' ORDER BY user_id)
FROM (
    SELECT user_id FROM app_user
    WHERE alias LIKE 'loadtest\_%'
    ORDER BY user_id
    LIMIT :usuarios
) t;
