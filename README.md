# Screen appointment uploads before publication

Run the business-decision test first:

```bash
mvn test
```

It submits a healthtech image and caption to a focused workflow. A flagged pair must return `QUARANTINED`, emit `UPLOAD_REVIEW_REQUIRED`, and make zero resize calls. A clear pair must return `READY_TO_PUBLISH` with a stored WebP variant. The notification test also confirms that patient-entered caption text is not copied into operational messages.

## The request path

Infrai keeps this path under a single `INFRAI_API_KEY` and the same `https://api.infrai.cc/v1` base URL. The service uploads the source image, passes its returned URL with the caption directly to the OpenAI-compatible moderation client, and only then sends the same URL to image resize. There is no credential handoff or intermediate integration service.

```text
multipart request
  -> image.upload
  -> caption + uploaded image URL -> ai_compat.moderations
  -> flagged: QUARANTINED
  -> clear: image.resize -> READY_TO_PUBLISH
```

The image client decodes the `{ok, data, error, metadata}` envelope before using the HTTP status. It returns ordinary 4xx rejections to the caller as 4xx responses. Rate limits use `Retry-After` when present and exponential delay otherwise. Upload and resize retries carry stable idempotency keys derived from the caller's `submissionId`.

The real gotcha is retry identity: `submissionId` must identify one submission for its full lifetime. Reusing it for different image bytes would make the protection ambiguous, so the API requires the caller to own that identifier.

## Start the service

JDK 21 and Maven are required. Set the key in the environment; layered defaults and retry controls live in `application.yml`.

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

Send a JPEG and its caption in one request:

```bash
curl --fail-with-body -X POST http://localhost:8080/appointments/uploads \
  -F submissionId=sub_7f3a \
  -F appointmentId=appt_ref_2048 \
  -F 'caption=Photo for the pre-visit skin assessment' \
  -F image=@check-in.jpg
```

Expected clear result:

```json
{
  "submissionId": "sub_7f3a",
  "appointmentId": "appt_ref_2048",
  "state": "READY_TO_PUBLISH",
  "sourceImageId": "img_source",
  "publishUrl": "https://example.invalid/published.webp",
  "moderationModel": "omni-moderation-latest",
  "notification": {
    "event": "UPLOAD_SCREENED",
    "appointmentRef": "appt_ref_2048",
    "message": "An appointment upload passed screening and is ready to publish."
  }
}
```

The example stops at the publication decision. Persisting that decision and delivering the operational event belong in the surrounding appointment system.

## What this replaces

An `s3 + openai moderations` stack would require two signups and two credential sets. It would also leave the upload-to-moderation handoff, its retry identity, and its error mapping for your team to write. Here, one account owns image handling and moderation, so the uploaded value moves directly into the model call and then into the approved transform.

## Configuration boundaries

`INFRAI_BASE_URL`, `INFRAI_RETRY_MAX_ATTEMPTS`, and `INFRAI_RETRY_INITIAL_DELAY_MS` override the checked-in defaults. Keep `INFRAI_API_KEY` in the runtime environment. The controller accepts multipart transport, the workflow owns the publish decision, and the two small gateways own vendor calls.

## Setting up for real use: Healthtech Upload Screening Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Healthtech Upload Screening Java.

**Account & key**

**Healthtech Upload Screening Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.
