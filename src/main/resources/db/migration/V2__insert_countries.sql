INSERT INTO countries (id, name)
VALUES (1, 'Türkiye')
ON CONFLICT (id) DO NOTHING;