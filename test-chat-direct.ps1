# Test 1: Direct call to chat-backend with small message
Write-Host "=== Test 1: Direct call to chat-backend (gemma-3-27b-it) ==="
$body1 = @{
    messages = @(
        @{ role = "user"; content = "Hello, what can you do?" }
    )
    model = "gemma-3-27b-it"
    temperature = 0.7
    max_tokens = 200
} | ConvertTo-Json -Depth 5

try {
    $response1 = Invoke-WebRequest -Uri 'http://localhost:5000/api/chat' -Method POST -Body $body1 -ContentType 'application/json' -UseBasicParsing
    Write-Host "SUCCESS: $($response1.Content)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}

Write-Host ""
Write-Host "=== Test 2: Direct call to chat-backend (gpt-4o-mini) ==="
$body2 = @{
    messages = @(
        @{ role = "user"; content = "Hello" }
    )
    model = "gpt-4o-mini"
    temperature = 0.7
    max_tokens = 200
} | ConvertTo-Json -Depth 5

try {
    $response2 = Invoke-WebRequest -Uri 'http://localhost:5000/api/chat' -Method POST -Body $body2 -ContentType 'application/json' -UseBasicParsing
    Write-Host "SUCCESS: $($response2.Content)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}

Write-Host ""
Write-Host "=== Test 3: Direct call to chat-backend (pixtral-12b-2409) ==="
$body3 = @{
    messages = @(
        @{ role = "user"; content = "Hello" }
    )
    model = "pixtral-12b-2409"
    temperature = 0.7
    max_tokens = 200
} | ConvertTo-Json -Depth 5

try {
    $response3 = Invoke-WebRequest -Uri 'http://localhost:5000/api/chat' -Method POST -Body $body3 -ContentType 'application/json' -UseBasicParsing
    Write-Host "SUCCESS: $($response3.Content)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}

Write-Host ""
Write-Host "=== Test 4: Spring Boot backend /api/chat/context ==="
try {
    $response4 = Invoke-WebRequest -Uri 'http://localhost:8080/api/chat/context' -UseBasicParsing
    $contextData = $response4.Content | ConvertFrom-Json
    Write-Host "Context retrieved successfully"
    Write-Host "Available Models: $($contextData.availableModels -join ', ')"
    $summary = $contextData.portfolioSummary | ConvertTo-Json
    Write-Host "Portfolio Summary: $summary"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
}
