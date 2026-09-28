package dev.infrai.healthupload.web;

import dev.infrai.healthupload.workflow.AppointmentUploadWorkflow;
import dev.infrai.healthupload.workflow.AppointmentUploadWorkflow.UploadDecision;
import dev.infrai.healthupload.workflow.AppointmentUploadWorkflow.UploadSubmission;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/appointments")
public final class AppointmentUploadController {
    private final AppointmentUploadWorkflow workflow;

    public AppointmentUploadController(AppointmentUploadWorkflow workflow) {
        this.workflow = workflow;
    }

    @PostMapping(path = "/uploads", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadDecision submit(
            @RequestParam String submissionId,
            @RequestParam String appointmentId,
            @RequestParam String caption,
            @RequestPart MultipartFile image) throws IOException {
        return workflow.submit(new UploadSubmission(
                submissionId, appointmentId, caption, image.getOriginalFilename(), image.getBytes()));
    }
}
