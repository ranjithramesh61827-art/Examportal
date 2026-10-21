# API / Endpoint Documentation

The Online Examination Portal is a **server-rendered Spring MVC application**. Endpoints return HTML pages or redirects, not JSON. Forms are sent as `application/x-www-form-urlencoded`.

- **Base URL (local):** `http://localhost:8080`
- **Authentication:** session cookie (`JSESSIONID`) issued after form login
- **CSRF:** every `POST` must include the `_csrf` token (the Thymeleaf forms add it automatically)
- **Roles:** `STUDENT` (assigned at registration) and `ADMIN` (created at startup from `app.admin.*`)

**Access levels:** *Public* = no login. *Auth* = any logged-in user. *Admin* = role `ADMIN` only.

## 1. Authentication and public pages

| Method | URL | Access | Request body / params | Success response | Error response |
|---|---|---|---|---|---|
| GET | `/` | Public | none | 302 redirect to `/home` | none |
| GET | `/home` | Public | none | 200 home page | none |
| GET | `/login` | Public | none | 200 login page | none |
| POST | `/login` | Public | `username`, `password`, `_csrf` | 302 to `/dashboard` | 302 to `/login?error` |
| POST | `/logout` | Auth | `_csrf` | 302 to `/login?logout`; session invalidated | none |
| GET | `/register` | Public | none | 200 registration page | none |
| POST | `/register` | Public | `username`, `password`, `_csrf` | 302 to `/login?registered` | 200 registration page with `error` message: `Username and password are required.` or `That username already exists.` |

Notes: the username is trimmed before use; the password is stored as a BCrypt hash; the new account always gets role `STUDENT`.

## 2. Student endpoints (login required)

| Method | URL | Access | Request body / params | Success response | Other responses |
|---|---|---|---|---|---|
| GET | `/dashboard` | Auth | none | 200 student dashboard (`username`, `examTaken`) | Admins: 302 to `/admin/dashboard` |
| GET | `/exam` | Auth | none | 200 exam page (list of questions, timer) | Already attempted: 302 to `/results` with message *"You have already taken the exam. Retakes are not allowed."*. No questions: 302 to `/dashboard` with message *"No exam questions are available yet."* |
| POST | `/submit` | Auth | `question_<id>` = `A`/`B`/`C`/`D` for each answered question (e.g. `question_1=A`), `_csrf` | 200 exam page showing `score` / `total`; an `ExamResult` row is saved | Already attempted: 302 to `/results` with the same message |
| GET | `/results` | Auth | none | 200 results page listing the user's results (newest first) | none |
| GET | `/test` | Auth | none | 200 plain text: `Spring Boot is working successfully!` | Not logged in: 302 to `/login` |

Scoring: 1 mark per question whose submitted letter equals the stored correct answer (case-insensitive). Unanswered questions score 0.

Example form body for `POST /submit`:

```
question_1=A&question_2=B&question_3=C&question_4=C&question_5=A&_csrf=<token>
```

## 3. Administrator endpoints (role ADMIN)

| Method | URL | Access | Request body / params | Success response | Error response |
|---|---|---|---|---|---|
| GET | `/admin/dashboard` | Admin | none | 200 page with `totalQuestions`, `totalUsers`, `totalAttempts` | Non-admin: 403 |
| GET | `/admin/questions` | Admin | none | 200 question management page | Non-admin: 403 |
| POST | `/admin/questions/add` | Admin | `questionText`, `optionA`, `optionB`, `optionC`, `optionD`, `correctAnswer` (A/B/C/D, case-insensitive), `_csrf` | 302 to `/admin/questions` with message *"Question added successfully!"* | 302 to `/admin/questions` with error *"Please fill in all question fields."* or *"Correct answer must be A, B, C, or D."* |
| POST | `/admin/questions/delete` | Admin | `id` (question id), `_csrf` | 302 to `/admin/questions` with message *"Question deleted successfully!"* | If the id does not exist: 302 to `/admin/questions` with no message |
| GET | `/admin/results` | Admin | none | 200 page listing every `ExamResult` | Non-admin: 403 |

There is no endpoint for editing a question.

## 4. Static resources

| Method | URL | Access | Response |
|---|---|---|---|
| GET | `/css/**` | Public | Stylesheets (e.g. `/css/style.css`) |
| GET | `/js/**` | Public | Allowed by the security rules; the project currently has no files here |

## 5. Data models

**ExamResult** (stored by `/submit`, displayed on `/results` and `/admin/results`)

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated |
| username | String | Student who submitted |
| score | int | Number of correct answers |
| totalQuestions | int | Number of questions at submission time |
| submittedAt | LocalDateTime | Submission timestamp |

**Question** (managed by the admin endpoints)

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated |
| questionText | String | The question |
| optionA - optionD | String | Answer choices |
| correctAnswer | String | `A`, `B`, `C` or `D` |

## 6. Trying it with curl

```bash
# 1. Fetch the login page and extract the CSRF token
curl -s -c cookies.txt http://localhost:8080/login -o login.html
TOKEN=$(grep -o 'name="_csrf" value="[^"]*"' login.html | sed 's/.*value="//;s/"//')

# 2. Log in (replace credentials)
curl -s -b cookies.txt -c cookies.txt -i \
  -d "username=YOUR_USER&password=YOUR_PASSWORD&_csrf=$TOKEN" \
  http://localhost:8080/login

# 3. Call an authenticated endpoint
curl -s -b cookies.txt http://localhost:8080/test
# Spring Boot is working successfully!

# 4. Protected pages without a session redirect to the login page
curl -s -i http://localhost:8080/exam | head -n 5
```

Spring Security rotates the CSRF token after login; for further `POST` requests, fetch a page that contains a form (for example `/admin/questions`) and read its `_csrf` value again.
