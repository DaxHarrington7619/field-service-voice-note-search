package dev.infrai.fieldnotes.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkOrderNoteTest {
    @Test
    void completedJobWithPromisedReturnIsFlaggedForFollowUp() {
        var note = new WorkOrderNote("note-104", "WO-2048",
                "Replaced the relay. I will come back Tuesday to verify the temperature.",
                List.of("photo-before", "photo-after"), WorkOrderNote.DispatchStatus.COMPLETED, false);

        assertEquals(WorkOrderNote.FollowUpState.REQUIRED, note.followUpState());
    }
}
