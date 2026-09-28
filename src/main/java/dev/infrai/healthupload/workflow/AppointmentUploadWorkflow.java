package dev.infrai.healthupload.workflow;

import dev.infrai.healthupload.infrai.ImageOperations;
import dev.infrai.healthupload.infrai.ModerationGateway;
import org.springframework.stereotype.Service;

@Service
public final class AppointmentUploadWorkflow {
    private final ImageOperations images;
    private final ModerationGateway moderation;

    public AppointmentUploadWorkflow(ImageOperations images, ModerationGateway moderation) {
        this.images = images;
        this.moderation = moderation;
    }

    public UploadDecision submit(UploadSubmission input) {
        String operationKey = "appointment-upload-" + input.submissionId();
        ImageOperations.UploadedImage uploaded = images.upload(input.file(), input.filename(), operationKey + "-source");
        ModerationGateway.ModerationAssessment assessment = moderation.screen(input.caption(), uploaded.url());

        if (assessment.flagged()) {
            return new UploadDecision(
                    input.submissionId(), input.appointmentId(), UploadState.QUARANTINED,
                    uploaded.id(), null, assessment.model(),
                    new OperationalNotification("UPLOAD_REVIEW_REQUIRED", input.appointmentId(),
                            "An appointment upload is waiting for clinical operations review."));
        }

        ImageOperations.PublishedImage published = images.resize(uploaded.url(), operationKey + "-publish");
        return new UploadDecision(
                input.submissionId(), input.appointmentId(), UploadState.READY_TO_PUBLISH,
                uploaded.id(), published.url(), assessment.model(),
                new OperationalNotification("UPLOAD_SCREENED", input.appointmentId(),
                        "An appointment upload passed screening and is ready to publish."));
    }

    public record UploadSubmission(
            String submissionId, String appointmentId, String caption, String filename, byte[] file) {
        public UploadSubmission {
            if (submissionId == null || submissionId.isBlank()
                    || appointmentId == null || appointmentId.isBlank()
                    || caption == null || filename == null || filename.isBlank()
                    || file == null || file.length == 0) {
                throw new IllegalArgumentException("Submission fields and file are required");
            }
        }
    }

    public enum UploadState { QUARANTINED, READY_TO_PUBLISH }

    public record OperationalNotification(String event, String appointmentRef, String message) {}

    public record UploadDecision(
            String submissionId,
            String appointmentId,
            UploadState state,
            String sourceImageId,
            String publishUrl,
            String moderationModel,
            OperationalNotification notification) {}
}
