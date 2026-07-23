-- V2__seed_admin_user.sql
-- Seeds one ADMIN user for local testing/demo purposes.
-- Username: admin   Password: admin123
-- (BCrypt hash below was generated offline — never generate real credentials this way in production.)
INSERT INTO users (username, password_hash, role)
VALUES ('admin', '$2b$10$HKePPMU6On4IZ8LLhegV4eLrsopaX3tUewQ2c1UykosiLnpEyyWkm', 'ADMIN')
ON CONFLICT (username) DO NOTHING;
