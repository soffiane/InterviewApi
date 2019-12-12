package fr.fabernovel.pokemango.service;

import fr.fabernovel.pokemango.model.PokemonApiResponse;
import fr.fabernovel.pokemango.model.PokemonType;
import fr.fabernovel.pokemango.model.PokemonWithStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PokemonService {

    private static final String urlApiPokeman = "https://pokeapi.co/api/v2/pokemon/";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    public PokemonService() {
    }

    /**
     * Retrieves one pokemon's details from the API.
     *
     * @param name The pokemon's name
     * @return The pokemon's details, including stats calculated from other pokemons of the same type(s).
     */
    public PokemonWithStats getPokemonByName(String name) {
        PokemonWithStats pokemonS = new PokemonWithStats();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        Map<String, String> params = new HashMap<>();
        StringBuilder urlS = new StringBuilder();
        urlS.append(urlApiPokeman);

        if (null != name && "" != name.trim()) {
            params.put("name", name);
            urlS.append("/");
            urlS.append(name);
        } else {
            urlS.append("?offset=20");
            urlS.append("&limit=20");
        }

        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

        Optional<PokemonApiResponse> pokemon = getBody(params, urlS.toString(), entity, PokemonApiResponse.class);

        if (null == pokemon) {
            return pokemonS;
        }
        PokemonApiResponse pokemonApiResponse = pokemon.get();

        setAverageStats(pokemonApiResponse, restTemplate);
        setPokemon(pokemonS, pokemonApiResponse);

        System.out.println(pokemonS);

        return pokemonS;
    }

    private void setAverageStats(PokemonApiResponse pokemon, RestTemplate restTemplate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        List<String> typesUrls = pokemon.getTypes().stream().map(type -> type.getType()).map(t -> t.getUrl()).collect(Collectors.toList());

        List<PokemonType> pokemonTypes = typesUrls.stream().map(url -> getBody(null, url, entity, PokemonType.class).get()).collect(Collectors.toList());

        List<PokemonApiResponse> pokemons = pokemonTypes.stream()
                .flatMap(pokemonType -> pokemonType.getPokemon().stream()
                        .map(pokemonL -> getBody(null, pokemonL.getPokemon().getUrl(), entity, PokemonApiResponse.class).get())).collect(Collectors.toList());

        if (pokemons == null) {
            return;
        }

        List<Integer> stats = pokemon.getStats().stream().flatMap(stat -> pokemons.stream()
                .flatMap(pokemonApiResponse -> pokemonApiResponse.getStats().stream()
                        .filter(stat1 -> stat1.getStat().getName().equalsIgnoreCase(stat.getStat().getName()))
                        .map(stat1 -> stat1.getBase_stat())))
                .collect(Collectors.toList());

        pokemon.getStats().stream().forEach(pStat -> pStat.setAverageStat(stats.stream().mapToInt(i -> i).sum() / stats.size()));

    }

    private void setPokemon(PokemonWithStats pokemonS, PokemonApiResponse pokemon) {
        pokemonS.setBase_experience(pokemon.getBase_experience());
        pokemonS.setHeight(pokemon.getHeight());
        pokemonS.setId(pokemon.getId());
        pokemonS.setName(pokemon.getName());
        pokemonS.setSpecies(pokemon.getSpecies());
        pokemonS.setSprite_img(pokemon.getSprites().getFront_default());
        pokemonS.setStats(pokemon.getStats());
    }

    private <T> Optional<T> getBody(Map<String, String> params, String urlS, HttpEntity<String> entity, final Class<T> clazz) {
        try {
            return Optional.of(restTemplate.exchange(urlS,
                    HttpMethod.GET,
                    entity,
                    clazz, params).getBody());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

}
