
create table build(
            id serial,
            customer_id varchar,
            installation_id bigint,
            clone_url varchar,
            ref varchar,
            commit varchar,
            phase varchar,
            logs text,
            start_time timestamp)
