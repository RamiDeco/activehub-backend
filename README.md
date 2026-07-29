# ActiveHub Backend

Backend de la aplicación ActiveHub desarrollado con Java 17, Spring Boot 3 y MySQL sobre contenedores con Docker.

---

## Requisitos Previos

Asegúrate de tener instalado lo siguiente en tu máquina:

* Java JDK 17 o superior
* Maven (opcional, se puede usar el wrapper ./mvnw)
* Docker y Docker Engine
* Git

---

## Pasos para Clonar y Ejecutar

### 1. Clonar el repositorio

```bash
git clone git@github.com:RamiDeco/activehub-backend.git
cd activehub-backend
```

### 2. Levantar base de datos con docker

```bash
docker run -d \
  --name mysql-container \
  -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=activehub \
  mysql:latest
```
Verificar estado: Corre docker ps para confirmar que el contenedor mysql-container esté activo.

### 3. Configurar las propiedades de entorno
Asegúrate de que tu archivo src/main/resources/application.properties apunte a la base de datos local:

```bash
spring.application.name=activehub

# Conexión a MySQL
spring.datasource.url=jdbc:mysql://localhost:3307/activehub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=rootpassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuración de JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 4. Compilar y Ejecutar la aplicación
Puedes ejecutar la aplicación directamente usando el Maven Wrapper incluido:

```bash
http://localhost:8080/
```

