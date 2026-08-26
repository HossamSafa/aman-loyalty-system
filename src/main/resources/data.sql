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


UPDATE customers
SET mobile_hash = '0895f44c7a43ae484d10b8509021516091079927afc18baa37fad1a70f35c01',
    name = 'Ahmed Mohamed'
WHERE id = 1;

INSERT INTO customers (id, mobile_hash, mobile_encrypted, name, status, created_at)
VALUES
    (2, 'b1033eccac57cabbb1890d06d1459dbbe82f4eff984dfb428d163dd3dd638a5', 'encrypted-02', 'Sara Ali', 'ACTIVE', now()),
    (3, '6386b07d492d714ac407d3f4af867a0bcdc325cb9fe63e58e585c3ce90abfd0', 'encrypted-03', 'Mohamed Hassan', 'ACTIVE', now()),
    (4, '6afb0e2c89ae5b6f0dca979aa91327da9974d47dc320b64c79cdb724c00056d', 'encrypted-04', 'Nour Ahmed', 'ACTIVE', now())
ON CONFLICT (id) DO NOTHING;


UPDATE customers
SET mobile_hash = '0895f44c7a43ae484d10b8509021516091079927afc18baa37fad1a70f35c01d',
    name = 'Ahmed Mohamed'
WHERE id = 1;

UPDATE customers SET mobile_hash = 'b1033eccac57cabbb1890d06d1459dbbe82f4eff984dfb428d163dd3dd638a59' WHERE id = 2;
UPDATE customers SET mobile_hash = '6386b07d492d714ac407d3f4af867a0bcdc325cb9fe63e58e585c3ce90abfd09' WHERE id = 3;
UPDATE customers SET mobile_hash = '6afb0e2c89ae5b6f0dca979aa91327da9974d47dc320b64c79cdb724c00056d7' WHERE id = 4;