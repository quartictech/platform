
create table build(
            id serial,
            customer_id varchar,
            revision varchar,
            phase varchar,
            logs text,
            build_date timestamp)
