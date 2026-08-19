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


INSERT INTO loyalty_transactions (id, account_id, type, source_transaction_id, points, status, transaction_time, created_at)
VALUES (1, 1, 'EARN', 'sale-seed-001', 2500, 'COMMITTED', now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO points_lots (id, account_id, earning_transaction_id, original_points, remaining_points, unlock_at, expires_at, status, version, created_at)
VALUES (1, 1, 1, 2500, 2500, now() - interval '1 day', now() + interval '360 days', 'AVAILABLE', 0, now())
ON CONFLICT (id) DO NOTHING;