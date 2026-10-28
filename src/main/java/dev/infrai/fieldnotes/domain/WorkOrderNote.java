package dev.infrai.fieldnotes.domain;

import java.util.List;
import java.util.Locale;

public record WorkOrderNote(
        String noteId,
        String workOrderId,
        String transcript,
        List<String> photoIds,
        DispatchStatus dispatchStatus,
        boolean technicianFollowUp
) {
    public WorkOrderNote {
        photoIds = photoIds == null ? List.of() : List.copyOf(photoIds);
    }

    public FollowUpState followUpState() {
        String words = transcript.toLowerCase(Locale.ROOT);
        boolean promisedReturn = words.contains("return visit") || words.contains("come back");
        return technicianFollowUp || (dispatchStatus == DispatchStatus.COMPLETED && promisedReturn)
                ? FollowUpState.REQUIRED
                : FollowUpState.NONE;
    }

    public String searchableText() {
        return "Work order " + workOrderId + ". Dispatch " + dispatchStatus
                + ". Follow-up " + followUpState() + ". Transcript: " + transcript;
    }

    public enum DispatchStatus { ASSIGNED, EN_ROUTE, ON_SITE, COMPLETED }
    public enum FollowUpState { NONE, REQUIRED }
}
