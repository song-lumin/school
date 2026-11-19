UPDATE dispute SET point_check_note=NULL, point_checker_id=NULL, point_checked_at=NULL WHERE id=3;
SELECT id, user_id, title FROM sys_message ORDER BY id DESC LIMIT 5;
SELECT id,status,point_checker_id FROM dispute WHERE id=3;
