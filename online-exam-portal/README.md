# Online Examination Portal with Result Analysis

A web-based multiple-choice examination portal built with **Java 21, Spring Boot 4.0.8, Spring Security, Thymeleaf and MySQL**. Students register, log in, take a timed MCQ exam once, and see their score. Administrators manage the question bank and view every student's result.

> Server-rendered MVC application (Thymeleaf pages), not a JSON REST API. See [API / Endpoint Documentation](#api--endpoint-documentation) for all routes.

---

## Table of Contents
1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Project Structure](#project-structure)
5. [Database Design](#database-design)
6. [Prerequisites](#prerequisites)
7. [Setup, Build and Run](#setup-build-and-run)
8. [Configuration](#configuration)
9. [API / Endpoint Documentation](#api--endpoint-documentation)
10. [Sample Requests](#sample-requests)
11. [Application Workflow](#application-workflow)
12. [Security](#security)
13. [Known Limitations](#known-limitations)
14. [Future Enhancements](#future-enhancements)

---

## Features

### Student
- Registration with duplicate-username check; passwords hashed with BCrypt
- Login / logout (Spring Security form login)
- Dashboard showing the logged-in username; the "start exam" option is hidden once the exam is taken
- Multiple-choice exam page with an answered counter, progress bar and a **10-minute countdown timer that auto-submits** when it reaches zero
- Automatic scoring (1 mark per correct answer) and persistent storage of the result
- One attempt only, enforced **on the server** in both `GET /exam` and `POST /submit`
- Results page showing score, total questions and submission time

### Administrator
- Role-based access: everything under `/admin/**` requires the `ADMIN` role
- Dashboard with total questions, registered accounts and exam attempts
- Add and delete questions (with validation: all fields required, correct answer must be A/B/C/D)
- View all students' results

### Data and Configuration
- MySQL persistence through Spring Data JPA / Hibernate (`ddl-auto=update` creates the tables)
- Default admin account created at startup from configuration, not hard-coded
- 5 sample questions seeded automatically when the question table is empty
- Real `application.properties` is git-ignored; `application.properties.example` is provided

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.8 (Spring MVC) |
| Security | Spring Security (form login, BCrypt, role-based authorization, CSRF) |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8.0 |
| View | Thymeleaf, HTML, CSS, a little vanilla JavaScript (exam timer) |
| Build | Maven (Maven Wrapper included) |
| Tools | IntelliJ IDEA, Git, GitHub |

---

## Architecture

The application follows a layered MVC structure. Controllers talk to Spring Data repositories directly; there is no separate service layer.

```
 Browser
    │  HTTP (forms, session cookie)
    ▼
┌───────────────────────────────────────────────┐
│ Security layer  – SecurityConfig               │
│   authentication, role checks, CSRF, BCrypt    │
└───────────────────────────────────────────────┘
    ▼
┌───────────────────────────────────────────────┐
│ Controller layer                               │
│  AuthController · DashboardController          │
│  ExamController · ResultsController            │
│  AdminController · TestController              │
└───────────────────────────────────────────────┘
    ▼                              ▲
┌──────────────────────┐   ┌───────────────────┐
│ Repository layer      │   │ View layer         │
│ AppUserRepository     │   │ Thymeleaf templates│
│ QuestionRepository    │   │ + static CSS       │
│ ExamResultRepository  │   └───────────────────┘
└──────────────────────┘
    ▼
┌───────────────────────────────────────────────┐
│ Entity layer (JPA): AppUser · Question ·       │
│ ExamResult   ──►  MySQL: online_exam_portal    │
└───────────────────────────────────────────────┘

Startup runners: AdminInitializer (creates admin) · QuestionDataLoader (seeds questions)
```

### Class responsibilities

| Class | Responsibility |
|---|---|
| `OnlineExamPortalApplication` | Spring Boot entry point |
| `SecurityConfig` | Defines the BCrypt `PasswordEncoder`, loads users from the database for login, and sets URL access rules, login page, and logout behaviour |
| `AuthController` | Home redirect, login page, registration (validation, duplicate check, hashing, saves user with role `STUDENT`) |
| `DashboardController` | `/dashboard`: redirects admins to the admin dashboard; tells the student view whether the exam is already taken |
| `ExamController` | Shows the exam, blocks retakes, grades submitted answers, saves an `ExamResult` |
| `ResultsController` | Shows the logged-in student's own results |
| `AdminController` | Admin dashboard counts, question add/delete, all-results view |
| `AdminInitializer` | `CommandLineRunner` that creates the admin user from `app.admin.*` if missing |
| `QuestionDataLoader` | `CommandLineRunner` that seeds 5 sample questions when the table is empty |
| `AppUser`, `Question`, `ExamResult` | JPA entities (see Database Design) |
| `AppUserRepository`, `QuestionRepository`, `ExamResultRepository` | Spring Data JPA repositories, including the derived queries `findByUsername`, `existsByUsername`, `findByUsernameOrderBySubmittedAtDesc` |
| `TestController` | Health-check endpoint `GET /test` (login required) |

---

## Project Structure

```
Examportal/
└── online-exam-portal/
    ├── pom.xml
    ├── mvnw, mvnw.cmd
    └── src/
        ├── main/
        │   ├── java/com/examportal/        # controllers, config, entities, repositories
        │   └── resources/
        │       ├── application.properties.example
        │       ├── static/css/style.css
        │       └── templates/              # Thymeleaf pages
        │           ├── home.html  login.html  register.html
        │           ├── dashboard.html  exam.html  results.html
        │           └── admin-dashboard.html  admin-questions.html  admin-results.html
        └── test/java/com/examportal/OnlineExamPortalApplicationTests.java
```

---

## Database Design

Database name: `online_exam_portal`. Tables are generated by Hibernate.

### Entity description

**`app_users`** (`AppUser`)

| Column | Type | Constraints |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| username | VARCHAR | NOT NULL, UNIQUE |
| password | VARCHAR | NOT NULL (BCrypt hash) |
| role | VARCHAR | NOT NULL (`STUDENT` or `ADMIN`) |

**`question`** (`Question`, default table name)

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| question_text | VARCHAR | |
| option_a … option_d | VARCHAR | four choices |
| correct_answer | VARCHAR | one of `A`, `B`, `C`, `D` |

**`exam_results`** (`ExamResult`)

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK, auto-increment |
| username | VARCHAR | links to `app_users.username` by value (**no foreign key**) |
| score | INT | number of correct answers |
| total_questions | INT | question count at submission time |
| submitted_at | DATETIME | set when the exam is submitted |

### ER diagram

```
┌────────────┐ 1        0..1 ┌──────────────┐
│ app_users  │───────────────│ exam_results │
│ id (PK)    │  username     │ id (PK)      │
│ username   │  (by value)   │ username     │
│ password   │               │ score        │
│ role       │               │ total_questi.│
└────────────┘               │ submitted_at │
                             └──────────────┘
┌────────────┐
│ question   │   (standalone: one shared exam made of all rows)
│ id (PK)    │
│ question_text, option_a..d, correct_answer
└────────────┘
```

A student has at most one `exam_results` row in practice, because the controller rejects a second attempt. The database itself does not enforce this with a constraint.

---

## Prerequisites

- JDK 21
- MySQL 8.0 running locally (default port 3306)
- Git
- (Optional) Maven; the included wrapper (`mvnw`) is enough

---

## Setup, Build and Run

```bash
# 1. Clone
git clone https://github.com/ranjithramesh61827-art/Examportal.git
cd Examportal/online-exam-portal

# 2. Create the database
mysql -u root -p -e "CREATE DATABASE online_exam_portal;"

# 3. Create your local configuration from the template
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
#    then edit it: set your MySQL username/password and the admin credentials

# 4. Run
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Open **http://localhost:8080**.

To build a runnable jar instead:

```bash
./mvnw clean package
java -jar target/online-exam-portal-0.0.1-SNAPSHOT.jar
```

On first start the application will create the tables, create the admin account from your configuration, and insert 5 sample questions.

---

## Configuration

`src/main/resources/application.properties` (copy from `application.properties.example`; this file is git-ignored so credentials are never committed):

```properties
spring.application.name=examportal

spring.datasource.url=jdbc:mysql://localhost:3306/online_exam_portal
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.thymeleaf.cache=true

app.admin.username=YOUR_ADMIN_USERNAME
app.admin.password=YOUR_ADMIN_PASSWORD
```

| Property | Purpose |
|---|---|
| `spring.datasource.*` | MySQL connection |
| `spring.jpa.hibernate.ddl-auto=update` | Creates/updates tables automatically |
| `app.admin.username` / `app.admin.password` | Credentials of the admin account created on first startup (password is stored BCrypt-hashed) |

---

## API / Endpoint Documentation

The portal is server-rendered: pages return HTML and forms are submitted as `application/x-www-form-urlencoded`. All `POST` requests require a valid **CSRF token** (Spring Security default).

**Access:** *Public* = no login; *Auth* = any logged-in user; *Admin* = role `ADMIN`.

| Method | URL | Access | Request parameters | Response |
|---|---|---|---|---|
| GET | `/` | Public | none | Redirect to `/home` |
| GET | `/home` | Public | none | Home page (HTML) |
| GET | `/login` | Public | none | Login page (HTML) |
| POST | `/login` | Public | `username`, `password`, `_csrf` | Success: redirect to `/dashboard`. Failure: redirect to `/login?error` |
| POST | `/logout` | Auth | `_csrf` | Session invalidated, redirect to `/login?logout` |
| GET | `/register` | Public | none | Registration page (HTML) |
| POST | `/register` | Public | `username`, `password`, `_csrf` | Success: redirect to `/login?registered`. Failure: registration page with error ("Username and password are required." or "That username already exists.") |
| GET | `/dashboard` | Auth | none | Student dashboard (HTML); admins are redirected to `/admin/dashboard` |
| GET | `/exam` | Auth | none | Exam page with all questions. If already attempted: redirect to `/results` with a message. If no questions: redirect to `/dashboard` |
| POST | `/submit` | Auth | `question_<id>` = `A`/`B`/`C`/`D` for each question, `_csrf` | Exam page showing the score (`score / total`) and saves an `ExamResult`. If already attempted: redirect to `/results` |
| GET | `/results` | Auth | none | The logged-in user's results (HTML) |
| GET | `/test` | Auth | none | Plain text: `Spring Boot is working successfully!` |
| GET | `/admin/dashboard` | Admin | none | Counts of questions, users and attempts (HTML) |
| GET | `/admin/questions` | Admin | none | Question management page (HTML) |
| POST | `/admin/questions/add` | Admin | `questionText`, `optionA`, `optionB`, `optionC`, `optionD`, `correctAnswer` (A/B/C/D), `_csrf` | Redirect to `/admin/questions` with success or error message |
| POST | `/admin/questions/delete` | Admin | `id`, `_csrf` | Redirect to `/admin/questions` with success message |
| GET | `/admin/results` | Admin | none | All students' results (HTML) |
| GET | `/css/**`, `/js/**` | Public | none | Static resources |

---

## Sample Requests

### In the browser (simplest)
1. Open `http://localhost:8080/register`, create a student, then log in at `/login`.
2. Open `/test`. You should see `Spring Boot is working successfully!`.
3. Log in with the admin credentials from `application.properties` to reach `/admin/dashboard`.

### With curl
Because of CSRF protection, fetch a token and keep the session cookie first:

```bash
# Get login page + CSRF token
curl -s -c cookies.txt http://localhost:8080/login -o login.html
TOKEN=$(grep -o 'name="_csrf" value="[^"]*"' login.html | sed 's/.*value="//;s/"//')

# Log in (replace with your credentials)
curl -s -b cookies.txt -c cookies.txt -i \
  -d "username=YOUR_USER&password=YOUR_PASSWORD&_csrf=$TOKEN" \
  http://localhost:8080/login

# Call the health endpoint with the session cookie
curl -s -b cookies.txt http://localhost:8080/test
# -> Spring Boot is working successfully!
```

Unauthenticated access to a protected URL (e.g. `GET /exam`) returns a redirect to `/login`; a student calling `/admin/dashboard` receives `403 Forbidden`.

---

## Application Workflow

**Student**
1. Register at `/register` (role `STUDENT` is assigned automatically) → log in.
2. Dashboard → **Start Exam** (`/exam`).
3. Answer the MCQs; the timer counts down from 10:00 and auto-submits at 00:00.
4. `POST /submit` → score is calculated and saved → score is displayed.
5. `/results` lists the saved result. Any further attempt to open `/exam` is redirected to `/results`.

**Administrator**
1. Log in with the configured admin account → redirected to `/admin/dashboard`.
2. `/admin/questions`: add or delete questions.
3. `/admin/results`: view every student's score and submission time.

---

## Security

- Passwords stored as BCrypt hashes (never plain text)
- Session-based form login; logout invalidates the session and deletes the `JSESSIONID` cookie
- `/admin/**` restricted to `ROLE_ADMIN`; all other pages except home, login, register and static files require authentication
- CSRF protection enabled (Spring Security default); forms include the token
- Database and admin credentials kept out of Git via `.gitignore`
- Attempt limit is enforced server-side, not only by hiding a button

---

## Known Limitations

These are verified against the source code and are **not** implemented:

- **No edit-question feature.** Admins can add and delete questions but cannot edit them.
- **Timer is fixed and client-side.** The 10-minute limit is hard-coded in `exam.html` JavaScript; it is not configurable and the server does not enforce the time limit.
- **Single exam only.** All questions in the table form one shared exam; there is no concept of multiple independent exams.
- **No result analytics.** Results are listed (score, total, time) but there are no averages, rankings, charts or per-question analysis.
- No service layer, pagination, or automated tests beyond the default context-load test.
- `exam_results.username` is a plain column, not a foreign key to `app_users`.
- Admin pages print some debug output to the console (`System.out.println`).
- After submission, `exam.html` still shows a "Take Exam Again" link that points to `/`; a retake is rejected by the server.

## Future Enhancements

- Edit-question support
- Server-enforced, configurable exam duration
- Multiple exams with their own question sets and schedules
- Result analytics: class average, highest/lowest score, per-question accuracy, charts, CSV export
- Foreign key from results to users; unique constraint to enforce one attempt per exam
- Service layer, pagination, and unit/integration tests
- Question randomization and negative marking

---

## Author

Ranjith, CEG: [github.com/ranjithramesh61827-art](https://github.com/ranjithramesh61827-art)
