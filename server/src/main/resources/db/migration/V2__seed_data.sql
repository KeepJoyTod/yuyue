INSERT INTO service_categories (id, code, name, sort_order, enabled) VALUES
  (1, 'wedding', '婚纱摄影', 10, TRUE),
  (2, 'portrait', '写真套系', 20, TRUE),
  (3, 'kids', '儿童摄影', 30, TRUE),
  (4, 'business', '商务形象', 40, TRUE),
  (5, 'family', '全家福', 50, TRUE);

INSERT INTO services (id, category_id, name, cover_url, price_cent, duration_min, description, tags_json, rating, enabled) VALUES
  (1, 1, '梦境 · 白纱系列', 'https://picsum.photos/id/91/750/500', 498000, 180, '经典白纱 + 轻法式氛围，含造型与场景搭配建议，适合新人首选。', '["婚纱摄影","白纱","高口碑"]', 4.9, TRUE),
  (2, 2, '琥珀 · 轻奢写真', 'https://picsum.photos/id/64/750/500', 228000, 120, '轻奢质感棚拍路线，妆发更精致，适合日常纪念与个人写真。', '["写真套系","轻奢","氛围感"]', 4.8, TRUE),
  (3, 3, '亲子时光', 'https://picsum.photos/id/1025/750/500', 198000, 120, '亲子互动引导更自然，记录温暖瞬间，适合家庭日常纪念。', '["儿童摄影","亲子","温馨"]', 4.9, TRUE);

INSERT INTO stores (id, name, address, distance_km, rating, reviews, hours, tags_json, cover_url, enabled) VALUES
  (1, '琥珀映画·静安旗舰店', '静安区南京西路 1168 号嘉里中心 3F', 0.8, 4.9, 2341, '10:00-21:00', '["婚纱","写真","儿童"]', 'https://picsum.photos/id/325/750/500', TRUE),
  (2, '琥珀映画·陆家嘴店', '浦东新区世纪大道 88 号金茂大厦 L2', 2.3, 4.8, 1680, '10:00-21:00', '["婚纱","写真"]', 'https://picsum.photos/id/369/750/500', TRUE);

INSERT INTO store_service_rel (store_id, service_id, enabled) VALUES
  (1, 1, TRUE),
  (1, 2, TRUE),
  (1, 3, TRUE),
  (2, 1, TRUE),
  (2, 2, TRUE);

INSERT INTO schedules (id, store_id, service_id, service_date, start_time, end_time, capacity, booked_count, status) VALUES
  (1, 1, 1, DATE '2026-06-08', TIME '10:00:00', TIME '10:30:00', 2, 0, 'available'),
  (2, 1, 1, DATE '2026-06-08', TIME '14:00:00', TIME '14:30:00', 2, 0, 'available'),
  (3, 1, 1, DATE '2026-06-09', TIME '19:00:00', TIME '19:30:00', 1, 0, 'available'),
  (4, 1, 2, DATE '2026-06-08', TIME '10:30:00', TIME '11:00:00', 2, 0, 'available'),
  (5, 1, 2, DATE '2026-06-09', TIME '15:00:00', TIME '15:30:00', 2, 0, 'available'),
  (6, 1, 3, DATE '2026-06-10', TIME '14:30:00', TIME '15:00:00', 2, 0, 'available'),
  (7, 2, 1, DATE '2026-06-08', TIME '11:00:00', TIME '11:30:00', 1, 0, 'available'),
  (8, 2, 2, DATE '2026-06-09', TIME '19:30:00', TIME '20:00:00', 1, 0, 'available');

