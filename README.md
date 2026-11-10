
# Shop API — Spring Boot REST API

A backend REST API for managing an online shop's product catalog, developed using Java, Spring Boot, Spring Data JPA, and MySQL.

## Features

- Create, retrieve, update, and delete products
- Search products by name (case-insensitive)
- Pagination and sorting
- Product input validation
- HTTP status codes for successful and failed requests
- MySQL database persistence
- Layered architecture: Controller, Service, Repository
- Unit testing with JUnit 5 and Mockito

## Tech Stack

- Java
- Spring Boot
- Spring Data JPA / Hibernate
- MySQL 8
- Maven
- JUnit 5
- Mockito
- Git and GitHub

## Project Structure

src/
  main/
    java/com/shivani/shop_api/
      controller/ProductController.java
      service/ProductService.java
      repository/ProductRepository.java
      model/Product.java
    resources/application.properties
  test/
    java/com/shivani/shop_api/
      ShopApiApplicationTests.java

## REST API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /products | Get all products |
| GET | /products/{id} | Get product by ID |
| POST | /products | Create a product |
| PUT | /products/{id} | Update a product |
| DELETE | /products/{id} | Delete a product |
| GET | /products/search?name=Dell | Search by name |
| GET | /products/paginated?page=0&size=5&sortBy=price | Pagination and sorting |

## Example Request

POST /products

Content-Type: application/json

{
  "name": "Dell Laptop",
  "price": 55000,
  "description": "Dell Inspiron laptop with 16GB RAM",
  "stock": 10
}

## Run Locally

1. Install Java, Maven, and MySQL.
2. Create a MySQL database named `shop_db`.
3. Create a MySQL user named `shop_user` with access to the database.
4. Set the `DB_PASSWORD` environment variable to the database user's password.
5. Run:

   mvn spring-boot:run

6. Access the API at:

   http://localhost:8080/products

## Run Tests

mvn test

## Future Improvements

- User authentication and authorization
- Shopping cart and order management
- API documentation with Swagger/OpenAPI
- Docker deployment

## Author

Shivani Patil

GitHub: https://github.com/shivanivp18

