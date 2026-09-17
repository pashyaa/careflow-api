CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_roles (
                            user_id UUID NOT NULL,
                            role_id BIGINT NOT NULL,

                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles(id)
                                    ON DELETE CASCADE
);

INSERT INTO roles(name) VALUES
                            ('ROLE_ADMIN'),
                            ('ROLE_PLANNER'),
                            ('ROLE_TECHNICIAN'),
                            ('ROLE_CUSTOMER_VIEWER');