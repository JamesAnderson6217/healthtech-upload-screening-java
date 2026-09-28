package dev.infrai.healthupload.workflow;

import dev.infrai.healthupload.infrai.ImageOperations;
import dev.infrai.healthupload.infrai.ModerationGateway;
import org.junit.jupiter.api.Test;

import static dev.infrai.healthupload.workflow.AppointmentUploadWorkflow.UploadState.QUARANTINED;
import static dev.infrai.healthupload.workflow.AppointmentUploadWorkflow.UploadState.READY_TO_PUBLISH;
import static org.assertj.core.api.Assertions.assertThat;

class AppointmentUploadWorkflowTest {
    @Test
    void quarantinesFlaggedUploadWithoutCreatingPublishVariant() {
        RecordingImages images = new RecordingImages();
        AppointmentUploadWorkflow workflow = new AppointmentUploadWorkflow(
                images, (caption, imageUrl) -> new ModerationGateway.ModerationAssessment(true, "moderation-model"));

        var decision = workflow.submit(submission("flagged-case"));

        assertThat(decision.state()).isEqualTo(QUARANTINED);
        assertThat(decision.publishUrl()).isNull();
        assertThat(decision.notification().event()).isEqualTo("UPLOAD_REVIEW_REQUIRED");
        assertThat(images.resizeCalls).isZero();
    }

    @Test
    void publishesTransformedVariantAfterBothInputsPass() {
        RecordingImages images = new RecordingImages();
        AppointmentUploadWorkflow workflow = new AppointmentUploadWorkflow(
                images, (caption, imageUrl) -> new ModerationGateway.ModerationAssessment(false, "moderation-model"));

        var decision = workflow.submit(submission("safe-case"));

        assertThat(decision.state()).isEqualTo(READY_TO_PUBLISH);
        assertThat(decision.publishUrl()).isEqualTo("https://cdn.example/publish.webp");
        assertThat(decision.notification().message()).doesNotContain("Routine follow-up");
        assertThat(images.resizeCalls).isOne();
    }

    private static AppointmentUploadWorkflow.UploadSubmission submission(String id) {
        return new AppointmentUploadWorkflow.UploadSubmission(
                id, "appt_ref_2048", "Routine follow-up", "check-in.jpg", new byte[]{1, 2, 3});
    }

    private static final class RecordingImages implements ImageOperations {
        private int resizeCalls;

        @Override
        public UploadedImage upload(byte[] file, String filename, String idempotencyKey) {
            return new UploadedImage("img_source", "https://cdn.example/source.jpg");
        }

        @Override
        public PublishedImage resize(String image, String idempotencyKey) {
            resizeCalls++;
            return new PublishedImage("img_publish", "https://cdn.example/publish.webp");
        }
    }
}
