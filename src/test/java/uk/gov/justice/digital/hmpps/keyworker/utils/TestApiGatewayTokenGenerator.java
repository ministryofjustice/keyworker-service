package uk.gov.justice.digital.hmpps.keyworker.utils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Ignore
public class TestApiGatewayTokenGenerator {

    private String token = "***";
    private String key = "**";

    private ApiGatewayTokenGenerator generator;

    @Before
    public void init() {
        generator = new ApiGatewayTokenGenerator(token, key);
    }

    @Test
    public void testTokenGenerator() throws Exception {
        final String gatewayToken = generator.createGatewayToken();
        assertThat(gatewayToken).isNotNull();
        System.out.println(gatewayToken);

        final RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "bearer " + gatewayToken);
//        ResponseEntity<Map> health = restTemplate.exchange(
//                "https://noms-api-dev.dsd.io/elite2api/health",
//                HttpMethod.GET,
//                new HttpEntity<>(null, headers),
//                new ParameterizedTypeReference<Map>() {
//                });
//
//        assertThat(health).isNotNull();
    }
}
