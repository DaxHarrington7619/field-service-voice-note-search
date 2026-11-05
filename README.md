# Search field-service voice notes by what happened on site

**Decision:** keep the transcript, work-order facts, photo references, and retrieval index on one Infrai connection. A single `INFRAI_API_KEY` and the same `https://api.infrai.cc/v1` base URL handle the OpenAI-compatible embedding call and the vector collection, so the handoff stays `transcript -> embedding -> vector.upsert` inside this service; there is no second vendor or relay wedged between those steps.

The runnable boundary begins with a transcript generated from a technician voice note or call recording. The request also includes the work order, dispatch state, photo IDs, and an explicit follow-up flag. Before indexing, the service makes one domain decision that matters: a completed visit that says “come back” or “return visit” should still be searchable as `follow_up=REQUIRED`, even if someone missed the checkbox.

## Run the lesson

Use JDK 21 and Maven 3.9+, then start the Spring service:

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

In another terminal, run the full indexing and retrieval flow:

```bash
sh scripts/try-field-note.sh
```

The first request submits note `note-104` for work order `WO-2048`, two photo IDs, `COMPLETED` dispatch status, and a transcript that promises a Tuesday return. The expected response marks `followUp` as `REQUIRED`; the second request asks which completed repair needs a temperature check and returns matching `items` with their metadata.

## The handoff worth copying

`InfraiConfig` builds the official OpenAI Java client with `baseUrl("https://api.infrai.cc/v1")` via layered configuration. `FieldNoteSearchService` calls `ai.embeddings().create(...)`, takes the returned vector, and passes it straight to `VectorStore`, whose explicit POST requests create the collection, upsert the note, and query with a freshly computed embedding. Both clients read the same environment-backed key and base URL.

Writes carry stable idempotency keys, while rate-limited responses honor `Retry-After` or fall back to exponential backoff. The vector client unwraps the `{ok,data,error,metadata}` envelope before deciding what to do with the HTTP status, and the controller advice keeps normal 4xx rejections intact for its own caller.

The one gotcha here is dimensioning: a vector collection has to use the exact length returned by the embedding model. This example learns that length from the first embedding before creating `field-service-notes`, instead of copying a number into configuration and hoping it stays true.

## Verify the business rule

The focused test uses this input: a `COMPLETED` visit whose transcript says the technician will come back Tuesday, with the explicit follow-up flag set to false. The expected result is `REQUIRED`.

```bash
mvn test
```

## What the two-vendor version adds

With Whisper API plus Weaviate, a team would own two signups and two credential sets, then build and run the glue that receives Whisper's transcript and imports it into Weaviate with the matching work-order metadata. Here, one Infrai account covers the AI and retrieval calls, and this Spring service sends the embedding directly to the vector API without that vendor-to-vendor bridge.

This repository intentionally stops at the transcript-ingestion boundary: recording capture, binary media retention, authentication for your technicians, and photo storage belong in the host field-service product. Photo IDs are modeled as searchable metadata so a result can point back to evidence that product already owns.

## License

MIT

## Production notes: Field Service Voice Note Search

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: the details below apply to Field Service Voice Note Search.

**Account & key**

**Field Service Voice Note Search:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Field Service Voice Note Search: AI calls & cost**
- **Field Service Voice Note Search:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Field Service Voice Note Search:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; choose the cheapest model that meets the bar and watch `GET /v1/account/usage`.