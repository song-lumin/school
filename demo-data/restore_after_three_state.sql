UPDATE dispute SET status=0, handle_result=NULL, handler_note=NULL, handler_id=NULL, handled_at=NULL,
  point_check_note=NULL, point_checker_id=NULL, point_checked_at=NULL,
  site_unresponsive=0, site_unresponsive_at=NULL
 WHERE id IN (3,5,102);
UPDATE found_item SET item_status=2 WHERE id IN (20,100);
DELETE FROM credit_log WHERE id > 157;
UPDATE user SET credit_score = 15 WHERE id = 5;
DELETE FROM sys_message WHERE id > 198;
SELECT id, dispute_type, status, handle_result, point_checker_id FROM dispute WHERE id IN (3,5,102);
SELECT id, item_status FROM found_item WHERE id IN (20,100);
SELECT MAX(id) AS max_credit FROM credit_log;
SELECT MAX(id) AS max_msg FROM sys_message;
SELECT id, credit_score FROM user WHERE id=5;
