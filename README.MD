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

- Endpoints are secured with Spring Security. The default credentials are defined in the `application.yml` file.
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

### Users and Roles

Every API request needs HTTP Basic authentication. Users are defined in `src/main/resources/application.yml`;
the key under `users` is the username:

```yaml
app:
  security:
    users:
      reader:
        password: ...
        roles: [READER]
```

| Role     | Access                 |
|----------|------------------------|
| `READER` | read only (`GET`)      |
| `WRITER` | create, update, delete |
| `ADMIN`  | everything             |

- **Add a user:** add an entry with a `password` and `roles` under a new username. No code change is needed.
- **Several roles:** list them, e.g. `roles: [READER, WRITER]`. Role names are case-insensitive.
- **Override without editing the file:** use environment variables, e.g. `APP_SECURITY_USERS_ADMIN_PASSWORD`. Only
  that value changes; the other users stay. A new user can be added the same way
  (`APP_SECURITY_USERS_AUDITOR_PASSWORD` and `APP_SECURITY_USERS_AUDITOR_ROLES`).
- **Hashed passwords:** instead of plain text, a password can be given already hashed, with its algorithm prefix,
  e.g. `{bcrypt}$2y$10$...`. One way to create a BCrypt hash:
  `htpasswd -bnBC 10 "" 'the-password' | tr -d ':\n'`.
- An invalid configuration (a user without a password or roles, or an unknown role) stops the application at startup
  with an error naming the property.

Adding a new **role** still needs a code change: the role must be added to `Role` and to the access rules on the
endpoints (`@CanRead`, `@CanWrite`).

### Try It Out

- **Swagger UI:** open `http://localhost:8080/swagger-ui/index.html` (no login needed to view the docs), click
  **Authorize** and enter a username and password. "Try it out" then sends requests as that user; log in again as
  another user to switch roles.
- **Postman:** import `postman/PetManagement.postman_collection.json`. It has 31 requests with tests: CRUD for owners
  and pets, filtering and paging, the delete rules, and error and security cases. Credentials and the base URL are
  collection variables; set them to the values from `application.yml` before running. Run the whole
  collection in order (Collection Runner) with the server started, or from the command line:

```bash
npx newman run postman/PetManagement.postman_collection.json
```

- **curl:**

```bash
# Credentials from application.yml, as "username:password"
READER='<reader-username>:<reader-password>'
WRITER='<writer-username>:<writer-password>'

# Health check
curl -u "$READER" http://localhost:8080

# Dogs and cats, oldest first, 10 per page
curl -u "$READER" "http://localhost:8080/pets?species=dog,cat&sort=age,desc&size=10"

# Pets without an owner
curl -u "$READER" "http://localhost:8080/pets?hasOwner=false"

# Create an owner
curl -u "$WRITER" -X POST http://localhost:8080/owners \
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
- **DTOs in both directions:** the API never exposes JPA entities, so the JSON stays independent of the database model.
  `ControllerLayeringTests` enforces this.
  - Requests use `PetRequest`, `PetPatchRequest`, `OwnerRequest` and `OwnerPatchRequest`. They have no `id` field,
    so a client cannot overwrite another record by sending one.
  - Responses use `PetResponse` and `OwnerResponse`. Services map entities to responses inside the transaction, so
    lazy relations are loaded before the session closes (`spring.jpa.open-in-view=false`).

### Data Model

- **One owner, many pets.** `Pet` has `@ManyToOne owner` (foreign key `pet.owner_id`) and `Owner` has
  `@OneToMany pets`. This replaces the original `Owner.petId`, which allowed only one pet per owner.
- The pet JSON has `ownerId` (the same field as in requests) and a small `owner` summary (`id`, `nameFirst`,
  `nameLast`), so clients can show who owns a pet without a second request. Owners are loaded in the same query as the
  pets (`@EntityGraph`), so a page of pets does not trigger one extra query per pet (N+1).
- `Pet.setOwner(...)` keeps both sides of the relationship in sync. `Owner.getPets()` is read-only, so the two sides
  cannot drift apart.
- The database enforces the same rules as the API: required columns are `NOT NULL`, text columns have length limits,
  and `species` is stored by name rather than by ordinal.
- An owner's pets are not embedded in the owner JSON. They come from the paged endpoint `GET /owners/{id}/pets`,
  so a response stays small however many pets an owner has.

### REST API

| Resource | Endpoints                                                                                            |
|----------|------------------------------------------------------------------------------------------------------|
| Pets     | `GET /pets`, `GET/PUT/PATCH/DELETE /pets/{id}`, `POST /pets`, `GET /pets/{id}/history`,              |
|          | `GET /pets/{id}/history/as-of`                                                                       |
| Owners   | `GET /owners`, `GET/PUT/PATCH/DELETE /owners/{id}`, `POST /owners`, `GET /owners/{id}/pets`,         |
|          | `GET /owners/{id}/history`, `GET /owners/{id}/history/as-of`                                         |

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
- **Optimistic locking:** every entity has a `@Version`. Responses for a single resource carry it as an `ETag`
  header. A client can send that value back in `If-Match` on `PUT`, `PATCH` or `DELETE`; if someone else changed the
  resource in the meantime, the request fails with `412 Precondition Failed` instead of silently overwriting that
  change. `If-Match` is optional, so clients that don't use it keep working. Two transactions updating the same row at
  the same moment are rejected with `409 Conflict`.
- **Change history:** pets and owners are audited with Hibernate Envers. `GET /pets/{id}/history` and
  `GET /owners/{id}/history` list every change, newest first and paged (`page`, `size`): the revision, when it happened,
  who made it (`system` for the startup data), whether it was a creation, update or deletion, the version (the `ETag`
  at that time) and the full state after the change. A pet's state includes its owner as of that change. History stays
  available after a resource is deleted; the deletion entry shows its last state. Reassigning a pet is recorded in the
  pet's history only, not the owner's.
- **State at a point in time:** `GET /pets/{id}/history/as-of?time=2026-09-27T21:11:22Z` (and the same for owners)
  returns the resource as it was at that moment, in the same format as a history entry. Related data is shown as of
  the same moment: if a pet did not change but its owner was renamed before that time, the result has the new name.
  The time is ISO-8601 with a time zone (a `+` in the offset must be URL-encoded as `%2B`). It returns 404 if the
  resource did not exist yet or had already been deleted at that time.
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

- Stateless HTTP Basic authentication with in-memory users defined in `application.yml`
  (`SecurityUsersProperties`). Users can be added and given one or more roles through properties only:
  - `READER` can only read;
  - `WRITER` can only create, update and delete;
  - `ADMIN` can do both.
- The API documentation (Swagger UI and `/v3/api-docs`) is public so that users can open it and log in with its
  Authorize button. It describes the endpoints but returns no data; every API endpoint still requires authentication.
- The H2 console (`/h2-console`) is available to `ADMIN` only, because it can run any SQL against the database.
- Access rules are declared on each endpoint with `@CanRead` / `@CanWrite`. `EndpointSecurityCoverageTests` fails if
  any endpoint is missing an authorization annotation.

### Testing

- Integration tests with MockMvc against the in-memory H2 database cover every endpoint: success paths, validation,
  404/409, paging, sorting and filtering, and role rules for each user.
- Two guard tests enforce the project conventions: every endpoint declares authorization, and controllers do not
  access repositories directly.

### Trade-offs and Possible Improvements

- The change history is kept in the same in-memory H2 database, so it is lost on restart. It records only changes made
  through the application (Hibernate); changes made directly in the database, e.g. in the H2 console, are not
  recorded. Capturing those would need database triggers or change data capture (e.g. Debezium).
- The in-memory users and H2 database are kept for simplicity. A real deployment would use a persistent database,
  a proper user store, and schema migrations (e.g. Flyway) instead of `ddl-auto=update`.
