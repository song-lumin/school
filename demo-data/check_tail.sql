SELECT id,user_id,change_amount,operation_type,related_item_id,related_apply_id,LEFT(reason,30) FROM credit_log WHERE id >= 158 ORDER BY id;
SELECT id,item_id,claimer_id,apply_status FROM claim_apply WHERE id >= 100 ORDER BY id;
SELECT id,apply_id,dispute_type,status FROM dispute WHERE id >= 100 ORDER BY id;
SELECT id,credit_score FROM user WHERE id IN (4,6);
