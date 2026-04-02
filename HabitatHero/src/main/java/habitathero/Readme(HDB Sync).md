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
headers.set("api-key", "YOUR_DATAGOVSG_API_KEY_HERE"); 
```

Replace `"YOUR_DATAGOVSG_API_KEY_HERE"` with your actual key.

## Step 4: Build and Run the Server

Whenever you pull new code, it is highly recommended to clean the old build files to prevent hidden compilation errors.

Open your terminal in the root folder (where the `pom.xml` is located) and run:

```bash
mvn clean spring-boot:run
```

If successful, you will see `Tomcat started on port(s): 8080 (http)` and `Started Main in X seconds` in the terminal.

## Step 5: Verifying the Setup

To ensure your database and API connections are working perfectly, run these two tests in Postman:

### Test 1a: Register an Account (Generates your VIP Token)
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

### Test 1b: Login to an Existing Account (If Token Expires or Restarting)
* **Method:** POST
* **URL:** `http://localhost:8080/api/auth/login`

### Test 2: Trigger the HDB Data Sync
* **Method:** POST
* **URL:** `http://localhost:8080/api/admin/trigger-sync`
* **Auth:** Go to the Authorization tab -> Select "Bearer Token" -> Paste your JWT token.
* **Expected:** `200 OK` with the message "Sync triggered and completed successfully." You should also see Hibernate logging the database inserts in your Spring Boot terminal!



---

## Troubleshooting

* **`ApplicationContextException: Unable to start web server`**: Usually means Port 8080 is already in use by another app, or your PostgreSQL database is turned off.
* **`403 Forbidden` on Login/Register**: Ensure your Postman request is set to POST and not GET.
* **`500 Internal Server Error` on Login/Register**: Ensure your JSON body strictly uses lowercase keys (`email`, `password`) and valid formatting.