package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
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
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by jt on 3/7/21.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientIT {
    public static final String BASE_URL = "http://localhost:8080";

    WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebClient.builder()
                             .baseUrl(BASE_URL)
                             .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                                                                                       .wiretap(true)))
                             .build();
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
                    .expectError(NotFoundException.class);
    }

    private Mono<BeerDto> getBeerDtoMono(Integer id) {
        return webClient.get()
                        .uri("/api/v1/beer/" + id)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(BeerDto.class);
    }

    @Test
    void testGetBeerByUpc() {
        Mono<BeerDto> beerByUpc = webClient.get()
                                           .uri("/api/v1/beerUpc/" + BeerLoader.BEER_3_UPC)
                                           .accept(MediaType.APPLICATION_JSON)
                                           .retrieve()
                                           .bodyToMono(BeerDto.class);
        StepVerifier.create(beerByUpc)
                    .assertNext(beerDto -> {
                        assertNotNull(beerDto);
                        assertEquals(BeerLoader.BEER_3_UPC, beerDto.getUpc());
                    })
                    .verifyComplete();
    }

    @Test
    void testCreateNewBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        BeerDto beerDto = BeerDto.builder()
                                 .beerName("Heineken")
                                 .beerStyle("LAGER")
                                 .upc("1729369216")
                                 .quantityOnHand(100)
                                 .price(BigDecimal.valueOf(10))
                                 .build();
        Mono<ResponseEntity<Void>> beerDtoMono = webClient.post()
                                                          .uri("/api/v1/beer")
                                                          .accept(MediaType.APPLICATION_JSON)
                                                          .body(BodyInserters.fromValue(beerDto))
                                                          .retrieve()
                                                          .toBodilessEntity();

        beerDtoMono.publishOn(Schedulers.parallel())
                   .subscribe(responseEntity -> {

                       assertTrue(responseEntity.getStatusCode()
                                                .is2xxSuccessful());

                       countDownLatch.countDown();
                   });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, countDownLatch.getCount());
    }

    @Test
    void testListBeers() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerPagedList> beerPagedListMono = webClient.get()
                                                         .uri("/api/v1/beer")
                                                         .accept(MediaType.APPLICATION_JSON)
                                                         .retrieve()
                                                         .bodyToMono(BeerPagedList.class);

        beerPagedListMono.publishOn(Schedulers.parallel())
                         .subscribe(beerPagedList -> {

                             beerPagedList.getContent()
                                          .forEach(beerDto -> System.out.println(beerDto.toString()));

                             countDownLatch.countDown();
                         });

        countDownLatch.await();
    }

    @Test
    void testUpdateBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        webClient.get()
                 .uri("/api/v1/beer")
                 .accept(MediaType.APPLICATION_JSON)
                 .retrieve()
                 .bodyToMono(BeerPagedList.class)
                 .publishOn(Schedulers.single())
                 .subscribe(pagedList -> {
                     countDownLatch.countDown();

                     //get existing beer
                     BeerDto beerDto = pagedList.getContent()
                                                .get(0);

                     BeerDto updatePayload = BeerDto.builder()
                                                    .beerName("JTsUpdate")
                                                    .beerStyle(beerDto.getBeerStyle())
                                                    .upc(beerDto.getUpc())
                                                    .price(beerDto.getPrice())
                                                    .build();

                     //update existing beer
                     webClient.put()
                              .uri("/api/v1/beer/" + beerDto.getId())
                              .contentType(MediaType.APPLICATION_JSON)
                              .body(BodyInserters.fromValue(updatePayload))
                              .retrieve()
                              .toBodilessEntity()
                              .flatMap(responseEntity -> {
                                  //get and verify update
                                  countDownLatch.countDown();
                                  return webClient.get()
                                                  .uri("/api/v1/beer/" + beerDto.getId())
                                                  .accept(MediaType.APPLICATION_JSON)
                                                  .retrieve()
                                                  .bodyToMono(BeerDto.class);
                              })
                              .subscribe(savedDto -> {
                                  assertEquals("JTsUpdate", savedDto.getBeerName());
                                  countDownLatch.countDown();
                              });
                 });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, countDownLatch.getCount());
    }

    @Test
    void testUpdateBeer_NotFound() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(2);

        BeerDto updatePayload = BeerDto.builder()
                                       .beerName("JTsUpdate")
                                       .beerStyle("PALE_ALE")
                                       .upc("12345667")
                                       .price(new BigDecimal("9.99"))
                                       .build();

        webClient.put()
                 .uri("/api/v1/beer/" + 200)
                 .contentType(MediaType.APPLICATION_JSON)
                 .body(BodyInserters.fromValue(updatePayload))
                 .retrieve()
                 .toBodilessEntity()
                 .subscribe(responseEntity -> {
                 }, throwable -> {
                     if ("org.springframework.web.reactive.function.client.WebClientResponseException$NotFound"
                             .equals(throwable.getClass()
                                              .getName())) {
                         WebClientResponseException ex = (WebClientResponseException) throwable;

                         if (ex.getStatusCode()
                               .equals(HttpStatus.NOT_FOUND)) {
                             countDownLatch.countDown();
                         }
                     }
                 });

        countDownLatch.countDown();

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, countDownLatch.getCount());
    }

    @Test
    void testDeleteBeer() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(3);

        webClient.get().uri("/api/v1/beer")
                 .accept(MediaType.APPLICATION_JSON)
                 .retrieve()
                 .bodyToMono(BeerPagedList.class)
                 .publishOn(Schedulers.single())
                 .subscribe(pagedList -> {
                     countDownLatch.countDown();

                     BeerDto beerDto = pagedList.getContent().get(0);

                     webClient.delete().uri("/api/v1/beer/" + beerDto.getId() )
                              .retrieve().toBodilessEntity()
                              .flatMap(responseEntity -> {
                                  countDownLatch.countDown();

                                  return webClient.get().uri("/api/v1/beer/" + beerDto.getId())
                                                  .accept(MediaType.APPLICATION_JSON)
                                                  .retrieve().bodyToMono(BeerDto.class);
                              }) .subscribe(savedDto -> {

                              }, throwable -> {
                                  countDownLatch.countDown();
                              });
                 });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertEquals(0, countDownLatch.getCount());
    }
}
