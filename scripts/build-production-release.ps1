$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$keystorePath = Join-Path $projectRoot "standby-release.keystore"
$keyAlias = "standby-release"

$keytoolCommand = Get-Command keytool.exe -ErrorAction SilentlyContinue
if (-not $keytoolCommand -and $env:JAVA_HOME) {
    $javaKeytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
    if (Test-Path -LiteralPath $javaKeytool) {
        $keytoolPath = $javaKeytool
    }
}
elseif ($keytoolCommand) {
    $keytoolPath = $keytoolCommand.Source
}

if (-not $keytoolPath) {
    throw "JDK keytool was not found. Install JDK 17 or set JAVA_HOME to your JDK."
}

$createdKeystore = -not (Test-Path -LiteralPath $keystorePath)
if ($createdKeystore) {
    Write-Host "Creating your local production signing key. Enter a strong keystore password when keytool prompts."
    Write-Host "At the key password prompt, press Enter to reuse the keystore password."
    & $keytoolPath -genkeypair `
        -keystore $keystorePath `
        -alias $keyAlias `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -dname "CN=StandBy Android"
    if ($LASTEXITCODE -ne 0) {
        throw "keytool could not create the signing key."
    }

    Write-Host "Keystore created at: $keystorePath"
    $backupConfirmation = Read-Host "Make a secure offline backup of this keystore now, then type BACKED-UP"
    if ($backupConfirmation -cne "BACKED-UP") {
        throw "Release build stopped. Keep the keystore safe, make an offline backup, and rerun this script."
    }
}

$storePasswordSecure = Read-Host "Enter the keystore password" -AsSecureString
$keyPasswordSecure = Read-Host "Enter the key password (press Enter if it matches the keystore password)" -AsSecureString
$storePasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePasswordSecure)
$keyPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPasswordSecure)

try {
    $env:STANDBY_RELEASE_STORE_FILE = $keystorePath
    $env:STANDBY_RELEASE_STORE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($storePasswordPointer)
    $env:STANDBY_RELEASE_KEY_ALIAS = $keyAlias
    $env:STANDBY_RELEASE_KEY_PASSWORD = if ($keyPasswordSecure.Length -eq 0) {
        $env:STANDBY_RELEASE_STORE_PASSWORD
    } else {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPasswordPointer)
    }

    Push-Location $projectRoot
    try {
        & .\gradlew.bat testDebugUnitTest lintRelease assembleRelease
        if ($LASTEXITCODE -ne 0) {
            throw "Release checks or APK build failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($storePasswordPointer)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPasswordPointer)
    Remove-Item Env:STANDBY_RELEASE_STORE_FILE -ErrorAction SilentlyContinue
    Remove-Item Env:STANDBY_RELEASE_STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:STANDBY_RELEASE_KEY_ALIAS -ErrorAction SilentlyContinue
    Remove-Item Env:STANDBY_RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
}

Write-Host "Signed production APK: app\build\outputs\apk\release\app-release.apk"
Write-Host "Back up standby-release.keystore and its passwords securely before publishing."
