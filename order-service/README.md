# Order Service

Microservicio encargado de administrar pedidos de NovaShop.

Integra `customer-service` y `product-service` mediante OpenFeign para validar clientes, validar productos, calcular totales y actualizar stock. En ejecucion local escucha en el puerto `8082`.

## Tecnologias

- Java 25
- Spring Boot 4.0.8
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- Spring Boot Actuator
- Spring Cloud OpenFeign
- MySQL
- Lombok
- Maven Wrapper

## Configuracion

El servicio usa las siguientes variables de entorno:

| Variable | Valor por defecto | Descripcion |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/db_orders` | URL de conexion a MySQL |
| `DB_USERNAME` | `root` | Usuario de base de datos |
| `DB_PASSWORD` | vacio | Password de base de datos |
| `CUSTOMER_SERVICE_URL` | `http://localhost:8081` | URL de `customer-service` |
| `PRODUCT_SERVICE_URL` | `http://localhost:8080` | URL de `product-service` |

Configuracion principal:

```properties
spring.application.name=order-service
server.port=8082
spring.jpa.hibernate.ddl-auto=update
customer-service.url=${CUSTOMER_SERVICE_URL:http://localhost:8081}
product-service.url=${PRODUCT_SERVICE_URL:http://localhost:8080}
```

## Base De Datos

Crear la base antes de iniciar el servicio:

```sql
CREATE DATABASE db_orders;
```

En desarrollo, Hibernate crea y actualiza las tablas automaticamente usando:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Para produccion conviene reemplazar esto por migraciones versionadas con Flyway o Liquibase.

## Servicios Requeridos

Para crear pedidos correctamente deben estar levantados:

| Servicio | Puerto | Uso |
| --- | --- | --- |
| `customer-service` | `8081` | Validar que el cliente exista y este activo |
| `product-service` | `8080` | Validar productos, precios, stock y actualizar stock |

## Ejecucion Local

Desde la carpeta `order-service`:

```bash
./mvnw spring-boot:run
```

O desde VS Code usando la configuracion:

```text
OrderServiceApplication
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

Los tests de contexto pueden requerir que MySQL este disponible, porque el servicio configura un datasource MySQL.

## Endpoints

Base path:

```text
/api/orders
```

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| `POST` | `/api/orders` | Crea un pedido |
| `GET` | `/api/orders` | Lista pedidos paginados |
| `GET` | `/api/orders/{id}` | Obtiene un pedido por id |
| `PATCH` | `/api/orders/{id}/cancel` | Cancela un pedido |

## Flujo De Creacion De Pedido

El flujo actual es:

```text
1. Valida que el cliente exista y este activo.
2. Valida que cada producto exista, este activo y tenga stock suficiente.
3. Calcula subtotal por item y total del pedido.
4. Crea la orden en estado PENDING.
5. Actualiza el stock de productos.
6. Confirma la orden cambiando su estado a CONFIRMED.
```

Estados posibles:

```text
PENDING
CONFIRMED
CANCELLED
```

Al cancelar una orden `CONFIRMED`, el servicio restaura el stock de los productos.

## Ejemplos

### Crear Pedido

```http
POST /api/orders
Content-Type: application/json
```

```json
{
  "customerId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

### Listar Pedidos

```http
GET /api/orders?page=0&size=10
```

Tambien se puede ordenar:

```http
GET /api/orders?page=0&size=10&sort=createdAt,desc
```

### Obtener Pedido Por Id

```http
GET /api/orders/1
```

### Cancelar Pedido

```http
PATCH /api/orders/1/cancel
```

## Validaciones

El request de creacion requiere:

| Campo | Validacion |
| --- | --- |
| `customerId` | Obligatorio |
| `items` | Obligatorio y no vacio |
| `items[].productId` | Obligatorio |
| `items[].quantity` | Obligatorio y mayor a cero |

Reglas de negocio:

- El cliente debe estar activo.
- Cada producto debe estar activo.
- Cada producto debe tener stock suficiente.
- No se puede cancelar una orden ya cancelada.

## Seguridad

`order-service` no valida JWT directamente. La seguridad se aplica desde `api-gateway`.

Reglas actuales en el gateway:

| Endpoint | Acceso |
| --- | --- |
| `GET /api/orders` | Requiere rol `ADMIN` |
| `/api/orders/**` | Requiere usuario autenticado |

Para consumir endpoints protegidos a traves del gateway:

```http
Authorization: Bearer <token>
```

## Postman

Existe una coleccion para probar el servicio:

```text
postman/order-service/order-service.postman_collection.json
```

Para probar pasando por el gateway, usar como base URL:

```text
http://localhost:9090
```

Para probar el servicio directamente:

```text
http://localhost:8082
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
CREATE DATABASE db_orders;
```

Tambien revisar las variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

### Falla Al Crear Un Pedido

Verificar que esten levantados `customer-service` y `product-service`.

Tambien revisar que:

- El cliente exista y este activo.
- Los productos existan y esten activos.
- Los productos tengan stock suficiente.

### Orden Ya Cancelada

Si se intenta cancelar una orden con estado `CANCELLED`, el servicio responde con error.

### Consistencia De Stock

El servicio crea la orden como `PENDING` y solo la marca como `CONFIRMED` despues de actualizar el stock. Para escenarios de alta concurrencia, la reserva de stock deberia reforzarse con una operacion atomica en `product-service`.
