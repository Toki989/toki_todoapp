-- MySQL 8.0 / one-time migration.
-- This script intentionally fails if either target table already exists so that
-- a potentially incompatible schema is never silently accepted.
CREATE TABLE todo_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_todo_templates_name_not_blank
        CHECK (CHAR_LENGTH(TRIM(REPLACE(name, '　', ' '))) > 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE todo_template_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    todo_template_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    detail VARCHAR(255) NULL,
    category VARCHAR(255) NOT NULL,
    priority INT NOT NULL DEFAULT 2,
    due_date DATE NULL,
    display_order INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_todo_template_items_template
        FOREIGN KEY (todo_template_id) REFERENCES todo_templates(id) ON DELETE CASCADE,
    CONSTRAINT uq_todo_template_items_order UNIQUE (todo_template_id, display_order),
    CONSTRAINT chk_todo_template_items_display_order CHECK (display_order >= 1),
    CONSTRAINT chk_todo_template_items_title_not_blank
        CHECK (CHAR_LENGTH(TRIM(REPLACE(title, '　', ' '))) > 0),
    CONSTRAINT chk_todo_template_items_category CHECK (category IN (
        'デザイン', 'マーケティング', 'プログラミング', '資格', '就職活動'
    )),
    CONSTRAINT chk_todo_template_items_priority CHECK (priority IN (1, 2, 3)),
    INDEX idx_todo_template_items_template_order (todo_template_id, display_order)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
