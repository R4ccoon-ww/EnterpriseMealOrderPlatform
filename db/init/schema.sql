-- WeBox 企业员工餐食订购平台 - 建表脚本
-- 由 docker-entrypoint-initdb.d 在 MySQL 容器首次启动时自动执行

USE webox;

CREATE TABLE `user` (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(128) NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密',
    name        VARCHAR(64)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE menu_item (
    id          VARCHAR(32)  PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    price       DECIMAL(10, 2) NOT NULL,
    image       VARCHAR(256),
    category    VARCHAR(32) NOT NULL,
    allergens   VARCHAR(256) NOT NULL DEFAULT '[]' COMMENT 'JSON 数组，如 ["peanut","dairy"]',
    available   TINYINT NOT NULL DEFAULT 1 COMMENT '1=当日可订',
    INDEX idx_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cart_item (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    menu_item_id  VARCHAR(32) NOT NULL,
    quantity      INT         NOT NULL DEFAULT 1,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_item (user_id, menu_item_id),
    INDEX idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    total_amount     DECIMAL(10, 2) NOT NULL,
    delivery_date    VARCHAR(10)    NOT NULL COMMENT 'YYYY-MM-DD',
    meal_period      VARCHAR(16)    NOT NULL COMMENT 'lunch | dinner',
    delivery_address VARCHAR(256)   NOT NULL,
    status           VARCHAR(16)    NOT NULL DEFAULT 'pending' COMMENT 'pending|confirmed|completed|cancelled',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE order_item (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT         NOT NULL,
    menu_item_id  VARCHAR(32)    NOT NULL,
    name          VARCHAR(128)   NOT NULL COMMENT '菜品名称快照',
    price         DECIMAL(10, 2) NOT NULL COMMENT '下单时单价快照',
    quantity      INT            NOT NULL,
    INDEX idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_preference (
    user_id              BIGINT PRIMARY KEY,
    allergens            VARCHAR(256) NOT NULL DEFAULT '[]' COMMENT 'JSON 数组，需排除的过敏原',
    preferred_categories VARCHAR(256) NOT NULL DEFAULT '[]' COMMENT 'JSON 数组，偏好菜系',
    spicy_level          VARCHAR(16) COMMENT 'none | mild | hot',
    taste                VARCHAR(16) COMMENT 'light | medium | heavy',
    budget_min           DECIMAL(10, 2),
    budget_max           DECIMAL(10, 2),
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- PRD 第 7 节菜品示例数据
INSERT INTO menu_item (id, name, description, price, image, category, allergens) VALUES
('item_001', '宫保鸡丁', '经典川菜，鸡肉搭配花生、干辣椒爆炒', 22.00, 'https://example.com/kungpao.jpg', 'chinese', '["peanut"]'),
('item_002', '凯撒沙拉', '新鲜罗马生菜配帕玛森芝士与凯撒酱', 28.00, 'https://example.com/caesar.jpg', 'salad', '["dairy","egg"]'),
('item_003', '三文鱼刺身定食', '新鲜三文鱼刺身搭配米饭、味噌汤', 45.00, 'https://example.com/sashimi.jpg', 'japanese', '["fish"]'),
('item_004', '番茄意面', '经典意式番茄酱意大利面配新鲜罗勒', 26.00, 'https://example.com/pasta.jpg', 'western', '["gluten"]'),
('item_005', '冬阴功汤', '泰式酸辣虾汤配香茅、南姜、柠檬叶', 32.00, 'https://example.com/tomyum.jpg', 'southeast_asian', '["shellfish"]'),
('item_006', '鸡胸肉藜麦碗', '低脂高蛋白，烤鸡胸配藜麦、牛油果、时蔬', 35.00, 'https://example.com/quinoa.jpg', 'salad', '[]'),
('item_007', '麻婆豆腐', '四川经典，嫩豆腐配麻辣肉末', 18.00, 'https://example.com/mapo.jpg', 'chinese', '["soy"]'),
('item_008', '韩式拌饭', '石锅拌饭配各式时蔬、煎蛋与辣酱', 30.00, 'https://example.com/bibimbap.jpg', 'korean', '["egg","soy"]');
