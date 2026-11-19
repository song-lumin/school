SELECT id, user_id, change_amount, operation_type, related_item_id, related_apply_id
FROM credit_log WHERE operation_type = 'CHECK_ISSUE' ORDER BY id;
SELECT id, item_status, drop_point_id, founder_id FROM found_item WHERE id IN (1,2,3,4,5,7,8);
