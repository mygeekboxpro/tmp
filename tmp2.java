import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class RestApiExecutor<T, R> {
    private String url;
    private HttpMethod method;
    private HttpHeaders headers = new HttpHeaders();
    private T body;

    private RestApiExecutor() {}

    public static <T, R> UrlStep<T, R> newRequest() {
        return new Builder<>();
    }

    public ResponseEntity<R> execute(RestTemplate restTemplate, Class<R> responseType) {
        HttpEntity<T> entity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(url, method, entity, responseType);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RestApiException(e.getStatusCode().value(), e.getMessage());
        } catch (RestClientException e) {
            throw new RestApiException(500, "Request execution failed: " + e.getMessage());
        }
    }

    public interface UrlStep<T, R> {
        MethodStep<T, R> url(String url);
    }

    public interface MethodStep<T, R> {
        HeadersStep<T, R> method(HttpMethod method);
    }

    public interface HeadersStep<T, R> {
        HeadersStep<T, R> addHeader(String key, String value);
        BodyStep<T, R> headersDone();
    }

    public interface BodyStep<T, R> {
        BodyStep<T, R> setBody(T body);
        BuildStep<T, R> bodyDone();
    }

    public interface BuildStep<T, R> {
        RestApiExecutor<T, R> build();
    }

    private static class Builder<T, R> implements UrlStep<T, R>, MethodStep<T, R>, HeadersStep<T, R>, BodyStep<T, R>, BuildStep<T, R> {
        private final RestApiExecutor<T, R> instance = new RestApiExecutor<>();

        @Override
        public MethodStep<T, R> url(String url) {
            instance.url = url;
            return this;
        }

        @Override
        public HeadersStep<T, R> method(HttpMethod method) {
            instance.method = method;
            return this;
        }

        @Override
        public HeadersStep<T, R> addHeader(String key, String value) {
            instance.headers.add(key, value);
            return this;
        }

        @Override
        public BodyStep<T, R> headersDone() {
            return this;
        }

        @Override
        public BodyStep<T, R> setBody(T body) {
            instance.body = body;
            return this;
        }

        @Override
        public BuildStep<T, R> bodyDone() {
            return this;
        }

        @Override
        public RestApiExecutor<T, R> build() {
            return instance;
        }
    }

    public static class RestApiException extends RuntimeException {
        private final int errorCode;

        public RestApiException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();

        RestApiExecutor<String, String> executor = RestApiExecutor.<String, String>newRequest()
                .url("https://example.com/api")
                .method(HttpMethod.POST)
                .addHeader("Content-Type", "application/json")
                .headersDone()
                .setBody("{\"key\": \"value\"}")
                .bodyDone()
                .build();

        try {
            ResponseEntity<String> response = executor.execute(restTemplate, String.class);
            System.out.println(response.getBody());
        } catch (RestApiException e) {
            System.err.println("API call failed: Error Code " + e.getErrorCode() + " - " + e.getMessage());
        }
    }
}
