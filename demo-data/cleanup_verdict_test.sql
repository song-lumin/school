DELETE FROM sys_message WHERE id BETWEEN 199 AND 206;
UPDATE dispute SET status=0, handle_result=NULL, handler_note=NULL, handler_id=NULL, handled_at=NULL,
  point_check_note=NULL, point_checker_id=NULL, point_checked_at=NULL,
  site_unresponsive=0, site_unresponsive_at=NULL WHERE id IN (5,102);
UPDATE found_item SET item_status=2 WHERE id IN (20,100);
UPDATE dispute SET status=0, point_check_note=NULL, point_checker_id=NULL, point_checked_at=NULL WHERE id=3;
SELECT id,status,handle_result,point_checker_id FROM dispute WHERE id IN (3,5,102);
SELECT id,item_status FROM found_item WHERE id IN (20,100);
SELECT MAX(id) AS max_msg FROM sys_message;
