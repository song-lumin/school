SELECT id, user_id, title, created_at FROM sys_message ORDER BY id DESC LIMIT 8;
SELECT id,item_id,dispute_type,status,handle_result,point_check_note IS NULL AS note_null, point_checker_id, site_unresponsive FROM dispute WHERE id IN (3,5,102);
SELECT id,item_status FROM found_item WHERE id IN (20,100);
