CREATE DATABASE IF NOT EXISTS deliverydb;
USE deliverydb;

CREATE TABLE IF NOT EXISTS delivery_slot
(
    slot_id           integer primary key auto_increment not null,
    delivery_date     date                               not null,
    slot_from         datetime                           not null,
    slot_to           datetime                           not null,
    avail_deliveries  int                                not null,
    booked_deliveries int,
    farm_id           integer                            not null
);

CREATE TABLE IF NOT EXISTS delivery
(
    delivery_id integer primary key auto_increment not null,
    slot_id     integer                            not null,
    user_id     integer                            not null,
    farm_id     integer                            not null
);

DROP USER IF EXISTS '{{username}}';

CREATE USER IF NOT EXISTS '{{username}}' identified by '{{password}}';
GRANT DELETE, UPDATE, INSERT, SELECT ON deliverydb.* TO '{{username}}';
FLUSH PRIVILEGES;
