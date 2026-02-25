# Test script for chat API
$body = @{
    message = "What is my total investment?"
    model = "gpt-4o-mini"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/chat' -Method POST -Body $body -ContentType 'application/json' -UseBasicParsing
    Write-Host "Success! Response:"
    Write-Host $response.Content
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
}
