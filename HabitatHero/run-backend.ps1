param(
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $projectRoot ".env"

if (-not (Test-Path $envFile)) {
    throw ".env file not found at $envFile"
}

$values = @{}
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
        $values[$Matches[1]] = $Matches[2]
    }
}

function Get-ValueOrDefault([string]$name, [string]$defaultValue) {
    if ($values.ContainsKey($name) -and -not [string]::IsNullOrWhiteSpace($values[$name])) {
        return $values[$name]
    }
    return $defaultValue
}

$dbHost = Get-ValueOrDefault "DB_HOST" "localhost"
$dbPort = Get-ValueOrDefault "DB_PORT" "5432"
$dbName = Get-ValueOrDefault "DB_NAME" "habitathero_db"
$dbUser = Get-ValueOrDefault "DB_USERNAME" "postgres"
$dbPassword = Get-ValueOrDefault "DB_PASSWORD" ""

if ([string]::IsNullOrWhiteSpace($dbPassword)) {
    throw "DB_PASSWORD is missing in .env"
}

# Export API key for DataPipelineService which reads DATAGOV_API_KEY via Spring property binding.
if ($values.ContainsKey("DATAGOV_API_KEY")) {
    $env:DATAGOV_API_KEY = $values["DATAGOV_API_KEY"]
}

$jdbcUrl = "jdbc:postgresql://$dbHost`:$dbPort/$dbName"
$bootArgs = @(
    "--server.port=$Port",
    "--spring.datasource.url=$jdbcUrl",
    "--spring.datasource.username=$dbUser",
    "--spring.datasource.password=$dbPassword"
) -join " "

Push-Location $projectRoot
try {
    mvn spring-boot:run "-Dspring-boot.run.arguments=$bootArgs"
}
finally {
    Pop-Location
}
