-- V10'da value_json sütunu raw JSON string olarak eklendi ("My Shop")
-- Doğru format JSON object olmalı: {"value": "My Shop"}
UPDATE site_settings
SET value_json = jsonb_build_object('value', value_json #>> '{}')
WHERE value_json IS NOT NULL
  AND jsonb_typeof(value_json) = 'string';
