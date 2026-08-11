package com.activehub.usecases.listarcategorias;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
public class ListarCategoriasController {

    private final ListarCategoriasService listarCategoriasService;

    public ListarCategoriasController(ListarCategoriasService listarCategoriasService) {
        this.listarCategoriasService = listarCategoriasService;
    }

    @GetMapping
    public List<ListarCategoriasResponse> listar() {
        return listarCategoriasService.listar();
    }
}
