
--drop table phase;

alter table build
  drop column time;

--alter table event
--  drop column phase_id,
--  add column build_id uuid not null;


