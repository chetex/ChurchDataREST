package com.chetex.church.rest.controller;

import com.chetex.church.rest.dto.HomeContentItemDTO;
import com.chetex.church.rest.dto.NavigationItemDTO;
import com.chetex.church.rest.service.WebScrapingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de los endpoints {@code /api/navigation} y {@code /api/home} del
 * {@link WebScrapingController}.
 *
 * <p>Usamos Mockito puro ({@code @Mock} + {@code @InjectMocks}) sin levantar
 * el contexto de Spring: es más rápido y mantiene el test aislado de la red.
 * El {@link WebScrapingService} se mockea completamente, así que los tests
 * verifican <b>exclusivamente</b> la capa controller (routing, serialización
 * JSON y contrato de respuesta).</p>
 */
@ExtendWith(MockitoExtension.class)
class WebScrapingControllerTest {

    @Mock
    private WebScrapingService webScrapingService;

    @InjectMocks
    private WebScrapingController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // MockMvc standalone: no arranca Spring, solo envuelve el controlador.
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getNavigation_devuelveListaDeMenusYSubmenus() throws Exception {
        // Arrange: construimos un árbol de navegación simulado con un submenú.
        NavigationItemDTO inicio = new NavigationItemDTO("Inicio", "https://www.parroquiasanpablovi.es/");
        NavigationItemDTO parroquia = new NavigationItemDTO("Parroquia", "https://www.parroquiasanpablovi.es/parroquia/");
        parroquia.addSubItem(new NavigationItemDTO("Historia", "https://www.parroquiasanpablovi.es/parroquia/historia/"));
        parroquia.addSubItem(new NavigationItemDTO("Equipo", "https://www.parroquiasanpablovi.es/parroquia/equipo/"));

        when(webScrapingService.getNavigation()).thenReturn(List.of(inicio, parroquia));

        // Act + Assert
        mockMvc.perform(get("/api/navigation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Inicio"))
                .andExpect(jsonPath("$[0].url").value("https://www.parroquiasanpablovi.es/"))
                .andExpect(jsonPath("$[1].name").value("Parroquia"))
                .andExpect(jsonPath("$[1].subItems.length()").value(2))
                .andExpect(jsonPath("$[1].subItems[0].name").value("Historia"))
                .andExpect(jsonPath("$[1].subItems[1].name").value("Equipo"));

        verify(webScrapingService, times(1)).getNavigation();
    }

    @Test
    void getHome_devuelveContenidoAgregadoDeHomeYSubpaginas() throws Exception {
        // Arrange: contenido simulado proveniente de 2 páginas distintas.
        HomeContentItemDTO bloqueHome = new HomeContentItemDTO(
                "Bienvenidos",
                "Somos una comunidad parroquial en Madrid.",
                "https://www.parroquiasanpablovi.es/img/portada.jpg",
                "https://www.parroquiasanpablovi.es/sobre-nosotros/",
                "marzo 29, 2026",
                "https://www.parroquiasanpablovi.es/"
        );
        HomeContentItemDTO bloqueHorarios = new HomeContentItemDTO(
                "Horarios de misa",
                "De lunes a viernes 19:30h",
                null,
                "https://www.parroquiasanpablovi.es/horarios/",
                null,
                "https://www.parroquiasanpablovi.es/horarios/"
        );

        when(webScrapingService.getHomeAggregate()).thenReturn(List.of(bloqueHome, bloqueHorarios));

        // Act + Assert
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Primer elemento (home)
                .andExpect(jsonPath("$[0].title").value("Bienvenidos"))
                .andExpect(jsonPath("$[0].text").value("Somos una comunidad parroquial en Madrid."))
                .andExpect(jsonPath("$[0].image").value("https://www.parroquiasanpablovi.es/img/portada.jpg"))
                .andExpect(jsonPath("$[0].linkUrl").value("https://www.parroquiasanpablovi.es/sobre-nosotros/"))
                .andExpect(jsonPath("$[0].date").value("marzo 29, 2026"))
                .andExpect(jsonPath("$[0].sourceUrl").value("https://www.parroquiasanpablovi.es/"))
                // Segundo elemento (sub-página): verifica que nulls se serializan correctamente
                .andExpect(jsonPath("$[1].title").value("Horarios de misa"))
                .andExpect(jsonPath("$[1].date").doesNotExist())
                .andExpect(jsonPath("$[1].image").doesNotExist())
                .andExpect(jsonPath("$[1].sourceUrl").value("https://www.parroquiasanpablovi.es/horarios/"));

        verify(webScrapingService, times(1)).getHomeAggregate();
    }
}
