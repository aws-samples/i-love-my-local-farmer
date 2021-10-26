create database provman;

create table product (
                          id integer primary key auto_increment not null,
                          name varchar(30) not null,
                          farm_id integer not null,
                          description varchar(50)
);

create table pricing (
                          id integer primary key auto_increment not null,
                          weight float not null,
                          price float not null,
                          product_id integer not null
);

create table provider (
                          id integer primary key auto_increment not null,
                          name varchar(50) not null,
                          entering_date date not null,
                          nationality varchar(5) not null,
                          code varchar(20) not null
);