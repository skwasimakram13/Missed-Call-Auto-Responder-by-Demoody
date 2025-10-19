# API Documentation

Base URL: `https://yourdomain.com/api/v1`

## Authentication

Most endpoints require device authentication. Include the API token in the Authorization header:

```
Authorization: Bearer YOUR_API_TOKEN
```

## Endpoints

### Health Check

**GET** `/health`

Check API health status.

**Response:**
```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "version": "1.0.0",
    "timestamp": "2025-01-01T12:00:00Z"
  }
}
```

### Device Registration

**POST** `/register_device`

Register a new device and get API token.

**Request:**
```json
{
  "device_id": "unique_device_identifier",
  "device_name": "Samsung Galaxy S21"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "Device registered successfully",
    "api_token": "your_api_token_here"
  }
}
```

### Log Missed Call

**POST** `/missed_calls`

Log a missed call and schedule automatic response.

**Headers:**
```
Authorization: Bearer YOUR_API_TOKEN
Content-Type: application/json
```

**Request:**
```json
{
  "device_id": "unique_device_identifier",
  "phone_number": "+1234567890",
  "call_time": 1640995200000,
  "message_text": "Hello! We missed your call...",
  "delay_minutes": 5
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "Missed call logged successfully",
    "call_id": 123,
    "scheduled_time": "2025-01-01T12:05:00Z",
    "delay_minutes": 5
  }
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "Invalid phone number format",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### Process Scheduled Messages

**POST** `/send_scheduled`

Process pending scheduled messages (typically called by cron).

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "Processed scheduled messages",
    "processed_count": 5,
    "results": [
      {
        "call_id": 123,
        "phone_number": "+1234567890",
        "result": {
          "status": "SENT",
          "message_id": "msg_123456"
        }
      }
    ]
  }
}
```

### Get Logs

**GET** `/logs?device_id=DEVICE_ID&page=1&limit=20`

Retrieve call logs for a device.

**Parameters:**
- `device_id` (required): Device identifier
- `page` (optional): Page number (default: 1)
- `limit` (optional): Items per page (default: 20, max: 100)

**Response:**
```json
{
  "success": true,
  "data": {
    "logs": [
      {
        "id": 123,
        "phone_number": "+1234567890",
        "call_time": "2025-01-01T12:00:00Z",
        "status": "SENT",
        "message_text": "Hello! We missed your call...",
        "sent_at": "2025-01-01T12:05:00Z"
      }
    ],
    "stats": {
      "total_calls": 100,
      "sent_count": 85,
      "failed_count": 5,
      "skipped_count": 10,
      "pending_count": 0
    },
    "pagination": {
      "page": 1,
      "limit": 20
    }
  }
}
```

### Opt Out

**POST** `/opt_out`

Add a phone number to the opt-out list.

**Request:**
```json
{
  "phone_number": "+1234567890"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "Number added to opt-out list"
  }
}
```

## Error Codes

| Code | Description |
|------|-------------|
| 400 | Bad Request - Invalid input data |
| 401 | Unauthorized - Invalid or missing API token |
| 404 | Not Found - Endpoint or resource not found |
| 429 | Too Many Requests - Rate limit exceeded |
| 500 | Internal Server Error - Server error |

## Rate Limiting

- **Device limit**: 100 requests per hour per device
- **Phone limit**: 5 requests per hour per phone number

Rate limit headers are included in responses:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640998800
```

## Status Values

### Call Status
- `PENDING`: Message scheduled but not sent
- `SENT`: Message sent successfully
- `FAILED`: Message sending failed after retries
- `SKIPPED`: Message skipped (business hours, opt-out, etc.)
- `BLOCKED`: Phone number is blocked

## Webhooks (Future)

Webhook endpoints for real-time notifications:

- `POST /webhooks/delivery_status` - Message delivery status updates
- `POST /webhooks/opt_out` - Opt-out notifications

## SDK Examples

### cURL
```bash
# Register device
curl -X POST https://yourdomain.com/api/v1/register_device \
  -H "Content-Type: application/json" \
  -d '{"device_id":"test123","device_name":"Test Device"}'

# Log missed call
curl -X POST https://yourdomain.com/api/v1/missed_calls \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "device_id":"test123",
    "phone_number":"+1234567890",
    "call_time":1640995200000,
    "message_text":"Hello! We missed your call.",
    "delay_minutes":5
  }'
```

### JavaScript
```javascript
// Register device
const response = await fetch('https://yourdomain.com/api/v1/register_device', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    device_id: 'unique_device_id',
    device_name: 'My Device'
  })
});

const data = await response.json();
console.log(data);
```

### PHP
```php
// Log missed call
$data = [
    'device_id' => 'unique_device_id',
    'phone_number' => '+1234567890',
    'call_time' => time() * 1000,
    'message_text' => 'Hello! We missed your call.',
    'delay_minutes' => 5
];

$ch = curl_init('https://yourdomain.com/api/v1/missed_calls');
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Authorization: Bearer YOUR_TOKEN',
    'Content-Type: application/json'
]);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
curl_close($ch);

$result = json_decode($response, true);
```

## Testing

Use the provided Postman collection or test with curl commands above.

For development, you can use the health endpoint to verify your setup:

```bash
curl https://yourdomain.com/api/v1/health
```