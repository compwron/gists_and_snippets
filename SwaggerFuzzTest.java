

import com.google.common.collect.Lists;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;


public class FuzzTest {

    private String authToken = "REDACTED";
//    userid , guid
    private RestTemplate restTemplate = new RestTemplate();
    private String endpoint = endpoint();
    private Map<String, Path> paths;
    private Matcher<Integer> notServerErrorMatcher = lessThan(INTERNAL_SERVER_ERROR.value());
    private List<String> wipPathsToIgnore;

    @Before
    public void setup() {
        restTemplate.setErrorHandler(errorHandler());
        paths = getRealSwagger().getPaths();

        // Put endpoints for cards that are in progress here temporarily
        // And add a TODO with your jira card number

        wipPathsToIgnore = Lists.newArrayList();
    }

    @Test
    public void allEndpointsMustRequireAuthToken() {
        HttpEntity httpEntity = new HttpEntity(invalidHeaders());
        assertReturnCode(paths, httpEntity, is(FORBIDDEN.value()));
    }

    @Test
    public void noEndpointShouldEverReturnServerErrorWhenPassedEmptyArguments() {
        HttpEntity httpEntity = new HttpEntity(validHeaders());
        assertReturnCode(paths, httpEntity, lessThan(INTERNAL_SERVER_ERROR.value()));
    }

    @Test
    public void noEndpointShouldEverReturnServerErrorWhenPassedInvalidArguments() {
        paths.forEach((pathName, path) -> {
            if (!wipPathsToIgnore.contains(pathName)) {
                httpMethodsToCheck(path).forEach((httpMethod) -> {
                    fuzzingStrategies().forEach((strategyName, strategy) -> {
                        int responseCode = apiResponseCode(interpolatedPathName(pathName), httpMethod, httpEntity(path, httpMethod, strategy));
                        String errorInfo = errorInfo(pathName, httpMethod, strategyName, responseCode);
                        assertThat(errorInfo, responseCode, notServerErrorMatcher);
                    });
                });
            }
        });
    }

    private String interpolatedPathName(String pathName) {
        return pathName.replaceAll("\\{.*\\}", "testValue");
    }

    private HttpEntity httpEntity(Path path, HttpMethod httpMethod, Object strategy) {
        HttpEntity httpEntity;
        if (takesParameters(path, httpMethod)) {
            JSONObject body = new JSONObject();
            for (Parameter parameter : parameters(path, httpMethod)) {
                body.put(parameter.getName(), strategy);
            }
            httpEntity = new HttpEntity(body.toString(), validHeaders());
        } else {
            httpEntity = new HttpEntity(validHeaders());
        }
        return httpEntity;
    }

    private int apiResponseCode(String interpolatedPathName, HttpMethod httpMethod, HttpEntity httpEntity) {
        ResponseEntity<String> response = restTemplate.exchange(endpoint + interpolatedPathName, httpMethod, httpEntity, String.class);
        assertJsonReturn(interpolatedPathName, httpMethod, response);
        return response.getStatusCode().value();
    }

    private void assertJsonReturn(String interpolatedPathName, HttpMethod httpMethod, ResponseEntity<String> response) {
        if (response.getStatusCode() != FORBIDDEN) {
            try {
                new JSONObject(response.getBody());
            } catch (Exception e) {
                String errorMessage = "Cannot parse JSON response to " + httpMethod + " " + interpolatedPathName;
                assertThat(errorMessage, e, is("not an exception"));
            }
        }
    }

    private String errorInfo(String pathName, HttpMethod httpMethod, String strategyName, int value) {
        return "ERROR: path " +
                pathName +
                "\n with http method: " +
                httpMethod +
                "\nwith fields all using strategy: " +
                strategyName +
                "\n returned " +
                value;
    }

    private Map<String, Object> fuzzingStrategies() {
        String hamburgerEmoji = "\uD83C\uDF54";
        String weirdCharacters = hamburgerEmoji + "0~!@#$%^&*()_+`';:,.></?|%n%s\"";

        return new HashMap<String, Object>() {
            {
                put("weirdCharacters", weirdCharacters);
                put("sql injection", "%");
                put("empty string", "");
                put("null", null);
            }
        };
    }

    private boolean takesParameters(Path path, HttpMethod httpMethod) {
        List<Parameter> params = parameters(path, httpMethod);
        return params.size() > 0;
    }

    private List<Parameter> parameters(Path path, HttpMethod httpMethod) {
        return operationMap(path, httpMethod).getParameters()
                .stream()
                .filter(param -> param.getClass().equals(BodyParameter.class))
                .collect(Collectors.toList());
    }

    private void assertReturnCode(Map<String, Path> paths, HttpEntity httpEntity, Matcher<Integer> integerMatcher) {
        paths.forEach((pathName, path) -> {
            if (!wipPathsToIgnore.contains(pathName)) {
                httpMethodsToCheck(path).forEach((httpMethod) -> {
                    int value = apiResponseCode(interpolatedPathName(pathName), httpMethod, httpEntity);
                    assertThat(errorInfo(pathName, httpMethod, "empty fields", value), value, integerMatcher);
                });
            }
        });
    }

    private String endpoint() {
        String endpoint = System.getenv("INTEGRATION_TEST_API_ENDPOINT");
        if (endpoint == null) {
            return "http://localhost:8080";
        }
        return endpoint;
    }

    private Operation operationMap(Path path, HttpMethod httpMethod) {
        switch (httpMethod) {
            case GET:
                return path.getGet();
            case POST:
                return path.getPost();
            case DELETE:
                return path.getDelete();
            case PUT:
                return path.getPut();
            case PATCH:
                return path.getPatch();
            default:
                return null;
        }
    }


    private List<HttpMethod> httpMethodsToCheck(Path path) {
        List<HttpMethod> httpMethods = new ArrayList<>();
        if (path.getDelete() != null) {
            httpMethods.add(HttpMethod.DELETE);
        }
        if (path.getPost() != null) {
            httpMethods.add(HttpMethod.POST);
        }
        if (path.getGet() != null) {
            httpMethods.add(HttpMethod.GET);
        }
        if (path.getPut() != null) {
            httpMethods.add(HttpMethod.PUT);
        }
        if (path.getPatch() != null) {
            System.out.println("Cannot test PATCH yet: see https://bugs.openjdk.java.net/browse/JDK-7016595");
//            do nothing
//            TODO: convert to HttpClient so that PATCH can be sent
//            https://bugs.openjdk.java.net/browse/JDK-7016595
        }
        return httpMethods;
    }


    private HttpHeaders validHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.set("Content-Type", "application/json");
        return headers;
    }


    private HttpHeaders invalidHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid auth token");
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private ResponseErrorHandler errorHandler() {
        return new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }


            @Override
            public void handleError(ClientHttpResponse response) throws IOException {

            }
        };
    }

    private Swagger getRealSwagger() {
        String swaggerLocation = endpoint + "/v2/api-docs";
        Swagger swagger = new SwaggerParser().read(swaggerLocation);
        assertThat("Server may not be up yet; swagger is not findable", swagger, is(not(nullValue())));
        return swagger;
    }
}
