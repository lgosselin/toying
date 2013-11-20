-- This script is not run automatically. Just a quick reminder on how to proceed
create database unit_test character set utf8 collate utf8_general_ci;
create user unit_test identified by 'unit_test_pass';
grant all privileges on unit_test.* to unit_test@'%';
flush privileges;
