SELECT id, title, item_status, drop_point_id, founder_id FROM found_item WHERE item_status = 1 ORDER BY id;
SELECT DISTINCT related_item_id FROM credit_log WHERE operation_type = 'PICKUP_ISSUE' ORDER BY related_item_id;
SELECT id, item_id, claimer_id, apply_status FROM claim_apply ORDER BY id DESC LIMIT 6;
SELECT id, apply_id, item_id, dispute_type, status FROM dispute ORDER BY id DESC LIMIT 4;
