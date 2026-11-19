SELECT id,item_id,dispute_type,status,handle_result,point_checker_id,handler_id,LEFT(point_check_note,20) AS note FROM dispute WHERE id IN (3,5,102);
SELECT id,item_status FROM found_item WHERE id IN (20,100);
SELECT id,user_id,LEFT(title,40) AS t FROM sys_message ORDER BY id DESC LIMIT 8;
