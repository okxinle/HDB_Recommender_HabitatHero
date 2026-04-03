# HabitatHero - Local Environment Setup Guide

## Windows Setup (Your Machine Only)

### Step 1: Set Environment Variables in Windows

**Option A: Using PowerShell (Recommended)**

Open PowerShell as Administrator and run:

```powershell
[System.Environment]::SetEnvironmentVariable('DB_USERNAME', 'postgres', 'User')
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', '09J289242n04', 'User')
[System.Environment]::SetEnvironmentVariable('DATAGOV_API_KEY', 'your_actual_api_key_here', 'User')
```

Replace `your_actual_api_key_here` with your real Data.gov.sg API key.

**Option B: Using Environment Variables GUI**

1. Press `Win + X` → Select "System"
2. Click "Advanced system settings"
3. Click "Environment Variables" button
4. Under "User variables", click "New"
5. Add each variable:
   - Variable name: `DB_USERNAME` → Value: `postgres`
   - Variable name: `DB_PASSWORD` → Value: `09J289242n04`
   - Variable name: `DATAGOV_API_KEY` → Value: `your_api_key_here`
6. Click OK and restart your terminal

### Step 2: Verify Environment Variables

Open a NEW PowerShell and verify they're set:

```powershell
$env:DB_USERNAME
$env:DB_PASSWORD
$env:DATAGOV_API_KEY
```

Should display your values.

### Step 3: Start the Application

Now run the backend from the HabitatHero folder:

```powershell
cd C:\Users\xinle\OneDrive - Nanyang Technological University\Y2S2\SC2006 Software Engineering\SC2006-TCE2-G24\HabitatHero
mvn clean spring-boot:run
```

The application will now read credentials from environment variables instead of hardcoded values.

---

## For Your Team (GitHub Setup)

**DO NOT** share your `.env` file with teammates. Instead:

1. Each teammate copies `.env.example` to `.env` locally (on their machine only)
2. They update their own credentials
3. `.env` is in `.gitignore` and won't be committed
4. The code safely reads from environment variables

---

## What Changed?

- ✅ `application.properties` now uses `${DB_PASSWORD}` instead of hardcoded password
- ✅ `DataPipelineService.java` reads API key from `DATAGOV_API_KEY` environment variable
- ✅ `.gitignore` now excludes `application.properties` and `.env` files
- ✅ Pushed code will have NO secrets exposed

---

## If You Still See the Old Password in Code

After updating environment variables, delete the old build:

```powershell
mvn clean
```

Then run again:

```powershell
mvn clean spring-boot:run
```

This ensures the new code is compiled with the environment variable logic.
