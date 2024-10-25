package guru.springframework.sfgrestbrewery.web.functional;


import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeerHandlerV2 {
    private static final String LOCATION_PATH = "http://localhost:8080/api/v2/beer/";
    private final BeerService beerService;
    private final Validator validator;

    public Mono<ServerResponse> getBeerById(ServerRequest request) {
        Integer beerId = Integer.valueOf(request.pathVariable("beerId"));
        Boolean showInventory = Boolean.valueOf((request.queryParam("showInventory")
                                                        .orElse("false")));

        return beerService.getById(beerId, showInventory)
                          .flatMap(beerDto -> ServerResponse.ok()
                                                            .bodyValue(beerDto))
                          .switchIfEmpty(ServerResponse.notFound()
                                                       .build());
    }

    public Mono<ServerResponse> getBeerByUpc(ServerRequest request) {
        String upc = request.pathVariable("upc");

        return beerService.getByUpc(upc)
                          .flatMap(beerDto -> ServerResponse.ok()
                                                            .bodyValue(beerDto))
                          .switchIfEmpty(ServerResponse.notFound()
                                                       .build());
    }

    public Mono<ServerResponse> createNewBeer(ServerRequest request) {
        Mono<BeerDto> beerDtoMono = request.bodyToMono(BeerDto.class)
                                           .doOnNext(this::validate);

        return beerService.saveNewBeerMono(beerDtoMono)
                          .flatMap(beerDto -> ServerResponse
                                  .created(UriComponentsBuilder.fromHttpUrl(LOCATION_PATH + beerDto.getId())
                                                               .build()
                                                               .toUri())
                                  .build());
    }

    private void validate(BeerDto beerDto) {
        Errors errors = new BeanPropertyBindingResult(beerDto, "beerDto");
        validator.validate(beerDto, errors);

        if (errors.hasErrors()) {
            throw new ServerWebInputException(errors.toString());
        }
    }
}
