package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import guru.springframework.sfgrestbrewery.web.model.BeerStyleEnum;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebFluxTest(BeerController.class)
class BeerControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    BeerService beerService;

    BeerDto validBeer;

    @BeforeEach
    void setUp() {
        validBeer = BeerDto.builder()
                           .beerName("Test Beer")
                           .beerStyle("PALE_ALE")
                           .upc(BeerLoader.BEER_3_UPC)
                           .build();
    }

    @Test
    void testGetBeerById() {
        UUID beerId = UUID.randomUUID();
        given(beerService.getById(any(), any())).willReturn(validBeer);

        webTestClient.get()
                     .uri("/api/v1/beer/" + beerId)
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(BeerDto.class)
                     .value(beerDto -> beerDto.getBeerName(), Matchers.equalTo(validBeer.getBeerName()));
    }

    @Test
    public void testGetBeerByUpc() {
        String upc = String.valueOf(System.currentTimeMillis());

        given(beerService.getByUpc(any())).willReturn(validBeer);

        webTestClient.get()
                     .uri("/api/v1/beerUpc/" + upc)
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(BeerDto.class)
                     .value(beerDto -> beerDto.getBeerName(), Matchers.equalTo(validBeer.getBeerName()));
    }

    @Test
    public void testGetListBeers() {
        BeerDto beer1 = BeerDto.builder()
                               .beerName("No Hammers On The Bar")
                               .beerStyle(BeerStyleEnum.WHEAT.toString())
                               .upc(BeerLoader.BEER_3_UPC)
                               .price(new BigDecimal(BigInteger.valueOf(new Random().nextInt(10000)), 2))
                               .quantityOnHand(new Random().nextInt(5000))
                               .build();

        BeerDto beer2 = BeerDto.builder()
                               .beerName("Mango Bobs")
                               .beerStyle(BeerStyleEnum.ALE.toString())
                               .upc(BeerLoader.BEER_1_UPC)
                               .price(new BigDecimal(BigInteger.valueOf(new Random().nextInt(10000)), 2))
                               .quantityOnHand(new Random().nextInt(5000))
                               .build();

        BeerPagedList beerPagedList = new BeerPagedList(List.of(beer1, beer2));

        given(beerService.listBeers(any(), any(), any(), any())).willReturn(beerPagedList);

        webTestClient.get()
                     .uri("/api/v1/beer")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(BeerPagedList.class);
    }
}