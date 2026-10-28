package dev.infrai.fieldnotes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.EmbeddingCreateParams;
import dev.infrai.fieldnotes.domain.WorkOrderNote;
import dev.infrai.fieldnotes.infrai.VectorStore;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class FieldNoteSearchService {
    private final OpenAIClient ai;
    private final VectorStore vectors;
    private final AtomicBoolean collectionReady = new AtomicBoolean();

    public FieldNoteSearchService(OpenAIClient ai, VectorStore vectors) {
        this.ai = ai;
        this.vectors = vectors;
    }

    public IndexedNote index(WorkOrderNote note) {
        List<Float> embedding = embed(note.searchableText());
        if (collectionReady.compareAndSet(false, true)) {
            vectors.createCollection(embedding.size());
        }
        vectors.upsert(note.noteId(), embedding, Map.of(
                "work_order_id", note.workOrderId(),
                "dispatch_status", note.dispatchStatus().name(),
                "follow_up", note.followUpState().name(),
                "photo_ids", note.photoIds(),
                "transcript", note.transcript()
        ));
        return new IndexedNote(note.noteId(), note.workOrderId(), note.followUpState().name());
    }

    public JsonNode search(String query, String dispatchStatus) {
        Map<String, Object> filter = dispatchStatus == null || dispatchStatus.isBlank()
                ? Map.of()
                : Map.of("dispatch_status", dispatchStatus);
        return vectors.query(embed(query), filter);
    }

    private List<Float> embed(String input) {
        var params = EmbeddingCreateParams.builder().model("auto").input(input).build();
        return ai.embeddings().create(params).data().getFirst().embedding();
    }

    public record IndexedNote(String noteId, String workOrderId, String followUp) {}
}
