package guru.springframework.sfgrestbrewery.web.functional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class BeerRouterConfig {
    private static final String API_V2_BEER = "/api/v2/beer";
    private static final String API_V2_BEER_ID_PATH = "/api/v2/beer/{beerId}";
    private static final String API_V2_BEER_UPC_PATH = "/api/v2/beerUpc/{upc}";

    @Bean
    public RouterFunction<ServerResponse> beerRoutesV2(BeerHandlerV2 handlerV2) {

        return route().GET(API_V2_BEER_ID_PATH, accept(APPLICATION_JSON), handlerV2::getBeerById)
                      .GET(API_V2_BEER_UPC_PATH, accept(APPLICATION_JSON), handlerV2::getBeerByUpc)
                      .POST(API_V2_BEER, accept(APPLICATION_JSON), handlerV2::createNewBeer)
                      .PUT(API_V2_BEER_ID_PATH, accept(APPLICATION_JSON), handlerV2::updateBeer)
                      .DELETE(API_V2_BEER_ID_PATH, accept(APPLICATION_JSON), handlerV2::deleteBeer)
                      .build();
    }
}
