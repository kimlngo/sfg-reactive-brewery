package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.util.concurrent.CountDownLatch;

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
}
