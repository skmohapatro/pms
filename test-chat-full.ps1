# Test the full Spring Boot chat endpoint (which includes full context)
Write-Host "=== Test: Full chat through Spring Boot backend ==="

$body = @{
    message = "What is my total investment?"
    model = "gemma-3-27b-it"
} | ConvertTo-Json -Depth 5

try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/chat' -Method POST -Body $body -ContentType 'application/json' -UseBasicParsing -TimeoutSec 120
    Write-Host "SUCCESS: $($response.Content)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody"
    }
}
