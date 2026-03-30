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