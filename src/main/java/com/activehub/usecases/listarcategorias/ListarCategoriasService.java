package com.activehub.usecases.listarcategorias;

import com.activehub.domain.actividad.CategoriaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarCategoriasService {

    private final CategoriaRepository categoriaRepository;

    public ListarCategoriasService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<ListarCategoriasResponse> listar() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(c -> new ListarCategoriasResponse(c.getId(), c.getNombre()))
                .toList();
    }
}
