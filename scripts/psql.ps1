# Open a psql shell inside the database container.
docker exec -it ite4120-postgres-1 psql -U animals -d animals
