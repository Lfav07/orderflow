# Run Orderflow with Docker Compose

From the repository root, start Docker Desktop (Linux containers), then run:

```sh
docker compose up -d --build
```

Compose builds both Java 25 services and starts separate PostgreSQL databases. It waits for each database to accept connections before starting its application. Flyway initializes each schema. Order connects to Inventory through `inventory:9090` on the Compose network; its startup does not wait for Inventory to become ready, so allow both services time to start before making requests.

- Inventory GraphQL: `http://localhost:8080/graphql`
- Order GraphQL: `http://localhost:8081/graphql`

Set `INVENTORY_HTTP_PORT` or `ORDER_HTTP_PORT` before starting Compose to change host ports if they are in use. The default database credentials are `orderflow` / `orderflow-local` for local development. Override both `DB_USERNAME` and `DB_PASSWORD` in your shell or an untracked `.env` file for your environment. PostgreSQL credentials are initialized only when a volume is first created; changing the variables later does not update existing database users or passwords.

Check startup with `docker compose ps` and `docker compose logs -f inventory order`. Stop containers with `docker compose down`; database volumes persist. Removing the named volumes also removes the stored data.

The Jenkins configuration is commented out in Compose. The default local Spring configuration remains available when running applications outside Compose.
