# Arranca AgroTech completo, en orden:
#   1. El backend (Spring Boot) en su propia ventana, y espera a que responda.
#   2. El frontend (Vite) en otra ventana, que abre el navegador.
#
# Uso: doble clic en iniciar.bat, o desde PowerShell:  .\iniciar.ps1
#
# Los puertos salen de frontend\.env:
#   FRONTEND_PORT     puerto del frontend (por defecto 3000)
#   VITE_BACKEND_URL  direccion del backend (por defecto http://localhost:8080);
#                     el backend se arranca en el puerto de esa direccion.
#
# Para detener todo: cierre las dos ventanas (o Ctrl+C en cada una).

$ErrorActionPreference = 'Stop'
$Raiz = $PSScriptRoot
$Front = Join-Path $Raiz 'frontend'
$EsperaMaximaBackend = 300   # segundos; la primera vez Gradle compila y tarda mas

function Escribir-Paso($texto) { Write-Host "`n==> $texto" -ForegroundColor Green }
function Escribir-Aviso($texto) { Write-Host "    $texto" -ForegroundColor Yellow }
function Detener($texto) {
    Write-Host "`nERROR: $texto" -ForegroundColor Red
    exit 1
}

# Lee un archivo .env (CLAVE = valor) y devuelve un diccionario
function Leer-Env($ruta) {
    $valores = @{}
    if (Test-Path -LiteralPath $ruta) {
        foreach ($linea in Get-Content -LiteralPath $ruta) {
            if ($linea -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') { $valores[$Matches[1]] = $Matches[2] }
        }
    }
    return $valores
}

function Puerto-Ocupado([int]$puerto) {
    return [bool](Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue)
}

# El backend "responde" si contesta HTTP, aunque sea con un error: ya arranco
function Backend-Responde($url) {
    try {
        Invoke-WebRequest "$url/api/categorias" -UseBasicParsing -TimeoutSec 3 | Out-Null
        return $true
    } catch [System.Net.WebException] {
        return $null -ne $_.Exception.Response
    } catch {
        return $false
    }
}

# Abre una ventana nueva de PowerShell con un titulo y un comando
function Abrir-Ventana($titulo, $comando) {
    $script = "`$Host.UI.RawUI.WindowTitle = '$titulo'; $comando"
    $codificado = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($script))
    Start-Process powershell -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $codificado | Out-Null
}

function Texto-Literal($texto) { return "'" + ($texto -replace "'", "''") + "'" }

# ---------------------------------------------------------------
# Configuracion
# ---------------------------------------------------------------

$config = Leer-Env (Join-Path $Front '.env')
$urlBackend = if ($config['VITE_BACKEND_URL']) { $config['VITE_BACKEND_URL'].TrimEnd('/') } else { 'http://localhost:8080' }
$puertoBackend = ([Uri]$urlBackend).Port
$puertoFront = if ($config['FRONTEND_PORT']) { [int]$config['FRONTEND_PORT'] } else { 3000 }

Write-Host 'AgroTech - arranque' -ForegroundColor Cyan
Write-Host "    Backend:  $urlBackend"
Write-Host "    Frontend: http://localhost:$puertoFront"

# ---------------------------------------------------------------
# Herramientas necesarias
# ---------------------------------------------------------------

Escribir-Paso 'Revisando Java y Node'

# Gradle necesita Java para correr. Si no hay JAVA_HOME ni java en el PATH,
# se busca un JDK instalado en la carpeta habitual.
if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    $jdk = Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $jdk) { Detener 'No se encontro Java. Instale el JDK 25 o defina la variable JAVA_HOME.' }
    $env:JAVA_HOME = $jdk.FullName
    Escribir-Aviso "Se usa el JDK de $($jdk.FullName)"
}

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Detener 'No se encontro Node.js (npm). Instalelo desde https://nodejs.org y vuelva a intentar.'
}

if (-not (Test-Path -LiteralPath (Join-Path $Front 'node_modules'))) {
    Escribir-Paso 'Instalando las dependencias del frontend (solo la primera vez)'
    Push-Location -LiteralPath $Front
    try {
        npm install
        if ($LASTEXITCODE -ne 0) { Detener 'npm install fallo. Revise los mensajes de arriba.' }
    } finally {
        Pop-Location
    }
}

# ---------------------------------------------------------------
# 1. Backend
# ---------------------------------------------------------------

Escribir-Paso "Backend en el puerto $puertoBackend"

if (Puerto-Ocupado $puertoBackend) {
    if (Backend-Responde $urlBackend) {
        Escribir-Aviso 'Ya estaba corriendo: se usa ese.'
    } else {
        Detener "El puerto $puertoBackend esta ocupado por otro programa. Cierrelo o cambie VITE_BACKEND_URL en frontend\.env."
    }
} else {
    # Archivo que la ventana del backend escribe si Gradle termina (por ejemplo, por un error)
    $marca = Join-Path $env:TEMP "agrotech-backend-$PID.txt"
    Remove-Item -LiteralPath $marca -ErrorAction SilentlyContinue

    $comando = @(
        "Set-Location -LiteralPath $(Texto-Literal $Raiz)"
        "`$env:PORT = '$puertoBackend'"
        '.\gradlew.bat bootRun'
        "Set-Content -LiteralPath $(Texto-Literal $marca) -Value `$LASTEXITCODE"
        "Write-Host ''; Write-Host 'El backend se detuvo. Presione Enter para cerrar esta ventana.' -ForegroundColor Yellow"
        'Read-Host | Out-Null'
    ) -join '; '
    Abrir-Ventana 'AgroTech - Backend' $comando

    Write-Host '    Esperando a que el backend responda' -NoNewline
    $inicio = Get-Date
    while (-not (Backend-Responde $urlBackend)) {
        if (Test-Path -LiteralPath $marca) {
            Remove-Item -LiteralPath $marca -ErrorAction SilentlyContinue
            Detener ('El backend no pudo arrancar. Revise la ventana "AgroTech - Backend": ' +
                'lo mas comun es que MySQL no este encendido o que la clave de la base (DB_PASSWORD) no coincida.')
        }
        if (((Get-Date) - $inicio).TotalSeconds -gt $EsperaMaximaBackend) {
            Detener "El backend no respondio en $EsperaMaximaBackend segundos. Revise la ventana 'AgroTech - Backend'."
        }
        Write-Host '.' -NoNewline
        Start-Sleep -Seconds 2
    }
    Write-Host " listo ($([int]((Get-Date) - $inicio).TotalSeconds) s)"
}

# ---------------------------------------------------------------
# 2. Frontend
# ---------------------------------------------------------------

Escribir-Paso "Frontend en el puerto $puertoFront"

if (Puerto-Ocupado $puertoFront) {
    Escribir-Aviso "El puerto $puertoFront ya esta en uso: probablemente el frontend ya estaba corriendo."
    Escribir-Aviso "Abralo en http://localhost:$puertoFront o cambie FRONTEND_PORT en frontend\.env."
} else {
    # npm start abre el navegador solo (server.open en vite.config.mjs)
    Abrir-Ventana 'AgroTech - Frontend' "Set-Location -LiteralPath $(Texto-Literal $Front); npm start"
}

Write-Host ''
Write-Host 'AgroTech esta corriendo:' -ForegroundColor Cyan
Write-Host "    Aplicacion: http://localhost:$puertoFront"
Write-Host "    API:        $urlBackend/api"
Write-Host "    Swagger:    $urlBackend/swagger-ui.html"
Write-Host ''
Write-Host 'Para detenerlo, cierre las ventanas "AgroTech - Backend" y "AgroTech - Frontend".'
