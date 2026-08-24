# CareFlow REST API Contract

Base URL: `http://localhost:8080/api/v1`

All timestamps are ISO-8601 offsets. JSON request bodies use `Content-Type: application/json`. Validation and business-rule failures return Spring Problem Details with `title`, `status`, `detail`, `type`, and optional `errors`.

## Work orders

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/work-orders` | Raise a work order |
| `GET` | `/work-orders` | Search, filter, page, and sort work orders |
| `GET` | `/work-orders/{id}` | Read a single work order |
| `PATCH` | `/work-orders/{id}/assignment` | Assign or reassign an active technician |
| `PATCH` | `/work-orders/{id}/status` | Apply an allowed status transition |
| `GET` | `/work-orders/{id}/history` | Read the chronological status audit trail |

### List query parameters

| Parameter | Example | Notes |
|---|---|---|
| `query` | `generator` | Searches reference, title, and description |
| `status` | `IN_PROGRESS` | Exact enum match |
| `priority` | `CRITICAL` | Exact enum match |
| `siteId` | UUID | Site filter |
| `technicianId` | UUID | Assigned-technician filter |
| `page` | `0` | Zero-based |
| `size` | `20` | Capped at 100 |
| `sortBy` | `targetResolutionAt` | Allow-listed field |
| `direction` | `ASC` | `ASC` or `DESC` |

### Create request

```json
{
  "title": "Chiller pressure alarm during peak load",
  "description": "Pressure telemetry crossed the high threshold three times after 14:00.",
  "priority": "HIGH",
  "siteId": "10000000-0000-0000-0000-000000000001",
  "assetId": "20000000-0000-0000-0000-000000000001",
  "targetResolutionAt": "2026-08-25T18:00:00+05:30"
}
```

### Assignment request

```json
{
  "technicianId": "30000000-0000-0000-0000-000000000001",
  "changedBy": "Operations Planner"
}
```

### Status request

```json
{
  "status": "IN_PROGRESS",
  "note": "On site; baseline readings captured.",
  "changedBy": "Ananya Rao"
}
```

### Status state machine

| Current | Allowed next status |
|---|---|
| `NEW` | `ASSIGNED`, `CANCELLED` |
| `ASSIGNED` | `IN_PROGRESS`, `ON_HOLD`, `CANCELLED` |
| `IN_PROGRESS` | `ON_HOLD`, `RESOLVED` |
| `ON_HOLD` | `IN_PROGRESS`, `CANCELLED` |
| `RESOLVED` | None in starter |
| `CANCELLED` | None in starter |

Assignment automatically moves a `NEW` work order to `ASSIGNED`. Starting work requires an assigned technician.

## Dashboard

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/dashboard/summary` | Open, overdue, unassigned, critical, and per-status counts |

## Reference data

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/reference/sites` | Active service-site options |
| `GET` | `/reference/assets?siteId={id}` | Non-retired assets for one site |
| `GET` | `/reference/technicians` | Active technician options |

## Operational endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/actuator/health` | Application and database health |
| `GET` | `/actuator/info` | Application information |
| `GET` | `/actuator/metrics` | Local operational metrics index |

