CREATE TABLE IF NOT EXISTS products (
    id TEXT PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    material TEXT NOT NULL,
    price REAL NOT NULL DEFAULT 0,
    price_unit TEXT NOT NULL DEFAULT 'piece',
    moq INTEGER NOT NULL DEFAULT 1,
    stock INTEGER NOT NULL DEFAULT 0,
    restock_days INTEGER,
    size TEXT NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    features_json TEXT NOT NULL DEFAULT '[]',
    published INTEGER NOT NULL DEFAULT 0 CHECK (published IN (0, 1)),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS product_images (
    image_id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('ORIGINAL', 'MARKETING')),
    path TEXT NOT NULL,
    public_url TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    approved INTEGER NOT NULL DEFAULT 0 CHECK (approved IN (0, 1)),
    created_at TEXT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_generations (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL,
    model TEXT NOT NULL,
    status TEXT NOT NULL,
    prompt TEXT NOT NULL,
    result_image_id TEXT,
    error_message TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (result_image_id) REFERENCES product_images(image_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_products_published ON products(published);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_product_images_product ON product_images(product_id, kind, sort_order);
CREATE INDEX IF NOT EXISTS idx_ai_generations_product ON ai_generations(product_id, created_at);
UPDATE ai_generations
SET status = 'FAILED',
    result_image_id = NULL,
    error_message = CASE
        WHEN error_message IS NULL OR error_message = ''
        THEN 'Generation was interrupted before completion.'
        ELSE error_message
    END
WHERE status = 'RUNNING';
CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_generations_one_running_per_product
    ON ai_generations(product_id) WHERE status = 'RUNNING';


CREATE TABLE IF NOT EXISTS site_settings (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS admin_credentials (
    username TEXT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
