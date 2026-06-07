-- Amber Film backend database schema
-- Target database: MySQL 8.x
-- Charset: utf8mb4

CREATE DATABASE IF NOT EXISTS amber_film
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE amber_film;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  openid VARCHAR(64) NULL,
  phone VARCHAR(20) NULL,
  nickname VARCHAR(64) NOT NULL DEFAULT '',
  avatar_url VARCHAR(512) NULL,
  real_name VARCHAR(64) NULL,
  id_card_hash VARCHAR(128) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_users_openid (openid),
  UNIQUE KEY uk_users_phone (phone),
  KEY idx_users_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS service_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_service_categories_code (code),
  KEY idx_service_categories_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  storage_provider VARCHAR(32) NOT NULL,
  bucket VARCHAR(128) NOT NULL,
  object_key VARCHAR(512) NOT NULL,
  url VARCHAR(1024) NULL,
  mime_type VARCHAR(128) NULL,
  size_bytes BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_files_provider_bucket_key (storage_provider, bucket, object_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS services (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  cover_file_id BIGINT NULL,
  price_cent INT NOT NULL,
  duration_min INT NOT NULL,
  description TEXT NULL,
  tags_json JSON NULL,
  rating DECIMAL(2,1) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_services_category_enabled (category_id, enabled),
  KEY idx_services_enabled_created (enabled, created_at),
  CONSTRAINT fk_services_category FOREIGN KEY (category_id) REFERENCES service_categories (id),
  CONSTRAINT fk_services_cover_file FOREIGN KEY (cover_file_id) REFERENCES files (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS stores (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  address VARCHAR(255) NOT NULL,
  longitude DECIMAL(10,6) NULL,
  latitude DECIMAL(10,6) NULL,
  hours VARCHAR(64) NULL,
  tags_json JSON NULL,
  cover_file_id BIGINT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_stores_enabled (enabled),
  CONSTRAINT fk_stores_cover_file FOREIGN KEY (cover_file_id) REFERENCES files (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS store_service_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_store_service (store_id, service_id),
  KEY idx_store_service_service (service_id),
  CONSTRAINT fk_store_service_store FOREIGN KEY (store_id) REFERENCES stores (id),
  CONSTRAINT fk_store_service_service FOREIGN KEY (service_id) REFERENCES services (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS schedules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id BIGINT NOT NULL,
  service_id BIGINT NULL,
  service_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  capacity INT NOT NULL DEFAULT 1,
  booked_count INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'available',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_schedule_slot (store_id, service_id, service_date, start_time),
  KEY idx_schedules_query (store_id, service_date, status),
  CONSTRAINT fk_schedules_store FOREIGN KEY (store_id) REFERENCES stores (id),
  CONSTRAINT fk_schedules_service FOREIGN KEY (service_id) REFERENCES services (id),
  CONSTRAINT chk_schedules_capacity CHECK (capacity >= 0),
  CONSTRAINT chk_schedules_booked CHECK (booked_count >= 0 AND booked_count <= capacity)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  schedule_id BIGINT NOT NULL,
  contact_name VARCHAR(64) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  price_cent INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  pay_status VARCHAR(20) NOT NULL DEFAULT 'unpaid',
  appointment_at DATETIME NOT NULL,
  cancelled_at DATETIME NULL,
  user_hidden TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_orders_order_no (order_no),
  KEY idx_orders_user_status_created (user_id, status, created_at),
  KEY idx_orders_schedule (schedule_id),
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_orders_service FOREIGN KEY (service_id) REFERENCES services (id),
  CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES stores (id),
  CONSTRAINT fk_orders_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  channel VARCHAR(20) NOT NULL,
  amount_cent INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  transaction_no VARCHAR(128) NULL,
  paid_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_payments_order (order_id),
  KEY idx_payments_transaction (transaction_no),
  CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS negatives (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  file_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  type VARCHAR(20) NOT NULL DEFAULT 'original',
  status VARCHAR(20) NOT NULL DEFAULT 'visible',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_negatives_user_order (user_id, order_id),
  KEY idx_negatives_status (status),
  CONSTRAINT fk_negatives_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_negatives_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_negatives_file FOREIGN KEY (file_id) REFERENCES files (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS operation_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  actor_user_id BIGINT NULL,
  actor_role VARCHAR(32) NOT NULL DEFAULT 'user',
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id BIGINT NULL,
  detail_json JSON NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operation_logs_target (target_type, target_id),
  KEY idx_operation_logs_actor_created (actor_user_id, created_at),
  CONSTRAINT fk_operation_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
) ENGINE=InnoDB;

INSERT INTO service_categories (code, name, sort_order, enabled)
VALUES
  ('wedding', '婚纱摄影', 10, 1),
  ('portrait', '写真套系', 20, 1),
  ('kids', '儿童摄影', 30, 1),
  ('business', '商务形象', 40, 1),
  ('family', '全家福', 50, 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  sort_order = VALUES(sort_order),
  enabled = VALUES(enabled);

