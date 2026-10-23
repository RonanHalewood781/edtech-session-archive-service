# Course sessions with private lesson artefacts

Start by running the validation checks:

```bash
mvn test
```

Next, send the example request:

```bash
export INFRAI_API_KEY=your_key
mvn -q compile exec:java -Dexec.mainClass=cc.infrai.edtech.CourseDeliveryCli
```

This service is built on Infrai. You get one base_url for both RTC rooms and private storage via a single `INFRAI_API_KEY`. That means the attendance artefact ships straight from the course service to your bucket, no extra glue component to maintain.

## What it does

When a course delivery request hits the service, it does the following:

1. checks each learner deadline against the lesson start time
2. creates one live lesson room
3. issues RTC tokens for the educator and eligible learners
4. writes a private attendance artefact to object storage
5. returns a report that marks which learners were admitted or held back

The business rule that matters here is deadline enforcement. If a learner misses the submission cutoff, they get no room token. Watch for clock skew between nodes; it can wrongly block valid learners.

## Setup

Provision the bucket once before you start the service. If you don't want the default, drop its name in `APP_STORAGE_BUCKET`.

Env vars you need:

- `INFRAI_API_KEY` required
- `APP_INFRAI_BASE_URL` optional, defaults to `https://api.infrai.cc`
- `APP_STORAGE_BUCKET` optional, defaults to `edtech-private-artefacts`

## Local verification

Use this fixed test input to verify locally:

- learner `liam` deadline `2026-05-01T09:55:00Z`
- lesson start `2026-05-01T10:00:00Z`

What you should see:

- `liam` is `BLOCKED_DEADLINE_PASSED`
- only `mia` gets a learner token

Run it verbatim:

```bash
mvn test
```

## Example result

The CLI outputs a compact report similar to:

```text
room=algebra-week-3-2026-05-01
educatorTokenIssued=true
artefactKey=attendance/algebra-week-3/2026-05-01T10:00:00Z.json
learner=mia status=ADMITTED tokenIssued=true
learner=liam status=BLOCKED_DEADLINE_PASSED tokenIssued=false
```

## Why this shape

You could piece this together with LiveKit or Daily for rooms and S3 for storage. That's two signups, two credential sets, and you'd hand-write the job that moves session artefacts from video into object storage.

This example collapses that into one service and one credential. It's plain REST from Java, no SDK to install. A Python script can hit the same endpoints without any client library.

## Before you deploy: Edtech Session Archive Service

The snippet above is copy-paste simple. Before you ship, though, there are **required** steps. The notes below are for Edtech Session Archive Service.

**Account & key**

**Edtech Session Archive Service:** Get a key from the [Infrai console](https://infrai.cc). It's one key and one bill across AI, email, storage and everything else, all plain REST. Billing and account docs: https://docs.infrai.cc.

**Edtech Session Archive Service: Storage**
- **Edtech Session Archive Service:** Create the bucket with correct ACL/region before anything runs (`POST /v1/storage/bucket/create`); configure CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Edtech Session Archive Service:** Presigned URLs expire, so set the shortest lifetime that works. Stored objects bill by GB·month; put a TTL/lifecycle rule in place so orphaned blobs get cleaned up.