CREATE TABLE platform_users (
                                id BIGSERIAL PRIMARY KEY,
                                phone VARCHAR(255) NOT NULL UNIQUE,
                                username VARCHAR(255) NOT NULL UNIQUE,
                                name VARCHAR(255) NOT NULL,
                                birthday DATE,
                                city VARCHAR(255),
                                vk VARCHAR(255),
                                instagram VARCHAR(255),
                                status VARCHAR(255),
                                last_login TIMESTAMP,
                                online BOOLEAN NOT NULL DEFAULT FALSE,
                                created TIMESTAMP DEFAULT NOW(),
                                completed_task INTEGER NOT NULL DEFAULT 0,
                                role VARCHAR(255) DEFAULT 'user',
                                active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO platform_users (phone, username, name, birthday, city, vk, instagram, status, last_login, online, completed_task, role, active)
VALUES
('+1234567890', 'john_doe', 'John Doe', '1990-01-01', 'New York', 'vk.com/johndoe', 'instagram.com/johndoe', 'Active', '2024-12-29 12:00:00', TRUE, 5, 'user', TRUE),
('+0987654321', 'jane_smith', 'Jane Smith', '1985-05-15', 'Los Angeles', 'vk.com/janesmith', 'instagram.com/janesmith', 'Inactive', NULL, FALSE, 2, 'user', TRUE),
('+1112223333', 'bob_brown', 'Bob Brown', '1995-07-20', 'Chicago', NULL, NULL, NULL, NULL, FALSE, 10, 'user', TRUE);
