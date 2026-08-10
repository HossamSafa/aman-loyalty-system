-- Test data (Freeze/Unfreeze) manual testing

INSERT INTO customers (id, mobile_hash, mobile_encrypted, name, status, created_at)
VALUES (1, 'test-hash-01', 'encrypted-mobile-01', 'Ahmed Test Customer', 'ACTIVE', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO loyalty_programs (id, merchant_id, name, currency, lock_days, expiry_days, status, created_at)
VALUES (1, 'merchant-001', 'Test Loyalty Program', 'EGP', 30, 360, 'ACTIVE', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO loyalty_accounts (id, program_id, customer_id, available_points, locked_points, reserved_points, status, version, created_at, updated_at)
VALUES (1, 1, 1, 2500, 1000, 0, 'ACTIVE', 0, now(), now())
ON CONFLICT (id) DO NOTHING;