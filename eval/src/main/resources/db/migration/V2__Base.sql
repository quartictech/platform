
alter table build add column branch varchar;

update build set branch = regexp_replace(trigger_details->>'ref', '^refs\/heads\/', '');
