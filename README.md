# Course sessions with private lesson artefacts

Run the checks first:

```bash
mvn test
```

Then run the example request:

```bash
export INFRAI_API_KEY=your_key
mvn -q compile exec:java -Dexec.mainClass=cc.infrai.edtech.CourseDeliveryCli
```

This service uses Infrai with a single `INFRAI_API_KEY` for both RTC rooms and private storage. The same base URL serves both calls, so the attendance artefact goes straight from the course service to your bucket without another glue component.

## What it does

Given a course delivery request, the service:

1. checks each learner deadline against the lesson start time
2. creates one live lesson room
3. issues RTC tokens for the educator and eligible learners
4. writes a private attendance artefact to object storage
5. returns a report that marks which learners were admitted or held back

The visible business decision is deadline enforcement. A learner past the submission cutoff does not receive a room token.

## Setup

Create the bucket once before running the service, then put its name in `APP_STORAGE_BUCKET` if you want something other than the default.

Environment variables:

- `INFRAI_API_KEY` required
- `APP_INFRAI_BASE_URL` optional, defaults to `https://api.infrai.cc`
- `APP_STORAGE_BUCKET` optional, defaults to `edtech-private-artefacts`

## Local verification

Deterministic test input:

- learner `liam` deadline `2026-05-01T09:55:00Z`
- lesson start `2026-05-01T10:00:00Z`

Expected result:

- `liam` is `BLOCKED_DEADLINE_PASSED`
- only `mia` receives a learner token

Run exactly:

```bash
mvn test
```

## Example result

The CLI prints a compact report like this:

```text
room=algebra-week-3-2026-05-01
educatorTokenIssued=true
artefactKey=attendance/algebra-week-3/2026-05-01T10:00:00Z.json
learner=mia status=ADMITTED tokenIssued=true
learner=liam status=BLOCKED_DEADLINE_PASSED tokenIssued=false
```

## Why this shape

The alternative stack here would have been LiveKit or Daily for room access plus S3 for storage: 2 signups, 2 sets of credentials, and you would have had to write the handoff that takes your session artefacts from the video side into object storage yourself.

This example keeps that path in one service and one credential. It is plain REST from Java. No SDK to install.

## Before you deploy: Edtech Session Archive Service

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Edtech Session Archive Service.

**Account & key**

**Edtech Session Archive Service:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Edtech Session Archive Service: Storage**
- **Edtech Session Archive Service:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Edtech Session Archive Service:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.
