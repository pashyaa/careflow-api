package com.careflow.serviceops.exception;

import com.careflow.serviceops.repository.WorkOrderRepository;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final WorkOrderRepository workOrderRepository;
    private final CurrentActorResolver actorResolver;

    public ApiExceptionHandler(WorkOrderRepository workOrderRepository, CurrentActorResolver actorResolver) {
        this.workOrderRepository = workOrderRepository;
        this.actorResolver = actorResolver;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage(), "resource-not-found");
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException exception) {
        return problem(HttpStatus.CONFLICT, "Business rule violation", exception.getMessage(), "business-rule");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Request validation failed",
                "One or more request fields are invalid.", "validation-error");
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), "invalid-request");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedRequest(HttpMessageNotReadableException exception) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                "Malformed JSON or invalid request value.",
                "invalid-request"
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String detail;
        if ("id".equals(exception.getName())) {
            detail = "Invalid UUID: " + exception.getValue();
        } else {
            detail = "Invalid value '" + exception.getValue() +
                    "' for parameter '" + exception.getName() + "'";
        }
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail, "invalid-request");
    }

    // ---- BUG-304: optimistic locking ------------------------------------------

    @ExceptionHandler(StaleVersionException.class)
    ResponseEntity<ProblemDetail> handleStaleVersion(StaleVersionException exception) {
        return versionConflict(exception.getMessage(), exception.getCurrentVersion());
    }

    // Lost the race between the read and the UPDATE (two requests passed the version
    // check together): Hibernate's @Version made the second UPDATE match 0 rows.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException exception) {
        Long current = null;
        if (exception instanceof ObjectOptimisticLockingFailureException locked
                && locked.getIdentifier() instanceof UUID id) {
            current = workOrderRepository
                    .findVersionByIdAndOrganizationId(id, actorResolver.resolve().organizationId())
                    .orElse(null);
        }
        return versionConflict("This work order was changed by someone else. Refresh and try again.", current);
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    ProblemDetail handlePreconditionRequired(PreconditionRequiredException exception) {
        return problem(HttpStatus.PRECONDITION_REQUIRED, "Precondition required",
                exception.getMessage(), "precondition-required");
    }

    private ResponseEntity<ProblemDetail> versionConflict(String detail, Long currentVersion) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Version conflict", detail, "version-conflict");
        problem.setProperty("code", "VERSION_CONFLICT");
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CONFLICT);
        if (currentVersion != null) {
            problem.setProperty("currentVersion", currentVersion);
            response.eTag(String.valueOf(currentVersion));
        }
        return response.body(problem);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://careflow.local/problems/" + type));
        return problem;
    }
}