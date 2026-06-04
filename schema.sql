-- =====================================================
-- Pass The Paper - Database Schema (DDL)
-- Database: MySQL 8.0+
-- Run this in MySQL Workbench before starting the app
-- =====================================================

CREATE DATABASE IF NOT EXISTS passthepaper CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE passthepaper;

-- =====================================================
-- TABLE: users
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id                  CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
    email               VARCHAR(255)    NOT NULL UNIQUE,
    password_hash       VARCHAR(255)    NOT NULL,
    name                VARCHAR(150)    NOT NULL,
    university          VARCHAR(255)    NOT NULL,
    student_id          VARCHAR(100),
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE,
    is_admin            BOOLEAN         NOT NULL DEFAULT FALSE,
    is_banned           BOOLEAN         NOT NULL DEFAULT FALSE,
    ban_reason          TEXT,
    wallet_balance      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    pending_balance     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    reward_points       INT             NOT NULL DEFAULT 0,
    membership_type     ENUM('free','premium_monthly','premium_yearly') NOT NULL DEFAULT 'free',
    membership_expiry   DATETIME,
    profile_picture     MEDIUMTEXT,
    id_card_image       MEDIUMTEXT,
    id_card_status      ENUM('none','pending','approved','rejected') NOT NULL DEFAULT 'none',
    seller_rating       DECIMAL(3, 2)   DEFAULT 0.00,
    total_ratings       INT             NOT NULL DEFAULT 0,
    can_upload          BOOLEAN         NOT NULL DEFAULT TRUE,
    can_purchase        BOOLEAN         NOT NULL DEFAULT TRUE,
    can_comment         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email       ON users (email);
CREATE INDEX idx_users_is_verified ON users (is_verified);
CREATE INDEX idx_users_is_admin    ON users (is_admin);
CREATE INDEX idx_users_is_banned   ON users (is_banned);

-- =====================================================
-- TABLE: resources
-- =====================================================
CREATE TABLE IF NOT EXISTS resources (
    id              CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    category        VARCHAR(100)    NOT NULL,
    price           DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    price_type      ENUM('money','points') NOT NULL DEFAULT 'money',
    uploaded_by     CHAR(36)        NOT NULL,
    uploader_name   VARCHAR(150)    NOT NULL,
    downloads       INT             NOT NULL DEFAULT 0,
    rating          DECIMAL(3, 2)   NOT NULL DEFAULT 0.00,
    status          ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    file_url        TEXT            NOT NULL,
    department      VARCHAR(150),
    course          VARCHAR(100),
    semester        VARCHAR(100),
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_resources_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_resources_status      ON resources (status);
CREATE INDEX idx_resources_category    ON resources (category);
CREATE INDEX idx_resources_uploaded_by ON resources (uploaded_by);
CREATE INDEX idx_resources_created_at  ON resources (created_at DESC);

-- =====================================================
-- TABLE: transactions
-- =====================================================
CREATE TABLE IF NOT EXISTS transactions (
    id                  CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
    user_id             CHAR(36)        NOT NULL,
    type                ENUM('add','purchase','upload_reward','withdrawal','topup_points','membership') NOT NULL,
    amount              DECIMAL(12, 2)  NOT NULL,
    currency            ENUM('BDT','Points') NOT NULL DEFAULT 'BDT',
    description         TEXT,
    payment_method      ENUM('Bkash','Nagad','Card','Bank_Transfer','Wallet'),
    status              ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    points_topup_rate   DECIMAL(10, 2),
    payment_phone       VARCHAR(50),
    transaction_number  VARCHAR(100),
    membership_plan     ENUM('free','premium_monthly','premium_yearly'),
    related_id          CHAR(36),
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_transactions_user_id    ON transactions (user_id);
CREATE INDEX idx_transactions_type       ON transactions (type);
CREATE INDEX idx_transactions_status     ON transactions (status);
CREATE INDEX idx_transactions_created_at ON transactions (created_at DESC);

-- =====================================================
-- TABLE: cart_items
-- =====================================================
CREATE TABLE IF NOT EXISTS cart_items (
    id          CHAR(36)    PRIMARY KEY DEFAULT (UUID()),
    user_id     CHAR(36)    NOT NULL,
    resource_id CHAR(36)    NOT NULL,
    added_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_cart_user_resource (user_id, resource_id),
    CONSTRAINT fk_cart_user     FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE,
    CONSTRAINT fk_cart_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
);

CREATE INDEX idx_cart_items_user_id ON cart_items (user_id);

-- =====================================================
-- TABLE: purchases
-- =====================================================
CREATE TABLE IF NOT EXISTS purchases (
    id              CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
    user_id         CHAR(36)        NOT NULL,
    resource_id     CHAR(36)        NOT NULL,
    price           DECIMAL(10, 2)  NOT NULL,
    price_type      ENUM('money','points') NOT NULL,
    payment_method  ENUM('Bkash','Nagad','Card','Bank_Transfer','Wallet'),
    feedback        TEXT,
    rating          SMALLINT        CHECK (rating BETWEEN 1 AND 5),
    purchased_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchases_user     FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE,
    CONSTRAINT fk_purchases_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE
);

CREATE INDEX idx_purchases_user_id      ON purchases (user_id);
CREATE INDEX idx_purchases_resource_id  ON purchases (resource_id);
CREATE INDEX idx_purchases_purchased_at ON purchases (purchased_at DESC);

-- =====================================================
-- TABLE: notifications
-- =====================================================
CREATE TABLE IF NOT EXISTS notifications (
    id          CHAR(36)        NOT NULL PRIMARY KEY DEFAULT (UUID()),
    user_id     CHAR(36)        NOT NULL,
    type        ENUM('purchase','sale','system','feedback') NOT NULL DEFAULT 'system',
    title       VARCHAR(255)    NOT NULL,
    message     TEXT            NOT NULL,
    is_read     BOOLEAN         NOT NULL DEFAULT FALSE,
    related_id  CHAR(36),
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_id   ON notifications (user_id);
CREATE INDEX idx_notifications_is_read   ON notifications (is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);

-- =====================================================
-- TABLE: feedbacks
-- =====================================================
CREATE TABLE IF NOT EXISTS feedbacks (
    id          CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
    user_id     CHAR(36)        NOT NULL,
    type        ENUM('system','item') NOT NULL DEFAULT 'system',
    rating      SMALLINT        NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT            NOT NULL,
    item_id     CHAR(36),
    item_title  VARCHAR(255),
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedbacks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedbacks_item FOREIGN KEY (item_id) REFERENCES resources(id) ON DELETE SET NULL
);

CREATE INDEX idx_feedbacks_user_id ON feedbacks (user_id);
CREATE INDEX idx_feedbacks_item_id ON feedbacks (item_id);

-- =====================================================
-- TABLE: withdrawals
-- =====================================================
CREATE TABLE IF NOT EXISTS withdrawals (
    id              CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
    user_id         CHAR(36)        NOT NULL,
    amount          DECIMAL(12, 2)  NOT NULL,
    method          ENUM('Bkash','Nagad','Card','Bank_Transfer','Wallet') NOT NULL,
    account_number  VARCHAR(100)    NOT NULL,
    status          ENUM('pending','completed','rejected') NOT NULL DEFAULT 'pending',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME,
    CONSTRAINT fk_withdrawals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_withdrawals_user_id ON withdrawals (user_id);
CREATE INDEX idx_withdrawals_status  ON withdrawals (status);

-- =====================================================
-- TABLE: appeals
-- =====================================================
CREATE TABLE IF NOT EXISTS appeals (
    id              CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
    user_id         CHAR(36)        NOT NULL,
    user_name       VARCHAR(150)    NOT NULL,
    user_email      VARCHAR(255)    NOT NULL,
    reason          TEXT            NOT NULL,
    status          ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
    admin_response  TEXT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at     DATETIME,
    CONSTRAINT fk_appeals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_appeals_user_id ON appeals (user_id);
CREATE INDEX idx_appeals_status  ON appeals (status);

-- =====================================================
-- TABLE: logs
-- =====================================================
CREATE TABLE IF NOT EXISTS logs (
    id                  CHAR(36)        PRIMARY KEY DEFAULT (UUID()),
    type                ENUM('user_action','admin_action','transaction','verification','system') NOT NULL,
    action              VARCHAR(100)    NOT NULL,
    description         TEXT            NOT NULL,
    user_id             CHAR(36),
    user_name           VARCHAR(150),
    target_user_id      CHAR(36),
    target_user_name    VARCHAR(150),
    metadata            JSON,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_user        FOREIGN KEY (user_id)        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_logs_target_user FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_logs_type       ON logs (type);
CREATE INDEX idx_logs_user_id    ON logs (user_id);
CREATE INDEX idx_logs_created_at ON logs (created_at DESC);

-- =====================================================
-- SEED: Default Admin User
-- Password: admin123 (bcrypt hash)
-- =====================================================
INSERT IGNORE INTO users (id, email, password_hash, name, university, is_verified, is_admin, wallet_balance, reward_points, membership_type)
VALUES (
    UUID(),
    'admin@passthepaper.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Zmod',
    'Admin User',
    'Pass The Paper',
    TRUE,
    TRUE,
    0.00,
    0,
    'free'
);
