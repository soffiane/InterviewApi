package fr.fabernovel.pokemango.service;

import fr.fabernovel.pokemango.model.PokemonWithStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Service
public class PokemonService {

    @Autowired
    public PokemonService() {
    }

    public PokemonWithStats getPokemonByName(String name) {
        throw new NotImplementedException();
    }
}
