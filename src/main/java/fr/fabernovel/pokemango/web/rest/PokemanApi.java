package fr.fabernovel.pokemango.web.rest;


import fr.fabernovel.pokemango.model.PokemonWithStats;
import fr.fabernovel.pokemango.service.PokemonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

//@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class PokemanApi {
    private static final String urlApiPokeman = "https://pokeapi.co/api/v2/pokeman";

    @Autowired
    PokemonService pokemonService;

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping("api/pokemon")
    public ResponseEntity<PokemonWithStats> getPokemons(@RequestParam(value="name", required = false)String name) throws IOException {
        PokemonWithStats pokemon = this.pokemonService.getPokemonByName(name);

        return ResponseEntity.ok(pokemon);
    }


}
