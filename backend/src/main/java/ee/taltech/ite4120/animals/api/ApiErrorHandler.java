package ee.taltech.ite4120.animals.api;

import ee.taltech.ite4120.animals.ownerregistry.OwnerRegistryException;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ApiException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * One error shape for every endpoint: RFC 9457 problem details
 * ({@code application/problem+json}). 4xx = the caller's fault, 5xx = ours;
 * never a stack trace, never framework HTML. This is the S3 error-model slide
 * as running code.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail beanValidation(MethodArgumentNotValidException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setDetail(e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .reduce((a, b) -> a + "; " + b)
                .orElse("request body is invalid"));
        return pd;
    }

    @ExceptionHandler(ApiClientException.class)
    public ProblemDetail clientError(ApiClientException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid request");
        pd.setDetail(detail(e));
        return pd;
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Not found");
        pd.setDetail(detail(e));
        return pd;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail conflict(ConflictException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Conflict with current state");
        pd.setDetail(detail(e));
        return pd;
    }

    /**
     * The commons exceptions render their message as {@code "CODE: text"} — the code
     * is for logs and machine matching, the text is for people. The problem document
     * carries the human text; the status code already says what kind of error it is.
     */
    private static String detail(ApiException e) {
        String message = e.getMessage();
        return message == null ? null : message.replaceFirst("^[\\w-]+: ", "");
    }

    /** The registry being down is an upstream failure, not the caller's fault: 502. */
    @ExceptionHandler(OwnerRegistryException.class)
    public ProblemDetail registryFailure(OwnerRegistryException e) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setTitle("Owner registry unavailable");
        pd.setDetail(e.getMessage());
        return pd;
    }
}
