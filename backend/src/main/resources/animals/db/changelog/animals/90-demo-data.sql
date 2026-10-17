--liquibase formatted sql

--changeset ite4120:animals-90-demo-data context:demo
--comment Demo rows for local development
/* Applied only when the Liquibase context includes `demo`
   (spring.liquibase.contexts in application.yml). Owner codes match the
   imitated population registry in MockOwnerRegistryController — so the
   owner lookup works out of the box. */
insert into animals.animal (registry_code, name, species_code, birth_date, owner_isikukood, chip_number) values
    ('EE-2026-0001', 'Muri',  'DOG',    '2021-04-12', '38102130265', '985141001234567'),
    ('EE-2026-0002', 'Miisu', 'CAT',    '2023-01-30', '47503121234', null),
    ('EE-2026-0003', 'Rufus', 'HORSE',  '2018-06-01', '50011020017', '985141007654321');

insert into animals.vaccination (animal_id, vaccine, vaccinated_on, valid_until)
select a.id, v.vaccine, v.vaccinated_on::date, v.valid_until::date
from animals.animal a
join (values
    ('EE-2026-0001', 'Rabies',    '2025-05-10', '2026-05-10'),
    ('EE-2026-0001', 'Distemper', '2024-11-02', '2026-11-02'),
    ('EE-2026-0003', 'Tetanus',   '2025-08-15', '2027-08-15')
) as v(registry_code, vaccine, vaccinated_on, valid_until)
  on v.registry_code = a.registry_code;
--
