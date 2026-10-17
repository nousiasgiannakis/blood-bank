--liquibase formatted sql

--changeset ite4120:animals-01-species
--comment Species reference table
/* The lookup list the animal table points at. Natural-key reference data:
   no core.seq_id, no sys columns. Compare with 02-animal.sql, which is a
   full business table. */
create table animals.species (
    code                text not null,
    name                text not null,
    constraint species_pk primary key (code)
);
--

--changeset ite4120:animals-01-species-seed
--comment Seed the species reference list
/* Reference data is its own changeset, separate from the DDL, and convergent:
   ON CONFLICT DO NOTHING lets it land safely even on a database that already
   holds some of the rows. */
insert into animals.species (code, name) values
    ('DOG',    'Dog'),
    ('CAT',    'Cat'),
    ('HORSE',  'Horse'),
    ('PARROT', 'Parrot')
on conflict do nothing;
--
