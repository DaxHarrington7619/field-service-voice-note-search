package dev.infrai.fieldnotes.web;

import com.fasterxml.jackson.databind.JsonNode;
import dev.infrai.fieldnotes.domain.WorkOrderNote;
import dev.infrai.fieldnotes.service.FieldNoteSearchService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/field-notes")
public class FieldNoteController {
    private final FieldNoteSearchService notes;

    public FieldNoteController(FieldNoteSearchService notes) {
        this.notes = notes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FieldNoteSearchService.IndexedNote index(@RequestBody IndexRequest request) {
        return notes.index(new WorkOrderNote(request.noteId(), request.workOrderId(), request.transcript(),
                request.photoIds(), request.dispatchStatus(), request.technicianFollowUp()));
    }

    @GetMapping("/search")
    JsonNode search(@RequestParam String q, @RequestParam(required = false) String dispatchStatus) {
        return notes.search(q, dispatchStatus);
    }

    public record IndexRequest(String noteId, String workOrderId, String transcript, List<String> photoIds,
                               WorkOrderNote.DispatchStatus dispatchStatus, boolean technicianFollowUp) {}
}
