# Product Service

Microservicio encargado de administrar el catalogo de productos y categorias de NovaShop.

Expone operaciones CRUD para categorias, productos y actualizacion de stock. En ejecucion local escucha en el puerto `8080`.

## Tecnologias

- Java 25
- Spring Boot 4.0.8
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- Spring Boot Actuator
- MySQL
- Lombok
- Maven Wrapper

## Configuracion

El servicio usa las siguientes variables de entorno:

| Variable | Valor por defecto | Descripcion |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/db_products` | URL de conexion a MySQL |
| `DB_USERNAME` | `root` | Usuario de base de datos |
| `DB_PASSWORD` | vacio | Password de base de datos |

Configuracion principal:

```properties
spring.application.name=product-service
server.port=8080
spring.jpa.hibernate.ddl-auto=update
```

## Base De Datos

Crear la base antes de iniciar el servicio:

```sql
CREATE DATABASE db_products;
```

En desarrollo, Hibernate crea y actualiza las tablas automaticamente usando:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Para produccion conviene reemplazar esto por migraciones versionadas con Flyway o Liquibase.

## Ejecucion Local

Desde la carpeta `product-service`:

```bash
./mvnw spring-boot:run
```

O desde VS Code usando la configuracion:

```text
ProductServiceApplication
```

## Compilacion Y Tests

Compilar sin ejecutar tests:

```bash
./mvnw -DskipTests compile
```

Ejecutar tests:

```bash
./mvnw test
```

## Endpoints De Categorias

Base path:

```text
/api/categories
```

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| `POST` | `/api/categories` | Crea una categoria |
| `GET` | `/api/categories` | Lista categorias paginadas |
| `GET` | `/api/categories/{id}` | Obtiene una categoria por id |
| `GET` | `/api/categories/search?name={name}` | Busca categorias por nombre |
| `PUT` | `/api/categories/{id}` | Actualiza una categoria |
| `DELETE` | `/api/categories/{id}` | Elimina una categoria |

### Crear Categoria

```http
POST /api/categories
Content-Type: application/json
```

```json
{
  "name": "Electronics",
  "description": "Electronic products and devices"
}
```

## Endpoints De Productos

Base path:

```text
/api/products
```

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| `POST` | `/api/products` | Crea un producto |
| `GET` | `/api/products` | Lista productos paginados |
| `GET` | `/api/products/{id}` | Obtiene un producto por id |
| `GET` | `/api/products/sku/{sku}` | Obtiene un producto por SKU |
| `GET` | `/api/products/search?name={name}` | Busca productos por nombre |
| `PUT` | `/api/products/{id}` | Actualiza un producto |
| `PATCH` | `/api/products/{id}/stock` | Actualiza el stock de un producto |
| `DELETE` | `/api/products/{id}` | Elimina un producto |

### Crear Producto

```http
POST /api/products
Content-Type: application/json
```

```json
{
  "name": "Notebook Lenovo",
  "sku": "SKU-001",
  "description": "Notebook Lenovo 16GB RAM 512GB SSD",
  "price": 1200.00,
  "stock": 10,
  "active": true,
  "categoryId": 1
}
```

### Actualizar Producto

```http
PUT /api/products/1
Content-Type: application/json
```

```json
{
  "name": "Notebook Lenovo Pro",
  "sku": "SKU-001",
  "description": "Notebook Lenovo Pro 16GB RAM 1TB SSD",
  "price": 1450.00,
  "stock": 8,
  "active": true,
  "categoryId": 1
}
```

### Actualizar Stock

```http
PATCH /api/products/1/stock
Content-Type: application/json
```

```json
{
  "stock": 25
}
```

## Paginacion

Los endpoints de listado usan `Pageable` de Spring Data.

Ejemplo:

```http
GET /api/products?page=0&size=10
```

Tambien se puede ordenar:

```http
GET /api/products?page=0&size=10&sort=name,asc
```

## Seguridad

`product-service` no valida JWT directamente. La seguridad se aplica desde `api-gateway`.

Reglas actuales en el gateway:

| Endpoint | Acceso |
| --- | --- |
| `GET /api/products` | Publico |
| `GET /api/products/**` | Publico |
| `GET /api/categories` | Publico |
| `GET /api/categories/**` | Publico |
| `POST /api/products` | Requiere rol `ADMIN` |
| `PUT /api/products/**` | Requiere rol `ADMIN` |
| `PATCH /api/products/**` | Requiere rol `ADMIN` |
| `DELETE /api/products/**` | Requiere rol `ADMIN` |
| `POST /api/categories` | Requiere rol `ADMIN` |
| `PUT /api/categories/**` | Requiere rol `ADMIN` |
| `DELETE /api/categories/**` | Requiere rol `ADMIN` |

Para consumir endpoints protegidos a traves del gateway:

```http
Authorization: Bearer <token>
```

## Postman

Existe una coleccion para probar el servicio:

```text
postman/product-service/product-service.postman_collection.json
```

Para probar pasando por el gateway, usar como base URL:

```text
http://localhost:9090
```

Para probar el servicio directamente:

```text
http://localhost:8080
```

## Health Check

```http
GET /actuator/health
```

Respuesta esperada:

```json
{
  "status": "UP"
}
```

## Errores Frecuentes

### No conecta a MySQL

Verificar que MySQL este corriendo y que exista la base:

```sql
CREATE DATABASE db_products;
```

Tambien revisar las variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

### Categoria No Encontrada Al Crear Producto

Antes de crear un producto, debe existir la categoria indicada en `categoryId`.

### SKU Duplicado

El campo `sku` es unico. Si se intenta crear otro producto con el mismo SKU, el servicio responde con error.
