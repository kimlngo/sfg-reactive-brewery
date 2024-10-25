package guru.springframework.sfgrestbrewery.web.functional;


import guru.springframework.sfgrestbrewery.services.BeerService;
import guru.springframework.sfgrestbrewery.web.controller.NotFoundException;
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
    private static final String BEER_ID = "beerId";
    private final BeerService beerService;
    private final Validator validator;

    public Mono<ServerResponse> getBeerById(ServerRequest request) {
        Boolean showInventory = Boolean.valueOf((request.queryParam("showInventory")
                                                        .orElse("false")));

        return beerService.getById(getBeerId(request), showInventory)
                          .switchIfEmpty(Mono.error(NotFoundException::new))
                          .flatMap(beerDto -> ServerResponse.ok()
                                                            .bodyValue(beerDto))
                          .onErrorResume(e -> ServerResponse.notFound()
                                                            .build());
    }

    public Mono<ServerResponse> getBeerByUpc(ServerRequest request) {
        String upc = request.pathVariable("upc");

        return beerService.getByUpc(upc)
                          .switchIfEmpty(Mono.error(NotFoundException::new))
                          .flatMap(beerDto -> ServerResponse.ok()
                                                            .bodyValue(beerDto))
                          .onErrorResume(e -> ServerResponse.notFound()
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

    public Mono<ServerResponse> updateBeer(ServerRequest request) {
        Mono<BeerDto> beerDtoMono = request.bodyToMono(BeerDto.class)
                                           .doOnNext(this::validate);

        return beerService.updateBeerMono(getBeerId(request), beerDtoMono)
                          .flatMap(beerDto -> {
                              if (beerDto.getId() != null)
                                  return ServerResponse.noContent().build();
                              else
                                  return ServerResponse.notFound().build();
                          });
    }

    public Mono<ServerResponse> deleteBeer(ServerRequest request) {
        return beerService.reactiveDeleteById(getBeerId(request))
                          .flatMap(voidMono -> ServerResponse.noContent()
                                                             .build())
                          .onErrorResume(e -> e instanceof NotFoundException, e -> ServerResponse.notFound()
                                                                                                 .build());
    }

    private Integer getBeerId(ServerRequest req) {
        return Integer.valueOf(req.pathVariable(BEER_ID));
    }
}
