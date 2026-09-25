# Spring Boot Assignment: Pet Management System

## Overview

This assignment is designed to evaluate your skills in building RESTful services using Spring Boot. You will be working
on a simple Pet Management System, which involves entities like `Owner` and `Pet`. Your task is to extend this system
with additional functionalities and improve its architecture.

## Existing Classes

- **Owner**: Represents the pet owner.
- **Pet**: Represents the pet.
- **PetController**: A REST controller for managing pets.

## Security Note

- Endpoints are secured with Spring Security. The default credentials are defined in the `application.properties` file.
  You will need to use these credentials when making requests through your preferred method (e.g., Postman).
- You can use `HelloController` to test whether you have the application running and set up properly. Sending a GET
  request to `localhost:8080` will return a "Hello, World!" message if it is running properly.

## Requirements

- Follow RESTful practices and conventions in implementing the API.
- Write clean, readable, and maintainable code.
- Include proper error handling and validation where necessary.
- Adhere to the principles of object-oriented design.
- Implement and utilize proper version control practices.

### Submission Guidelines

- Submit your code in a .zip file or a link to a GitHub repository.
- Ensure the README file includes instructions on how to run your application and tests.
- Document your code appropriately, including comments and Javadoc where necessary.
- Include a brief summary of your design and implementation choices in the README file.
- If there is any additional information you would like to share with us (such as special instructions on running the
  application), please include it in the README file.
- Please include any other collections that are relevant to your submission, like Postman collections

## How to Run

### Prerequisites

- JDK 17 or newer. Maven does not need to be installed: the project includes the Maven Wrapper (`./mvnw`, or
  `mvnw.cmd` on Windows).

### Start the Application

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080` with an in-memory H2 database, which is filled with sample owners and
pets on every start. Data is not kept between restarts.

### Credentials

Every request needs HTTP Basic authentication. Each user has one role (configured in `application.properties`):

| User     | Password          | Access                   |
|----------|-------------------|--------------------------|
| `reader` | `reader-password` | read only (`GET`)        |
| `writer` | `writer-password` | create, update, delete   |
| `admin`  | `admin-password`  | everything               |

### Try It Out

- **Swagger UI:** open `http://localhost:8080/swagger-ui/index.html` and log in as `admin` when the browser asks.
  It documents every endpoint and parameter and can send requests.
- **curl:**

```bash
# Health check
curl -u reader:reader-password http://localhost:8080

# Dogs and cats, oldest first, 10 per page
curl -u reader:reader-password "http://localhost:8080/pets?species=dog,cat&sort=age,desc&size=10"

# Pets without an owner
curl -u reader:reader-password "http://localhost:8080/pets?hasOwner=false"

# Create an owner
curl -u writer:writer-password -X POST http://localhost:8080/owners \
     -H "Content-Type: application/json" \
     -d '{"nameFirst": "Carol", "nameLast": "White"}'
```

### Run the Tests

```bash
./mvnw test
```

The tests are integration tests (Spring Boot + MockMvc + H2) and need no external services.

## Design and Implementation Choices

### Architecture

- **Layers: controller → service → repository.** Controllers handle HTTP only: mapping, status codes, `Location`
  headers, validation and paging parameters. Services (`PetService`, `OwnerService`) hold the business rules and
  transactions. `ControllerLayeringTests` fails if a controller uses a repository directly.
- **Request DTOs** (`PetRequest`, `PetPatchRequest`, `OwnerRequest`, `OwnerPatchRequest`) separate the API input from
  the entities. They have no `id` field, so a client cannot overwrite another record by sending one. Responses
  return the entities directly to keep the code small; relations are hidden with `@JsonIgnore`.

### Data Model

- **One owner, many pets.** `Pet` has `@ManyToOne owner` (foreign key `pet.owner_id`) and `Owner` has
  `@OneToMany pets`. This replaces the original `Owner.petId`, which allowed only one pet per owner.
- The pet JSON still exposes `ownerId`, read from the lazy association without loading the owner.
- An owner's pets are not embedded in the owner JSON. They come from the paged endpoint `GET /owners/{id}/pets`,
  so a response stays small however many pets an owner has.

### REST API

| Resource | Endpoints                                                                                            |
|----------|------------------------------------------------------------------------------------------------------|
| Pets     | `GET /pets`, `GET/PUT/PATCH/DELETE /pets/{id}`, `POST /pets`                                         |
| Owners   | `GET /owners`, `GET/PUT/PATCH/DELETE /owners/{id}`, `POST /owners`, `GET /owners/{id}/pets`          |

- `POST` returns `201 Created` with a `Location` header. `DELETE` returns `204 No Content`.
- `PUT` replaces all fields. `PATCH` changes only the fields in the request and also accepts
  `application/merge-patch+json` (RFC 7396). A missing field means "unchanged". An explicit `null` clears an
  optional field (`ownerId`, `address`), but required fields cannot be cleared.
- **Paging:** list endpoints take `page`, `size` and `sort` (e.g. `?page=0&size=20&sort=name,desc`). The default is
  20 items sorted by `id`, and `size` is capped at 100. Only an allow-list of fields can be used for sorting, and an
  unknown field returns 400. Responses use a stable `PageResponse` shape instead of Spring's `Page`.
- **Filtering:** list endpoints take optional filters, which are combined with AND and can be used together with
  paging and sorting.
  - Pets (`GET /pets`, `GET /owners/{id}/pets`): `name` (substring match, case-insensitive), `species` (one or
    more, e.g. `species=dog,cat`), `minAge` and `maxAge`. `GET /pets` also accepts `ownerId`, and
    `hasOwner=false` lists pets without an owner (e.g. to find them before reassigning).
  - Owners (`GET /owners`): `nameFirst`, `nameLast` and `address` (substring match, case-insensitive).
  - Filters are built with JPA Specifications. `%` and `_` in the input match literally, not as wildcards. Invalid
    values (unknown species, negative age, `minAge > maxAge`) return 400.
- **Deleting an owner who still has pets returns `409 Conflict`,** so pets are never changed as a hidden side effect.
  Reassign or delete the pets first.

### Validation and Error Handling

- Request bodies are checked with Bean Validation, e.g. a required non-blank name and an age between 0 and 100.
- Every error, including 401/403, returns the same JSON body:
  `{timestamp, status, error, message, path}`.
- Services throw domain exceptions (`ResourceNotFoundException`, `InvalidReferenceException`,
  `ResourceConflictException`) instead of HTTP-specific ones. `GlobalExceptionHandler` maps them to 404, 400 and 409.
- A reference to a missing owner in a request body is a `400`, not a `404`: the resource in the URL exists, and
  only the body is wrong.
- Unexpected errors return a generic `500`. The stack trace is logged on the server and never sent to the client.

### Security

- Stateless HTTP Basic authentication with three in-memory users, one per role (credentials in
  `application.properties`):
  - `reader` can only read;
  - `writer` can only create, update and delete;
  - `admin` can do both.
- Access rules are declared on each endpoint with `@CanRead` / `@CanWrite`. `EndpointSecurityCoverageTests` fails if
  any endpoint is missing an authorization annotation.

### Testing

- Integration tests with MockMvc against the in-memory H2 database cover every endpoint: success paths, validation,
  404/409, paging, sorting and filtering, and role rules for each user.
- Two guard tests enforce the project conventions: every endpoint declares authorization, and controllers do not
  access repositories directly.

### Trade-offs and Possible Improvements

- Responses return entities rather than dedicated response DTOs. Response DTOs would fully decouple the API from
  the database model.
- The in-memory users and H2 database are kept for simplicity. A real deployment would use a persistent database,
  a proper user store, and schema migrations (e.g. Flyway) instead of `ddl-auto=update`.
