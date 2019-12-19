package fr.fabernovel.pokemango.service;

import fr.fabernovel.pokemango.exception.PokemonNotFoundException;
import fr.fabernovel.pokemango.model.PokemonApiResponse;
import fr.fabernovel.pokemango.model.PokemonType;
import fr.fabernovel.pokemango.model.PokemonWithStats;
import fr.fabernovel.pokemango.model.Stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class PokemonService {

    private static final String URL_POKEMON_API = "https://pokeapi.co/api/v2/pokemon/";

    private final RestTemplate restTemplate;

    public PokemonService(@Autowired RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieves one pokemon's details from the API.
     *
     * @param name The pokemon's name
     * @return The pokemon's details, including stats calculated from other pokemons of the same type(s).
     */
    public PokemonWithStats getPokemonByName(String name) throws PokemonNotFoundException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name must not be null neither empty");
        }

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("name", name);

        return fetchApi(URL_POKEMON_API, queryParams, PokemonApiResponse.class)
                .map(this::buildPokemonWithStats)
                .orElseThrow(() -> new PokemonNotFoundException("name: " + name));
    }

    // I'm not sure this function will work
    public PokemonWithStats getPokemons() {
        final String requestUrl = URL_POKEMON_API + "?offset=20&limit=20";

        return fetchApi(requestUrl, PokemonApiResponse.class)
                .map(this::buildPokemonWithStats)
                .orElseThrow(() -> new PokemonNotFoundException("no pokemons found"));
    }

    private PokemonWithStats buildPokemonWithStats(PokemonApiResponse pokemonFromApi) {
        final PokemonWithStats pokemon = this.toPokemonWithStats(pokemonFromApi);

        List<Integer> stats = fetchAverageStats(pokemonFromApi);

        //final int statAverage = stats.stream().mapToInt((Function.identity()).sum() / stats.size());
        IntSummaryStatistics statistics = stats.stream().collect(Collectors.summarizingInt(Integer::intValue));
        final int statAverage = (int) statistics.getAverage();
        pokemon.getStats().forEach(stat -> stat.setAverageStat(statAverage));

        return pokemon;
    }

    private List<Integer> fetchAverageStats(PokemonApiResponse pokemon) {
        List<PokemonApiResponse> pokemons = pokemon.getTypes().stream()
                .map(typeUrl -> fetchApi(typeUrl.getType().getUrl(), PokemonType.class))
                .flatMap(this::toStream)
                .flatMap(pokemonType -> pokemonType.getPokemon().stream())
                .map(pkmn -> fetchApi(pkmn.getPokemon().getUrl(), PokemonApiResponse.class))
                .flatMap(this::toStream)
                .collect(Collectors.toList());

        List<Integer> stats = pokemon.getStats().stream().flatMap(stat ->
                pokemons.stream()
                        .flatMap(p -> p.getStats().stream())
                        .filter(statFromApi -> statFromApi.getStat().getName().equalsIgnoreCase(stat.getStat().getName()))
                        .map(Stat::getBase_stat)
        ).collect(Collectors.toList());

        return stats;
    }

    private <T> Stream<T> toStream(Optional<T> option) {
        return option.map(Stream::of).orElseGet(Stream::empty);
    }

    private PokemonWithStats toPokemonWithStats(PokemonApiResponse pokemonFromApi) {
        final PokemonWithStats pokemon = new PokemonWithStats();
        pokemon.setBase_experience(pokemonFromApi.getBase_experience());
        pokemon.setHeight(pokemonFromApi.getHeight());
        pokemon.setId(pokemonFromApi.getId());
        pokemon.setName(pokemonFromApi.getName());
        pokemon.setSpecies(pokemonFromApi.getSpecies());
        pokemon.setSprite_img(pokemonFromApi.getSprites().getFront_default());
        pokemon.setStats(pokemonFromApi.getStats());
        return pokemon;
    }

    private <T> Optional<T> fetchApi(String url, final Class<T> clazz) {
        return fetchApi(url, null, clazz);
    }

    private <T> Optional<T> fetchApi(String url, Map<String, String> queryParams, final Class<T> clazz) {
        // TODO: implement a better management of errors
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    defaultJsonHeaders(),
                    clazz, queryParams
            );

            if (response.getStatusCode().value() == 200) {
                return Optional.of(response.getBody());
            } else {
                return Optional.empty();
            }
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    private HttpEntity<String> defaultJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return new HttpEntity<String>("parameters", headers);
    }

}
