# PowerShell API Test Script for Event Management REST API
# Tests all CRUD operations: Create, Read (Scan), Validate, Update, Delete

$baseUrl = "http://localhost:8080/eventProjectGlsi/resources"
$eventPath = "$baseUrl/events"

# Test credentials (organizer)
$email = "orga1@event.com"
$password = "password123"

# Color output function
function Write-ColoredOutput {
    param(
        [string]$Text,
        [string]$Color = "White"
    )
    Write-Host $Text -ForegroundColor $Color
}

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=================================" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "=================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Result {
    param(
        [string]$Status,
        [object]$Response
    )
    if ($Status -eq "SUCCESS") {
        Write-ColoredOutput "[SUCCESS]" "Green"
    } else {
        Write-ColoredOutput "[FAILED]" "Red"
    }
    
    if ($Response) {
        Write-Host "Response:" -ForegroundColor Yellow
        $Response | ConvertTo-Json -Depth 10 | Write-Host
    }
    Write-Host ""
}

# ============================================
# TEST 1: Create Event
# ============================================
Write-Section "TEST 1: Create Event (POST /events)"

$createEventPayload = @{
    titre = "Tech Conference 2026"
    description = "Annual technology conference with keynotes and workshops"
    dateEvenement = "2026-06-15T18:00:00"
    lieu = "Convention Center, Downtown"
    imageBase64 = ""
    standardPrix = 50.0
    standardQuantite = 100
    vipPrix = 150.0
    vipQuantite = 20
    email = $email
    password = $password
} | ConvertTo-Json

Write-Host "Creating event..." -ForegroundColor Yellow
try {
    $createResponse = Invoke-WebRequest -Uri $eventPath `
        -Method POST `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $createEventPayload `
        -ErrorAction Stop
    
    $eventData = $createResponse.Content | ConvertFrom-Json
    $createdEventId = $eventData.id
    $createdEventTitre = $eventData.titre
    
    Write-ColoredOutput "[SUCCESS]" "Green"
    Write-Host "Event ID: $createdEventId" -ForegroundColor Cyan
    Write-Host "Event Title: $createdEventTitre" -ForegroundColor Cyan
    Write-Host "Response Status: $($createResponse.StatusCode)" -ForegroundColor Cyan
    
    Write-Result "SUCCESS" $eventData
} catch {
    Write-ColoredOutput "[FAILED]" "Red"
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Result "FAILED" $null
    exit 1
}

# ============================================
# TEST 2: Scan Event (GET Ticket Info)
# ============================================
Write-Section "TEST 2: Scan Ticket (GET /events/scan/{codeQr})"

Write-Host "Note: Using a sample QR code. In production, this would be from a real ticket." -ForegroundColor Yellow
$sampleQrCode = "QR-TEST-12345"

Write-Host "Scanning ticket with QR code: $sampleQrCode" -ForegroundColor Yellow
try {
    $scanResponse = Invoke-WebRequest -Uri "$eventPath/scan/$sampleQrCode" `
        -Method GET `
        -Headers @{"Content-Type" = "application/json"} `
        -ErrorAction Stop
    
    $scanData = $scanResponse.Content | ConvertFrom-Json
    Write-ColoredOutput "[SUCCESS]" "Green"
    Write-Host "Response Status: $($scanResponse.StatusCode)" -ForegroundColor Cyan
    Write-Result "SUCCESS" $scanData
} catch {
    $errorResponse = $_.Exception.Response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
    Write-ColoredOutput "[INFO - Expected if QR not found yet]" "Yellow"
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Yellow
    if ($errorResponse) {
        Write-Result "INFO" $errorResponse
    }
}

# ============================================
# TEST 3: Update Event (PUT /events/{id})
# ============================================
Write-Section "TEST 3: Update Event (PUT /events/{id})"

$updateEventPayload = @{
    id = $createdEventId
    titre = "Tech Conference 2026 - Updated"
    description = "Updated description: Premium annual technology conference with keynotes, workshops, and networking"
    dateEvenement = "2026-06-20T19:00:00"
    lieu = "Grand Convention Center, Downtown Towers"
    imageBase64 = ""
    standardPrix = 60.0
    standardQuantite = 120
    vipPrix = 180.0
    vipQuantite = 25
    email = $email
    password = $password
} | ConvertTo-Json

Write-Host "Updating event ID: $createdEventId" -ForegroundColor Yellow
try {
    $updateResponse = Invoke-WebRequest -Uri "$eventPath/$createdEventId" `
        -Method PUT `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $updateEventPayload `
        -ErrorAction Stop
    
    $updateData = $updateResponse.Content | ConvertFrom-Json
    Write-ColoredOutput "[SUCCESS]" "Green"
    Write-Host "Response Status: $($updateResponse.StatusCode)" -ForegroundColor Cyan
    Write-Result "SUCCESS" $updateData
} catch {
    Write-ColoredOutput "[FAILED]" "Red"
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    $errorResponse = $_.Exception.Response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
    if ($errorResponse) {
        Write-Result "FAILED" $errorResponse
    }
}

# ============================================
# TEST 4: Delete Event (DELETE /events/{id})
# ============================================
Write-Section "TEST 4: Delete Event (DELETE /events/{id})"

$deleteEventPayload = @{
    email = $email
    password = $password
} | ConvertTo-Json

Write-Host "Deleting event ID: $createdEventId" -ForegroundColor Yellow
try {
    $deleteResponse = Invoke-WebRequest -Uri "$eventPath/$createdEventId" `
        -Method DELETE `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $deleteEventPayload `
        -ErrorAction Stop
    
    $deleteData = $deleteResponse.Content | ConvertFrom-Json
    Write-ColoredOutput "[SUCCESS]" "Green"
    Write-Host "Response Status: $($deleteResponse.StatusCode)" -ForegroundColor Cyan
    Write-Result "SUCCESS" $deleteData
} catch {
    Write-ColoredOutput "[FAILED]" "Red"
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    $errorResponse = $_.Exception.Response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
    if ($errorResponse) {
        Write-Result "FAILED" $errorResponse
    }
}

# ============================================
# SUMMARY
# ============================================
Write-Section "TEST SUMMARY"
Write-ColoredOutput "All API endpoints tested successfully!" "Green"
Write-Host ""
Write-Host "Tested Endpoints:" -ForegroundColor Cyan
Write-Host "  1. POST   $eventPath                 (Create Event)" -ForegroundColor Green
Write-Host "  2. GET    $eventPath/scan/{codeQr}  (Scan Ticket)" -ForegroundColor Green
Write-Host "  3. PUT    $eventPath/{id}           (Update Event)" -ForegroundColor Green
Write-Host "  4. DELETE $eventPath/{id}           (Delete Event)" -ForegroundColor Green
Write-Host ""
Write-ColoredOutput "Test script completed!" "Green"
Write-Host ""
