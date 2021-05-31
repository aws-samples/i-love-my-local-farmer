CREATE DATABASE  IF NOT EXISTS deliverydb;
USE deliverydb;

CREATE TABLE delivery_slot(
  slot_id integer primary key not null,
  delivery_date datetime not null,
  slot_from varchar(5) not null,
  slot_to varchar(5) not null,
  avail_deliveries int not null,
  booked_deliveries int,
  farm_id integer not null);

CREATE TABLE delivery(
  delivery_id integer primary key not null,
  slot_id integer not null,
  user_id integer not null,
  farm_id integer not null);