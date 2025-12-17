CREATE TABLE users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    activated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE cities (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE districts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(55) NOT NULL,
    city_id INTEGER NOT NULL,
    CONSTRAINT fk_districts_cities FOREIGN KEY (city_id) REFERENCES cities (id)
);

CREATE TABLE addresses (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(50) NOT NULL,
    city_id INTEGER NOT NULL,
    district_id INTEGER NOT NULL,
    neighborhood VARCHAR(100) NOT NULL,
    full_address TEXT NOT NULL,
    zip_code VARCHAR(20),
    contact_name VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_addresses_users FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_addresses_cities FOREIGN KEY (city_id) REFERENCES cities (id),
    CONSTRAINT fk_addresses_districts FOREIGN KEY (district_id) REFERENCES districts (id)
);