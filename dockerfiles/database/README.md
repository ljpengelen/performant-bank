# PostgreSQL in Docker Container

To run PostgreSQL in a Docker container, first build an image by executing the following command from the root of this repository.

```
docker build -t bank-postgres -f dockerfiles/database/Dockerfile .
```

When you start the container for the first time, you need to specify a password by setting the environment variable POSTGRES_PASSWORD.

```
docker run --rm -p 5432:5432 -e POSTGRES_PASSWORD=bank -v bank-postgres:/var/lib/postgresql/data bank-postgres
```

After this initial run, you can start a container by executing the following command.

```
docker run --rm -p 5432:5432 -v bank-postgres:/var/lib/postgresql/data bank-postgres
```

The script `init-db.sh` will create two databases (`bank-dev` and `bank-test`) when you start the container for the first time.
