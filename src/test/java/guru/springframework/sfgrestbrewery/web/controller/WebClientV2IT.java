package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientV2IT {
    private static final String BASE_URL = "http://localhost:8080";
    private static final String BEER_V2_PATH = "api/v2/beer";
    private static final String BEER_V2_UPC_PATH = "api/v2/beerUpc";
    private static final String NOT_FOUND_EXCEPTION = "org.springframework.web.reactive.function.client.WebClientResponseException$NotFound";
    private static final String BAD_REQ_EXCEPTION = "org.springframework.web.reactive.function.client.WebClientResponseException$BadRequest";
    WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebClient.builder()
                             .baseUrl(BASE_URL)
                             .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                                                                                       .wiretap(true)))
                             .build();
    }

    /**
     * @param throwable
     * @return true if throwable is an instance of NotFound Exception
     */
    private boolean assertExceptionThrown(Throwable throwable, String expectedErrorName) {
        return throwable instanceof WebClientResponseException t &&
                expectedErrorName.equals(t.getClass().getName());
    }

    private Mono<BeerDto> getBeerDtoMono(Integer id) {
        return webClient.get()
                        .uri(BEER_V2_PATH + "/" + id)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(BeerDto.class);
    }

    @Test
    void testGetBeerById() {
        Mono<BeerDto> beerDtoMono = getBeerDtoMono(1);

        StepVerifier.create(beerDtoMono)
                    .assertNext(beerDto -> {
                        assertNotNull(beerDto);
                        assertEquals(Integer.valueOf(1), beerDto.getId());
                        assertEquals("Mango Bobs", beerDto.getBeerName());
                        assertEquals("ALE", beerDto.getBeerStyle());
                    })
                    .verifyComplete();
    }

    @Test
    void testGetBeerById_NotFound() {
        Mono<BeerDto> beerDtoMono = getBeerDtoMono(1000);

        StepVerifier.create(beerDtoMono)
                    .expectErrorMatches(t -> assertExceptionThrown(t, NOT_FOUND_EXCEPTION))
                    .verify();
    }

    private Mono<BeerDto> getBeerByUpcMono(String upc) {
        return webClient.get()
                        .uri(BEER_V2_UPC_PATH + "/" + upc)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(BeerDto.class);
    }

    @Test
    void testGetBeerByUpc() {
        Mono<BeerDto> beerByUpc = getBeerByUpcMono(BeerLoader.BEER_3_UPC);

        StepVerifier.create(beerByUpc)
                    .assertNext(beerDto -> {
                        assertNotNull(beerDto);
                        assertEquals(BeerLoader.BEER_3_UPC, beerDto.getUpc());
                    })
                    .verifyComplete();
    }

    @Test
    void testGetBeerByUpc_NotFound() {
        Mono<BeerDto> beerDtoMono = getBeerByUpcMono(String.valueOf(System.currentTimeMillis()));

        StepVerifier.create(beerDtoMono)
                    .expectErrorMatches(t -> assertExceptionThrown(t, NOT_FOUND_EXCEPTION))
                    .verify();
    }

    private BeerDto createBeerDtoBody() {
        return BeerDto.builder()
                      .beerName("Heineken")
                      .beerStyle("LAGER")
                      .upc("1729369216")
                      .quantityOnHand(100)
                      .price(BigDecimal.valueOf(10))
                      .build();
    }

    @Test
    void testCreateNewBeer() {
        BeerDto beerDto = createBeerDtoBody();
        Mono<ResponseEntity<Void>> response = webClient.post()
                                                       .uri("/api/v2/beer")
                                                       .accept(MediaType.APPLICATION_JSON)
                                                       .body(BodyInserters.fromValue(beerDto))
                                                       .retrieve()
                                                       .toBodilessEntity();

        Optional<String> location = Optional.ofNullable(response.block()
                                                                .getHeaders()
                                                                .get("Location")
                                                                .get(0));
        String[] items = location.get()
                                 .split("/");
        Integer id = Integer.parseInt(items[items.length - 1]);
        log.debug("id: {}", id);

        Mono<BeerDto> beerDtoMono = getBeerDtoMono(id);

        StepVerifier.create(beerDtoMono)
                    .assertNext(actual -> {
                        assertNotNull(actual);
                        assertEquals(id, actual.getId());
                        assertEquals(beerDto.getBeerName(), actual.getBeerName());
                        assertEquals(beerDto.getBeerStyle(), actual.getBeerStyle());
                        assertEquals(beerDto.getUpc(), actual.getUpc());
                    })
                    .verifyComplete();
    }

    @Test
    void testCreateBeerBadRequest() {
        BeerDto invalidBeerDto = BeerDto.builder()
                                        .price(new BigDecimal("8.99"))
                                        .build();

        Mono<ResponseEntity<Void>> beerResponseMono = webClient.post()
                                                               .uri("/api/v2/beer")
                                                               .accept(MediaType.APPLICATION_JSON)
                                                               .body(BodyInserters.fromValue(invalidBeerDto))
                                                               .retrieve()
                                                               .toBodilessEntity();

        StepVerifier.create(beerResponseMono)
                    .expectErrorMatches(t -> assertExceptionThrown(t, BAD_REQ_EXCEPTION))
                    .verify();

    }

    private Mono<ResponseEntity<Void>> updateBeerDto(Integer id, BeerDto payload) {
        return webClient.put()
                        .uri("/api/v2/beer/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(payload))
                        .retrieve()
                        .toBodilessEntity();
    }

    @Test
    void testUpdateBeer() {
        Integer id = Integer.valueOf(1);
        String newName = "Hello New World";
        Mono<BeerDto> beerToUpdateMono = webClient.get()
                                                  .uri("/api/v2/beer/" + id)
                                                  .accept(MediaType.APPLICATION_JSON)
                                                  .retrieve()
                                                  .bodyToMono(BeerDto.class);
        BeerDto beerToUpdate = beerToUpdateMono.block();
        beerToUpdate.setId(null);
        beerToUpdate.setBeerName(newName);

        //update existing beer
        Mono<ResponseEntity<Void>> updateResMono = updateBeerDto(id, beerToUpdate);

        //Assertion #1
        StepVerifier.create(updateResMono)
                    .assertNext(res -> assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode()))
                    .verifyComplete();

        //Assertion #2
        Mono<BeerDto> checkBeerDto = getBeerDtoMono(id);
        StepVerifier.create(checkBeerDto)
                    .assertNext(beer -> assertEquals(newName, beer.getBeerName()))
                    .verifyComplete();
    }

    @Test
    void testUpdateBeer_NotFound() throws InterruptedException {
        Integer invalidId = (int) (Math.random() * Math.pow(10, 6));
        BeerDto updatePayload = BeerDto.builder()
                                       .beerName("JTsUpdate")
                                       .beerStyle("PALE_ALE")
                                       .upc("12345667")
                                       .price(new BigDecimal("9.99"))
                                       .build();
        //update existing beer
        Mono<ResponseEntity<Void>> updateResMono = updateBeerDto(invalidId, updatePayload);

        StepVerifier.create(updateResMono)
                    .expectErrorMatches(t -> assertExceptionThrown(t, NOT_FOUND_EXCEPTION))
                    .verify();
    }

    @Test
    void testDeleteBeer() {
        //Delete the beer first, assert status 2xx
        Integer beerId = Integer.valueOf(1);
        Mono<ResponseEntity<Void>> deleteBeerMono = webClient.delete()
                                                             .uri("/api/v2/beer/" + beerId)
                                                             .retrieve()
                                                             .toBodilessEntity();
        StepVerifier.create(deleteBeerMono)
                    .assertNext(res -> assertTrue(res.getStatusCode().is2xxSuccessful()))
                    .verifyComplete();

        //Get the beer with same id again and assert NotFound
        Mono<BeerDto> doubleCheckMono = getBeerDtoMono(beerId);

        StepVerifier.create(doubleCheckMono)
                    .expectErrorMatches(t -> assertExceptionThrown(t, NOT_FOUND_EXCEPTION))
                    .verify();
    }

    @Test
    void testDeleteBeer_NotFound() {
        Integer invalidBeerId = Integer.valueOf(1000);
        Mono<ResponseEntity<Void>> monoResponse = webClient.delete()
                                                           .uri("/api/v2/beer/" + invalidBeerId)
                                                           .retrieve()
                                                           .toBodilessEntity();

        StepVerifier.create(monoResponse)
                    .expectErrorMatches(t -> assertExceptionThrown(t, NOT_FOUND_EXCEPTION))
                    .verify();
    }
}
