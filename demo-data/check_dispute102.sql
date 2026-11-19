SELECT id, dispute_type, item_id, apply_id, applicant_id, status, handle_result, point_check_note, point_checker_id, site_unresponsive, created_at FROM dispute WHERE id = 102;
SELECT d.id AS dispute_id, d.item_id, d.apply_id, i.title, i.drop_point_id, i.item_status, i.founder_id, i.actual_founder_id, i.forwarder_id
  FROM dispute d LEFT JOIN found_item i ON i.id = d.item_id WHERE d.id = 102;
SELECT id, name, admin_id, item_status FROM drop_point ORDER BY id;
SELECT id, username, real_name, role FROM user WHERE role = 'POINT_ADMIN' ORDER BY id;
