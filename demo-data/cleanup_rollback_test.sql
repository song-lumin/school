USE school_lost_found;
DELETE FROM credit_log WHERE id BETWEEN 145 AND 149;
DELETE FROM dispute WHERE id = 101;
DELETE FROM sys_message WHERE id >= 105;
UPDATE claim_apply SET apply_status = 4, pickup_time = NULL, pickup_photo = NULL,
       pickup_signature = NULL, credit_rollback = 0, credit_issued = 0
 WHERE id = 1;
UPDATE found_item SET item_status = 2, takedown_reason = NULL WHERE id = 9;
UPDATE user SET credit_score = 26 WHERE id = 13;
UPDATE user SET credit_score = 15 WHERE id = 5;
SELECT 'credit_log' AS t, COUNT(*) FROM credit_log
UNION ALL SELECT 'dispute', COUNT(*) FROM dispute
UNION ALL SELECT 'sys_message_max', MAX(id) FROM sys_message
UNION ALL SELECT 'apply1_status', apply_status FROM claim_apply WHERE id = 1
UNION ALL SELECT 'item9_status', item_status FROM found_item WHERE id = 9;
SELECT id, username, credit_score FROM user WHERE id IN (5, 13);
