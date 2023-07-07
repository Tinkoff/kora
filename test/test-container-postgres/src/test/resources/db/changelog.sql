--liquibase formatted sql

--changeset example:1
CREATE TABLE users
(
    id INT NOT NULL PRIMARY KEY
);
