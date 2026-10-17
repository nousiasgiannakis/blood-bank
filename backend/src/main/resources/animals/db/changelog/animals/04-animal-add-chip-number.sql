--liquibase formatted sql

--changeset ite4120:animals-04-animal-add-chip-number
--comment Add the microchip number — schema evolution done right
/* The chip number arrived after 02 shipped. We do NOT edit 02-animal.sql —
   a released changeset is immutable (Liquibase stores its checksum and
   refuses to run an edited one). We append a new changeset with the ALTER.
   This file is the whole migration lesson: the schema's history stays
   honest, and every environment — your laptop, a teammate's, CI —
   converges by replaying the same steps. */
alter table animals.animal add column chip_number text;

comment on column animals.animal.chip_number is 'Microchip number; optional. Added in changeset 04.';
--
