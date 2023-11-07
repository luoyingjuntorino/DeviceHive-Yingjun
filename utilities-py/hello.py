import requests
import json


url = "http://host.docker.internal:80/auth/rest/token/create"

payload = json.dumps({
  "userId": 1,
  "expiration": "2033-10-30T23:00:00.000Z",
  "actions": [
    0
  ],
  "networkIds": [
    "*"
  ],
  "iexperimentIds": [
    "*"
  ],
  "icomponentIds": [
    "*"
  ]
})
headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7ImEiOlswXSwiZSI6MjAxNDMyNjAwMDAwMCwidCI6MSwidSI6MSwibiI6WyIqIl0sImR0IjpbIioiXSwiY3AiOlsiKiJdfX0.v0F4bnnIwVRn6qCuukhAp0uXUgrQgGJ54HlghBTAWlg'
}

response = requests.request("POST", url, headers=headers, data=payload)

print(response.text)
