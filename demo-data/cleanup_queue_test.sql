DELETE FROM dispute WHERE id IN (104, 105);
DELETE FROM credit_log WHERE id >= 158;
DELETE FROM claim_apply WHERE id IN (102, 103, 104, 105);
UPDATE found_item SET item_status = 1, claimed_at = NULL WHERE id IN (5, 6);
UPDATE user SET credit_score = 15, misclaim_count = 1 WHERE id = 5;
UPDATE user SET credit_score = 13 WHERE id = 6;
UPDATE user SET credit_score = 23 WHERE id = 4;

SELECT 'credit_log_max' AS k, MAX(id) AS v FROM credit_log
UNION ALL SELECT 'dispute_cnt', COUNT(*) FROM dispute
UNION ALL SELECT 'apply_cnt', COUNT(*) FROM claim_apply;
SELECT id, item_status FROM found_item WHERE id IN (5, 6, 9);
SELECT id, username, credit_score, misclaim_count FROM user WHERE id IN (4, 5, 6, 13);
