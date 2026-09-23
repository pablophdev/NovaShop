# Customer Service

Microservicio encargado de administrar clientes de NovaShop.

Expone operaciones CRUD para crear, consultar, actualizar y eliminar clientes. En ejecucion local escucha en el puerto `8081`.

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
| `DB_URL` | `jdbc:mysql://localhost:3306/db_customers` | URL de conexion a MySQL |
| `DB_USERNAME` | `root` | Usuario de base de datos |
| `DB_PASSWORD` | vacio | Password de base de datos |

Configuracion principal:

```properties
spring.application.name=customer-service
server.port=8081
spring.jpa.hibernate.ddl-auto=update
```

## Base De Datos

Crear la base antes de iniciar el servicio:

```sql
CREATE DATABASE db_customers;
```

En desarrollo, Hibernate crea y actualiza las tablas automaticamente usando:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Para produccion conviene reemplazar esto por migraciones versionadas con Flyway o Liquibase.

## Ejecucion Local

Desde la carpeta `customer-service`:

```bash
./mvnw spring-boot:run
```

O desde VS Code usando la configuracion:

```text
CustomerServiceApplication
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

## Endpoints

Base path:

```text
/api/customers
```

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| `POST` | `/api/customers` | Crea un cliente |
| `GET` | `/api/customers` | Lista clientes paginados |
| `GET` | `/api/customers/{id}` | Obtiene un cliente por id |
| `PUT` | `/api/customers/{id}` | Actualiza un cliente |
| `DELETE` | `/api/customers/{id}` | Elimina un cliente |

## Ejemplos

### Crear Cliente

```http
POST /api/customers
Content-Type: application/json
```

```json
{
  "firstName": "Pablo",
  "lastName": "Perez",
  "email": "pablo.perez@example.com",
  "phone": "+5491123456789",
  "address": "Av. Siempre Viva 123",
  "active": true
}
```

### Actualizar Cliente

```http
PUT /api/customers/1
Content-Type: application/json
```

```json
{
  "firstName": "Pablo",
  "lastName": "Perez",
  "email": "pablo.perez@example.com",
  "phone": "+5491123456789",
  "address": "Av. Siempre Viva 742",
  "active": true
}
```

### Listar Clientes

```http
GET /api/customers?page=0&size=10
```

Tambien se puede ordenar:

```http
GET /api/customers?page=0&size=10&sort=lastName,asc
```

## Validaciones

El request de cliente requiere:

| Campo | Validacion |
| --- | --- |
| `firstName` | Obligatorio |
| `lastName` | Obligatorio |
| `email` | Obligatorio y formato email valido |
| `phone` | Obligatorio |
| `active` | Obligatorio |
| `address` | Opcional |

El campo `email` debe ser unico.

## Seguridad

`customer-service` no valida JWT directamente. La seguridad se aplica desde `api-gateway`.

Reglas actuales en el gateway:

| Endpoint | Acceso |
| --- | --- |
| `GET /api/customers` | Requiere rol `ADMIN` |
| `GET /api/customers/**` | Requiere usuario autenticado |
| `PUT /api/customers/**` | Requiere usuario autenticado |
| `DELETE /api/customers/**` | Requiere rol `ADMIN` |

Para consumir endpoints protegidos a traves del gateway:

```http
Authorization: Bearer <token>
```

## Postman

Existe una coleccion para probar el servicio:

```text
postman/customer-service/customer-service.postman_collection.json
```

Para probar pasando por el gateway, usar como base URL:

```text
http://localhost:9090
```

Para probar el servicio directamente:

```text
http://localhost:8081
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
CREATE DATABASE db_customers;
```

Tambien revisar las variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

### Email Duplicado

El campo `email` es unico. Si se intenta crear o actualizar un cliente con un email ya existente, el servicio responde con error.

### Cliente No Encontrado

Los endpoints por id devuelven error si no existe un cliente con el id indicado.
