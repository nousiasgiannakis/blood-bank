--liquibase formatted sql

--changeset ite4120:animals-03-vaccination
--comment Vaccination child table
/* One row per vaccination event, FK to the animal. With species and animal
   this makes three tables and two foreign keys: the assessment's gate-A
   data-layer minimum, demonstrated exactly. */
create table animals.vaccination (
    id                  bigint default nextval('core.seq_id') not null,
    animal_id           bigint not null,
    vaccine             text not null,
    vaccinated_on       date not null,
    valid_until         date,
    sys_status          char(1),
    sys_version         int,
    sys_created_at      timestamptz,
    sys_created_by      text,
    sys_modified_at     timestamptz,
    sys_modified_by     text,
    constraint vaccination_pk primary key (id),
    constraint vaccination_animal_fk foreign key (animal_id) references animals.animal (id)
);

select core.create_table_metadata('animals.vaccination');

create index vaccination_animal_idx on animals.vaccination (animal_id) where (sys_status = 'A');
--
