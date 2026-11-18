# Transporter Assignment on Lanes

A Spring Boot service that assigns transporters to lanes across India. It finds a **proven-optimal** assignment: every lane gets exactly one transporter that quoted for it, at most `maxTransporters` distinct transporters are used, as many transporters as possible are used, and the total cost is minimized.

## Tech stack

| | |
|---|---|
| Language / framework | Java 21, Spring Boot 3.5 (Web, Data JPA, Validation) |
| Database | PostgreSQL (Supabase in this setup), schema managed by Flyway |
| Optimizer | Google OR-Tools CP-SAT (exact integer programming) |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, AssertJ, MockMvc, brute-force oracle |

## Project layout

```
.
├── README.md
├── docker-compose.yml
├── .vscode/                       # VS Code: JDK 21 runtime, launch config, recommended extensions
└── backend/
    ├── pom.xml, mvnw              # Maven build (wrapper included)
    ├── Dockerfile
    ├── .env.example               # copy to .env (git-ignored)
    ├── postman/                   # Postman collection with the PDF test cases
    └── src/main/java/com/freightfox/transporter/
        ├── controller/            # HTTP layer only
        ├── dto/request|response/  # API contracts + bean validation
        ├── service/               # input validation, persistence, assignment orchestration
        ├── optimization/          # pure optimization logic (no Spring/DB): model, solver, verifier
        ├── entity/, repository/   # JPA
        ├── mapper/                # DTO <-> entity/result
        ├── exception/             # domain exceptions + global handler
        └── config/                # OpenAPI, optimizer properties
```

## Architecture

`Controller → Service → Optimizer`, with repositories behind the services.

- **Submitting input** validates the request (field rules plus cross-field rules: duplicate ids, duplicate quotes, quotes for unknown lanes). It then saves the whole submission as a new `input_dataset` in a single transaction. Submissions never overwrite each other.
- **Requesting an assignment** loads one dataset with two queries (lane ids, then all quotes projected straight to ids, so no N+1). It then runs the optimizer outside any database transaction.
- **Before responding**, `SolutionVerifier` checks the result independently: every lane is covered once by a transporter with a real quote, no more than K transporters are used, every selected transporter is used, and `totalCost` equals the sum of the assigned quotes.

### Database schema

```
input_dataset (id, created_at)
  ├─< lane        (id, dataset_id FK, lane_ref, origin, destination)        UNIQUE(dataset_id, lane_ref)
  └─< transporter (id, dataset_id FK, transporter_ref, name)                UNIQUE(dataset_id, transporter_ref)
lane_quote (id, lane_id FK, transporter_id FK, quote NUMERIC(14,2) > 0)     UNIQUE(transporter_id, lane_id)
```

`lane_ref` and `transporter_ref` are the ids you send in the API. They only need to be unique within one submission. Money uses `BigDecimal` / `NUMERIC` throughout.

## Getting started

### Prerequisites
- JDK 21 (`brew install openjdk@21`)
- A PostgreSQL database
- Docker is optional

### Configure the database
```bash
cp backend/.env.example backend/.env   # then set DB_URL, DB_USERNAME, DB_PASSWORD
```
Example `DB_URL` for Supabase: `jdbc:postgresql://<host>.pooler.supabase.com:5432/postgres?sslmode=require`.
Flyway creates the schema automatically on first start.

### Run
```bash
cd backend
./mvnw spring-boot:run
# or
./mvnw package -DskipTests && java -jar target/transporter-assignment-1.0.0.jar
# or, from the repo root
docker compose up --build
```

In VS Code, open the repo root, accept the recommended extensions, and run the **TransporterApplication** launch configuration.

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Using the API

### 1. Submit input: `POST /api/v1/transporters/input`

Request body: the format from the assignment, i.e. `lanes[]`, and `transporters[]` each with `laneQuotes[]`.

```bash
curl -X POST localhost:8080/api/v1/transporters/input -H 'Content-Type: application/json' -d '{
  "lanes": [{"id": 1, "origin": "Mumbai", "destination": "Delhi"}, {"id": 2, "origin": "Delhi", "destination": "Bangalore"}],
  "transporters": [
    {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 5000}, {"laneId": 2, "quote": 6000}]},
    {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 2, "quote": 5500}]}
  ]}'
```
**201**
```json
{"status": "success", "message": "Input data saved successfully.", "datasetId": 1}
```

Validation rules:
- `lanes` and `transporters` must not be empty.
- Ids must be positive and unique.
- Origin, destination and name are required.
- Each quote must be greater than 0 with at most 2 decimals.
- Each quote must reference an existing lane, and a transporter can quote a given lane only once.
- A transporter with an empty `laneQuotes` list is allowed; it is simply never assigned.

### 2. Get the assignment: `POST /api/v1/transporters/assignment`

```bash
curl -X POST localhost:8080/api/v1/transporters/assignment -H 'Content-Type: application/json' -d '{"maxTransporters": 3}'
```
**200**
```json
{
  "status": "success",
  "totalCost": 60139,
  "assignments": [{"laneId": 1, "transporterId": 5}, {"laneId": 2, "transporterId": 1}, {"laneId": 3, "transporterId": 4}, {"laneId": 4, "transporterId": 4}, {"laneId": 5, "transporterId": 4}],
  "selectedTransporters": [1, 4, 5]
}
```

`datasetId` is optional. Without it, the most recently submitted dataset is used. Pass it explicitly to optimize one specific submission, for example when several clients submit at the same time.

### Errors

All errors use the same shape: `{"status": "error", "message": "...", "errors": [...]}`.

| Status | When |
|---|---|
| 400 | Validation failed, malformed JSON, or `maxTransporters` < 1 |
| 404 | No dataset submitted yet, or unknown `datasetId` |
| 409 | No assignment is possible: a lane has no quotes, or K is below the minimum needed (the message states the minimum) |
| 503 | The solver could not prove the optimum within the time budget |
| 500 | Unexpected error (generic message; details are logged only) |

The Postman collection is at `backend/postman/transporter-assignment.postman_collection.json`.

## Optimization algorithm

The problem is modelled as a 0/1 integer program. c(i,j) is transporter j's quote for lane i; a pair with no quote has no variable at all, so it can never be assigned.

```
x(i,j) = 1  if lane i is assigned to transporter j
y(j)   = 1  if transporter j is used

Σ_j x(i,j) = 1       every lane assigned exactly once
x(i,j) ≤ y(j)        only used transporters get lanes
y(j) ≤ Σ_i x(i,j)    every used transporter gets at least one lane
Σ_j y(j) ≤ K
```

**Objective policy.** The assignment asks for both "use the maximum number of transporters" and "minimize cost", but doesn't say which wins when they conflict. The default policy is `MAX_UTILIZATION_THEN_COST`, solved in stages rather than by blending the goals with penalty weights:

1. Maximize the number of transporters used (up to K) and fix that number.
2. Minimize total cost and fix it.
3. Tie-break: minimize Σ x(i,j)·rank(j), so lower transporter ids are preferred among equally optimal answers.

Set `OPTIMIZER_POLICY=COST_THEN_UTILIZATION` to make cost the first priority instead. The policies can differ: on Test Case 1 with K=5, the default uses 5 transporters at 63,388, while cost-first uses 4 transporters at 59,896.

**Why CP-SAT?**
- This problem contains set cover and p-median as special cases, both NP-hard.
- A greedy "cheapest quote per lane" approach can break the K limit and is not globally optimal.
- CP-SAT returns a *proven* optimum. Costs are converted to whole paise, so the arithmetic is exact.
- Runs are deterministic: fixed seed, interleaved search, and a single-threaded tie-break stage.
- The model is linear in the input size: one variable per quote plus one per transporter.

**Measured performance** (8 solver threads):

| Input size | Quote density | Result |
|---|---|---|
| 1,000 lanes × 100 transporters (~10k quotes) | ~10% (sparse) | ~2 s |
| 100 lanes × 20 transporters, K=5 | 40% (dense) | ~1.2 s |
| 200 lanes × 30 transporters | 40%, tight K | not proven within 20 s (also true for the SCIP MIP solver) |

When optimality can't be proven within `OPTIMIZER_TIME_LIMIT_SECONDS`, the API returns 503 rather than a possibly sub-optimal answer.

**How correctness is tested.** Besides the unit scenarios, `CpSatAssignmentOptimizerTest` solves 400 random small instances with heavy cost ties, under both policies. It compares the optimizer against `BruteForceAssignmentOptimizer`, which enumerates every possible assignment, on transporter count, total cost and tie-break value.

## Testing

```bash
cd backend
createdb transporter_test   # one-time: integration tests use a local, disposable database
./mvnw test
```

Integration tests use the `test` profile, which points at `localhost:5432/transporter_test` by default. Override it with `TEST_DB_URL`, `TEST_DB_USERNAME` and `TEST_DB_PASSWORD`. Tests never touch the database in `.env`.

| Suite | Covers |
|---|---|
| `CpSatAssignmentOptimizerTest` | All 3 PDF datasets; K=1; K larger than the number of transporters; a lane with no quote; identical quotes and tie-breaks; determinism; unused quotes; decimal quotes; 1,000-lane input; the timeout path; brute-force comparison |
| `SolutionVerifierTest` | Detection of invalid quote, uncovered lane, too many transporters, unused selected transporter, wrong total |
| `InputValidatorTest` | Duplicate lanes, transporters and quotes; unknown lane references |
| `TransporterApiIntegrationTest` | Persistence, rollback after a partial write, 400/404/409 responses, full PDF runs end to end, dataset isolation |

## Assumptions and notes on the assignment PDF

The sample outputs in the PDF were checked by exhaustive search, and **both are inconsistent with their own input data**. They are not used as expected results.

**API example (10 lanes, K=3).** No valid assignment exists:
- The sample assigns lanes 7, 8, 9 and 10 to transporters that never quoted for them.
- Lanes 5–6 can only go to T3, lanes 7–8 only to T4, and lanes 9–10 only to T5. Lanes 1–2 also need T1 or T6, and lanes 3–4 need T2 or T7. That means at least 5 transporters, so K=3 correctly returns **409**.
- With K≥5 the optimum is 48,000.

**Test Case 1 (K=3).** The sample is wrong in three ways:
- It lists 3 selected transporters but uses 4 (T2, T3, T4, T6).
- Its own assignments add up to 69,001, not the stated 75,000.
- The proven optimum is **60,139** (T5→lane 1, T1→lane 2, T4→lanes 3–5).

**Test Case 2.** No expected output is given. The computed results:
- K≤2 is infeasible (409).
- K=3 → 7,500.
- K=4 → **7,450**, because transporter W is cheaper on lane 4.

Other assumptions:
- "Every lane is assigned to at least one transporter" is implemented as exactly one. A second transporter on the same lane would only add cost.
- Each submission is a separate, immutable dataset. The assignment API uses the latest one unless `datasetId` is given.
- Quotes must be greater than 0 with at most 2 decimal places (rupees and paise).
- The executable jar is about 170 MB because OR-Tools bundles native libraries for every platform.
