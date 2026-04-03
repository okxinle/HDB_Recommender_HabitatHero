# SC2006-TCE2-G24

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
4. **Postman** (for API testing)

---

## Step 1: Local Database Setup

Our application uses PostgreSQL and Hibernate. Hibernate will automatically create all the tables (`user_accounts`, `user_profiles`, `hdb_blocks`, `audit_logs`) for you, but **you must create the empty database first.**

1. Open **pgAdmin** (or your psql CLI).
2. Connect to your local PostgreSQL server (running on port `5432`).
3. Right-click on Databases -> Create -> Database.
4. Name the database **EXACTLY**: `habitathero_db`
5. Save.

## Step 2: Configure Application Properties

By default, the application is expecting specific database credentials. If your local PostgreSQL uses a different password, you must update the config file.

1. Navigate to `src/main/resources/application.properties`.
2. Locate the database connection lines:

```properties
spring.datasource.username=postgres
spring.datasource.password=password123
```

Change `password123` to whatever password you set when you installed PostgreSQL. (Note: Do not commit your personal password changes to GitHub!)

## Step 3: Data.gov.sg API Key

Our HDB synchronization pipeline (`DataPipelineService.java`) pulls real-time resale data from Data.gov.sg. To prevent the system from crashing with a `429 Too Many Requests` error, you must use your own API key.

1. Go to Data.gov.sg and create a free account.
2. Generate an API Key from your account dashboard.
3. Open `src/main/java/habitathero/control/DataPipelineService.java`.
4. Locate line 47:

```java
headers.set("x-api-key", "YOUR_DATAGOVSG_API_KEY_HERE"); 
```

Replace `"YOUR_DATAGOVSG_API_KEY_HERE"` with your actual key.

## Step 4: Build and Run the Server

Whenever you pull new code, it is highly recommended to clean the old build files to prevent hidden compilation errors.

Open your terminal in the root folder (where the `pom.xml` is located) and run:

```bash
mvn clean spring-boot:run
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

---

## Troubleshooting

* **`ApplicationContextException: Unable to start web server`**: Port 8080 is already in use by another app, or your PostgreSQL database is turned off.
* **`403 Forbidden` on `/api/admin/` endpoints**: This means your token is working, but you are not recognized as an Admin. Ensure you ran the SQL `UPDATE` command in Test 1b and **logged in again** to get a fresh token.
* **`500 Internal Server Error` on Login/Register**: Ensure your JSON body strictly uses lowercase keys (`email`, `password`) and valid formatting.
