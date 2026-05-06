# Habitat Hero 🏠👨‍👩‍👧‍👦

## Run Locally (Windows)

### Backend requirements
1. Java JDK 17
	- Verify: `java -version`
2. Apache Maven 3.9+
	- Verify: `mvn -v`
	- If `mvn` is not found in the current terminal, add Maven to PATH:
	- `$env:Path += ";<your-maven-install-path>\\bin"`

### Backend run commands
1. From workspace root (folder containing HabitatHero), run:
	- `cd .\HabitatHero`
2. `mvn clean compile`
3. `mvn spring-boot:run`

Notes:
- Always run Maven commands from the HabitatHero folder (same folder as pom.xml).
- If port 8080 is already used, stop the running process first or change server port.

### Frontend requirements
1. Node.js 18+ and npm
	- Verify: `node -v`
	- Verify: `npm -v`

### Frontend run commands
1. From workspace root (folder containing HabitatHero), run:
	- `cd .\HabitatHero\src\frontend`
2. `npm install`
3. `npm start`

### Vercel deployment
1. Point the Vercel project at the repository root.
2. Let Vercel use the root-level `vercel.json` so it builds `HabitatHero/src/frontend` and rewrites all SPA routes to `index.html`.
3. If you change the frontend folder name later, update the paths in `vercel.json`.

## Python dependencies
Use [HabitatHero/requirements.txt](HabitatHero/requirements.txt) for Python package dependencies.
A requirements.txt file should include all direct and indirect Python package dependencies needed for a project, typically with specific version numbers to ensure reproducibility. It is used to share projects and manage dependencies, usually created via `pip freeze > requirements.txt`.

# Habitat Hero - Backend Setup Guide

This document outlines the steps required for all team members to set up and run the Spring Boot backend on their local machines. 

## ⚠️ Prerequisites

Before starting, ensure you have the following installed on your machine:
1. **Java 17** (or higher)
2. **Maven**
3. **PostgreSQL** (and pgAdmin for easy management)
4. **PostGIS extension** for PostgreSQL
5. **Postman** (for API testing)

---

## Step 1: Local Database Setup

Our application uses PostgreSQL and Hibernate. Hibernate will automatically create all the tables (`user_accounts`, `user_profiles`, `hdb_blocks`, `audit_logs`) for you, but **you must create the empty database first.**

1. Open **pgAdmin** (or your psql CLI).
2. Connect to your local PostgreSQL server (running on port `5432`).
3. Right-click on Databases -> Create -> Database.
4. Name the database **EXACTLY**: `habitathero_db`
5. Save.

## Step 2: Create .env Configuration

This backend reads DB and API settings from a local `.env` file (not hardcoded credentials in code).

1. In the `HabitatHero` folder (same level as `pom.xml`), create a file named `.env`.
2. Add the following values:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=habitathero_db
DB_USERNAME=postgres
DB_PASSWORD=<your_postgres_password>

# Required for HDB sync endpoint (/api/admin/trigger-sync)
DATAGOV_API_KEY=<your_datagov_api_key>

# Optional but recommended to reduce OneMap throttling
ONEMAP_API_TOKEN=<your_onemap_api_token>
```

Notes:
- `DB_PASSWORD` must not be empty.
- Do not commit your personal `.env` values to GitHub.

## Step 3: API Keys

1. **Data.gov.sg API key**
  - Required for HDB resale sync (`/api/admin/trigger-sync`).
  - Create a Data.gov.sg account and generate an API key.
  - Set it as `DATAGOV_API_KEY` in `.env`.
2. **OneMap API token (optional but recommended)**
  - Improves geocoding reliability and reduces throttling.
  - Set it as `ONEMAP_API_TOKEN` in `.env`.

## Step 4: Build and Run the Server

Whenever you pull new code, it is highly recommended to clean the old build files to prevent hidden compilation errors.

Open your terminal in the root folder (where the `pom.xml` is located) and run:

```bash
mvn clean compile
.\run-backend.ps1
```

If successful, you will see `Tomcat started on port(s): 8080 (http)` and `Started Main in X seconds` in the terminal.

## Step 5: Verifying the Setup & Syncing Data

To ensure your database and API connections are working perfectly, run these tests in Postman. **You must run these in exact order.**

### Test 1a: Register an Account 
* **Method:** POST
* **URL:** `http://localhost:8080/api/auth/register`
* **Body (raw JSON):**
  ```json
  {
      "email": "test@habitathero.com",
      "password": "password123"
  }
  ```
* **Expected:** `200 OK` and a response containing your long JWT token string. Copy this token.

### Test 1b: Promote Your Account to Admin (Crucial)
By default, newly registered accounts are standard users and cannot trigger data syncs. You must manually promote your test account.
1. Open the **Query Tool** in pgAdmin for the `habitathero_db`.
2. Run this exact SQL command: 
   ```sql
   UPDATE user_accounts SET role = 'ADMIN';
   ```
3. **Important:** Go back to Postman and run a **POST** to `http://localhost:8080/api/auth/login` using the exact same JSON body as Test 1a. Copy the *new* long JWT token string. This is your Admin Bearer Token.

### Test 2: Trigger the HDB Data Sync
* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/trigger-sync`
* **Auth:** Go to the Authorization tab -> Select "Bearer Token" -> Paste your JWT Admin token.
* **Expected:** `200 OK`. You should see Hibernate logging the database inserts in your terminal. This downloads the raw resale pricing (~228,000 rows).

### Test 3: Initialize Spatial Data (The "Master Map")
The raw dataset lacks coordinates. This step automatically downloads the GeoJSON map of Singapore and creates a spatial reference table.
* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/init-hdb-building`
* **Auth:** Go to the Authorization tab -> Select "Bearer Token" -> Paste your JWT Admin token.
* **Expected:** `200 OK`. Check your terminal to watch the GeoJSON parsing.

### Test 4: Backfill HDB Coordinates
This final step matches our pricing records to the spatial map so they can be plotted and routed by the recommendation engine.
* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/backfill-coordinates`
* **Auth:** Go to the Authorization tab -> Select "Bearer Token" -> Paste your JWT Admin token.
* **Expected:** `200 OK`. The response payload will show `candidatesScanned`, `updatedBlocks`, and `unresolvedBlocks`. Check your terminal to watch the service batch-process the coordinates!

### Test 5: Initialize Land Use Dataset (Future Development Risk)
This step prepares `land_use_dataset` used by future development risk checks.

#### PostGIS Installation and Requirement

For Windows Users:
1. Open Stack Builder (found in the PostgreSQL folder in your Start Menu).
2. Select your PostgreSQL server and click Next.
3. Expand Spatial Extensions and check the box for PostGIS 3.X Bundle.
4. Follow the installation prompts (select No if asked to create a separate spatial database).

For macOS Users:
1. Homebrew: Run `brew install postgis` in terminal and restart PostgreSQL.
2. Postgres.app: PostGIS is included by default; no extra installation is required.

#### Enable Spatial Capabilities

Once installed, activate the extension in `habitathero_db`:
1. Open pgAdmin 4 and connect to `habitathero_db`.
2. Open Query Tool and run:

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
SELECT PostGIS_Version();
```

#### Dataset Initialization (API Call)

* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/init-land-use`
* **Auth:** Go to the Authorization tab -> Select "Bearer Token" -> Paste your JWT Admin token.
* **Expected Response:**
  * `message`: `land_use_dataset initialized and imported successfully.`
  * `tableReady`: `true`
  * `downloadedGeoJson`: `true`
  * `importedToDb`: `true`

## After Pull: HDB Coordinate Refresh (Team Runbook)

Use this after pulling latest backend changes related to HDB coordinate mapping.

### 1. Pull and run backend
```bash
git pull
cd .\HabitatHero
mvn clean spring-boot:run
```

### 2. Login as admin and get JWT
* **Method:** POST
* **URL:** `http://localhost:8080/api/auth/login`
* **Body (raw JSON):**
  ```json
   {
      "email": "test@habitathero.com",
      "password": "password123"
  }
  ```

### 3. (Recommended) set OneMap API token before starting backend
In PowerShell:
```powershell
$env:ONEMAP_API_TOKEN="<your_onemap_token>"
```

This reduces OneMap throttling and improves backfill completion.

### 4. Run only backfill first
* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/backfill-coordinates`
* **Auth:** `Authorization: Bearer <admin_jwt>`

Repeat this endpoint a few times until `unresolvedBlocks` stabilizes.

### 5. Use reingest only for full reset
* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/reingest-hdb-coordinates`

This endpoint clears existing `postal_code/coordinates` and rebuilds from scratch. Do not use this unless a full reset is intended.

### 6. Purge Placeholder Spatial Cache Rows (Recommended After Geospatial Fixes)
If historical cache rows were generated from fallback/default payloads, clear them first:

* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/cache/purge-placeholders`
* **Auth:** `Authorization: Bearer <admin_jwt>`

This endpoint removes invalid placeholder rows from:
- `sun_facing_analysis_result`
- `transport_line_cal_result`

### 7. Precompute Spatial Cache For All Postals
Warm the cache so first user requests do not pay compute cost.

* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/cache/precompute-spatial`
* **Auth:** `Authorization: Bearer <admin_jwt>`

#### Example A: Sun + Noise for all postals
```json
{}
```

#### Example B: Dry run / partial warm-up (first 200 postals)
```json
{
  "limit": 200
}
```

#### Example C: Include Future-Development Risk cache too
```json
{
  "includeFutureRisk": true,
  "futureRiskDistance": 500
}
```

Notes:
- `includeFutureRisk` defaults to `false`.
- `futureRiskDistance` defaults to `500` meters.
- For current recommendation flow, sun/noise precompute is essential. Future-risk precompute is optional unless your frontend calls future-risk heavily.

## POI Data Ingestion (Functional Steps 1-6)

Use this flow when you want live Singapore POI data for Convenience scoring.

### Step 1: Start Backend
Run the Spring Boot backend first so the admin API is reachable.

### Step 2: Install Python Dependency
From the `HabitatHero` folder:

```bash
pip install requests
```

### Step 3: Prepare a Valid Login
The endpoint `/api/admin/pois/load` is protected, so the script must authenticate.
Use any valid application account (email and password).

### Step 4: Run POI Fetch + Load Script
From the `HabitatHero` folder, run:

```bash
python fetch_sg_pois.py --chunk-size 500 --email <your_email> --password <your_password>
```

This script fetches OSM POIs for Singapore and loads them into `point_of_interest`.

### Step 5: What Data Is Loaded
The script ingests and maps these categories:
- `amenity=school` -> `SCHOOL`
- `amenity=food_court` or `amenity=hawker_centre` -> `HAWKER_CENTRE`
- `shop=supermarket` -> `SUPERMARKET`
- `leisure=park` -> `PARK`
- `amenity=hospital` -> `HOSPITAL`
- `leisure=playground` -> `PLAYGROUND`

### Step 6: Verify in pgAdmin
Refresh your server. You should see "point_of_interest" table.

Run these SQL checks:

```sql
SELECT COUNT(*) FROM point_of_interest;
SELECT category, COUNT(*) FROM point_of_interest GROUP BY category ORDER BY category;
```

---

## Troubleshooting

* **`ApplicationContextException: Unable to start web server`**: Port 8080 is already in use by another app, or your PostgreSQL database is turned off.
* **`403 Forbidden` on `/api/admin/` endpoints**: This means your token is working, but you are not recognized as an Admin. Ensure you ran the SQL `UPDATE` command in Test 1b and **logged in again** to get a fresh token.
* **`500 Internal Server Error` on Login/Register**: Ensure your JSON body strictly uses lowercase keys (`email`, `password`) and valid formatting.
