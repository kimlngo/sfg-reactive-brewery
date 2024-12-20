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
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@AutoConfigureWebTestClient
class BeerControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    BeerService beerService;

    BeerDto validBeer;

    @BeforeEach
    void setUp() {
        validBeer = BeerDto.builder()
                           .id(1)
                           .beerName("Test Beer")
                           .beerStyle("PALE_ALE")
                           .upc(BeerLoader.BEER_3_UPC)
                           .build();
    }

    @Test
    void testGetBeerById() {
        given(beerService.getById(any(), any())).willReturn(Mono.just(validBeer));

        webTestClient.get()
                     .uri("/api/v1/beer/1")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(BeerDto.class)
                     .value(beerDto -> beerDto.getBeerName(), Matchers.equalTo(validBeer.getBeerName()));
    }

    @Test
    public void testGetBeerByUpc() {
        String upc = BeerLoader.BEER_3_UPC;

        given(beerService.getByUpc(any())).willReturn(Mono.just(validBeer));

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

        List<BeerDto> beerDtoList = List.of(beer1, beer2);
        BeerPagedList beerPagedList = new BeerPagedList(beerDtoList, PageRequest.of(0, 10), beerDtoList.size());

        given(beerService.listBeers(any(), any(), any(), any())).willReturn(Mono.just(beerPagedList));

        webTestClient.get()
                     .uri("/api/v1/beer")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody(BeerPagedList.class);
    }
}