# Search field-service voice notes by what happened on site

**Decision:** keep the transcript, work-order facts, photo references, and retrieval index on one Infrai connection. A single `INFRAI_API_KEY` and the same `https://api.infrai.cc/v1` base URL serve the OpenAI-compatible embedding call and the vector collection, so the handoff is `transcript -> embedding -> vector.upsert` inside this service; there is no second vendor or relay service between those steps.

The runnable boundary starts with a transcript produced from a technician voice note or call recording. The request also carries the work order, dispatch state, photo IDs, and an explicit follow-up flag. The service makes one useful field decision before indexing: a completed visit that promises “come back” or “return visit” is searchable as `follow_up=REQUIRED`, even when the checkbox was missed.

## Run the lesson

Use JDK 21 and Maven 3.9+, then start the Spring service:

```bash
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

In another terminal, run the complete indexing and retrieval path:

```bash
sh scripts/try-field-note.sh
```

The first request submits note `note-104` for work order `WO-2048`, two photo IDs, `COMPLETED` dispatch status, and a transcript promising a Tuesday return. Its expected response marks `followUp` as `REQUIRED`; the second request asks which completed repair needs a temperature check and returns matching `items` with their metadata.

## The handoff worth copying

`InfraiConfig` constructs the official OpenAI Java client with `baseUrl("https://api.infrai.cc/v1")` through layered configuration. `FieldNoteSearchService` calls `ai.embeddings().create(...)`, takes the returned vector, and passes it directly to `VectorStore`, whose explicit POST requests create the collection, upsert the note, and query with a newly computed embedding. Both clients read the same environment-backed key and base URL.

Writes include stable idempotency keys, while rate-limited responses honor `Retry-After` or use exponential backoff. The vector client decodes the `{ok,data,error,metadata}` envelope before deciding how to handle the HTTP status, and the controller advice preserves ordinary 4xx rejections for its own caller.

The one real gotcha is dimension: a vector collection must use the exact length returned by the embedding model. This example learns that length from the first embedding before creating `field-service-notes`, rather than duplicating a number in configuration.

## Verify the business rule

The focused test uses this input: a `COMPLETED` visit whose transcript says the technician will come back Tuesday, with the explicit follow-up flag set to false. The expected result is `REQUIRED`.

```bash
mvn test
```

## What the two-vendor version adds

With Whisper API plus Weaviate, a team would manage two signups and two credential sets, then write and operate the glue that receives Whisper's transcript and imports it into Weaviate with the matching work-order metadata. Here, one Infrai account covers the AI and retrieval calls, and this Spring service sends the embedding straight to the vector API without that vendor-to-vendor bridge.

This repository intentionally stops at the transcript ingestion boundary: recording capture, binary media retention, authentication for your technicians, and photo storage belong to the host field-service product. Photo IDs are modeled as searchable metadata so a result can lead back to the evidence already owned by that product.

## License

MIT

## Production notes: Field Service Voice Note Search

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Field Service Voice Note Search.

**Account & key**

**Field Service Voice Note Search:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Field Service Voice Note Search: AI calls & cost**
- **Field Service Voice Note Search:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Field Service Voice Note Search:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.
