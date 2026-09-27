# NovaShop

## Docker Compose

Create a local `.env` file from `.env.example` and set at least:

```env
MYSQL_ROOT_PASSWORD=replace-with-a-local-mysql-root-password
JWT_SECRET=replace-with-a-long-local-secret
JWT_EXPIRATION=3600000
```

Start the full stack:

```bash
docker compose up --build
```

The gateway is exposed at:

```text
http://localhost:9090
```

Useful checks:

```bash
docker compose ps
docker compose logs -f
curl http://localhost:9090/api/products
```

Stop the stack:

```bash
docker compose down
```

Remove containers and the local MySQL volume:

```bash
docker compose down -v
```
