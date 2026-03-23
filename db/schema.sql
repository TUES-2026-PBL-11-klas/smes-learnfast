CREATE DATABASE IF NOT EXISTS learnfast;
USE learnfast;

CREATE TABLE roles(
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);
INSERT INTO roles (name) VALUES ('student'), ('mentor');

CREATE TABLE users(
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    
    FOREIGN KEY (role_id) REFERENCES roles(id)
);