package com.aluracursos.desafio.principal;

import com.aluracursos.desafio.model.*;
import com.aluracursos.desafio.repository.RepositorioAutor;
import com.aluracursos.desafio.repository.RepositorioLibro;
import com.aluracursos.desafio.service.ConsumoAPI;
import com.aluracursos.desafio.service.ConvierteDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;



    @Component
    public class Principal {

        private static final String URL_BASE = "https://gutendex.com/books/";

        @Autowired
        private ConsumoAPI consumoAPI;

        @Autowired
        private ConvierteDatos conversor;

        @Autowired
        private RepositorioLibro repoLibro;

        @Autowired
        private RepositorioAutor repoAutor;



        private List<Libro> librosBuscados = new ArrayList<>();

        private final Scanner teclado = new Scanner(System.in);

        public void muestraElMenu() {
            int opcion;

            do {
                mostrarBanner();
                try {
                    opcion = teclado.nextInt();
                    teclado.nextLine(); // limpiar el buffer

                    switch (opcion) {
                        case 1:
                            buscarLibroPorTitulo();
                            break;
                        case 2:
                            listarLibros();
                            break;
                        case 3:
                            buscarAutorPorNombre();
                            break;
                        case 4:
                            buscarAutoresVivosEnAnio();
                            break;
                        case 5:
                            buscarLibroPorIdioma();
                            break;
                        case 6:
                            mostrarLibrosBuscados();
                            break;
                        case 7:
                            mostrarCantidadDeLibrosPorIdioma();
                            break;
                        case 9:
                            System.out.println("👋 Gracias por utilizar Gutendex-Alura-On-line.");
                            break;
                        default:
                            System.out.println("⚠️ Opción inválida. Intenta nuevamente.");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("❌ Error: Ingresa un número válido.");
                    teclado.nextLine();
                    opcion = -1;
                }

            } while (opcion != 9);
        }

        private void mostrarBanner() {
            System.out.println("--------------------------------------------------------------------");
            System.out.print("""
                           ,--.   ,--.  ,--.                  ,---.  ,--.                      
                           |  |   `--',-'  '-. ,---. ,--.--. /  O  \\ |  |,--.,--.,--.--.,--,--.
                           |  |   ,--.'-.  .-'| .-. :|  .--'|  .-.  ||  ||  ||  ||  .--' ,-.  |
                           |  '--.|  |  |  |  \\   --.|  |   |  | |  ||  |'  ''  '|  |  \\ '-'  |
                           `-----'`--'  `--'   `----'`--'   `--' `--'`--' `----' `--'   `--`--'
                    """);
            System.out.println("--------------------------------------------------------------------");
            System.out.println("*************** Ingresa la opción que deseas ***************\n");
            System.out.println("---------------> 1. Buscar libro por título");
            System.out.println("---------------> 2. Listar todos los libros");
            System.out.println("---------------> 3. Buscar por autor");
            System.out.println("---------------> 4. Buscar autores vivos en determinado año");
            System.out.println("---------------> 5. Buscar libro por idioma");
            System.out.println("---------------> 6. Mostrar libros buscados");
            System.out.println("---------------> 7. Mostrar la cantidad de libros por idioma");
            System.out.println("---------------> 9. Salir");
            System.out.print("\nOpción: ");
        }


        private void buscarLibroPorTitulo() {
            Scanner scanner = new Scanner(System.in);
            System.out.print("🔍 Ingresa el título del libro: ");
            String titulo = scanner.nextLine();


            Optional<Libro> libroExistente = repoLibro.findByTitulo(titulo);
            if (libroExistente.isPresent()) {
                System.out.println("⚠️ El libro ya existe en la base de datos:");
                System.out.println(libroExistente.get());
                return; // Evita continuar el proceso
            }

            // Consulta a la API
            String urlBusqueda = URL_BASE + "?search=" + titulo.replace(" ", "+");
            String json = consumoAPI.obtenerDatos(urlBusqueda);
            Datos datos = conversor.obtenerDatos(json, Datos.class);

            // Buscar el primer libro cuyo título contenga el texto ingresado (sin distinguir mayúsculas)
            Optional<DatosLibros> libroEncontrado = datos.resultados().stream()
                    .filter(l -> l.titulo().toLowerCase().contains(titulo.toLowerCase()))
                    .findFirst();

            if (libroEncontrado.isPresent()) {
                DatosLibros libroApi = libroEncontrado.get();

                // Convertir clase Libro
                String nombreAutor = libroApi.autor().isEmpty() ? "Autor desconocido" : libroApi.autor().get(0).nombre();
                Autor autorEntidad = repoAutor.findByNombre(nombreAutor)
                        .orElseGet(() -> {
                            Autor nuevoAutor = new Autor();
                            nuevoAutor.setNombre(nombreAutor);
                            if (!libroApi.autor().isEmpty()) {
                                nuevoAutor.setFechaNacimiento(libroApi.autor().get(0).fechaDeNacimiento());
                                nuevoAutor.setFechaFallecimiento(libroApi.autor().get(0).fechaDeFallecimiento());
                            }
                            return repoAutor.save(nuevoAutor);
                        });

                Libro libroEntidad = new Libro(
                        libroApi.titulo(),
                        autorEntidad,
                        libroApi.idiomas().isEmpty() ? "desconocido" : libroApi.idiomas().get(0),
                        libroApi.numeroDeDescargas().intValue()
                );


                try {
                    repoLibro.save(libroEntidad);
                    System.out.println("✅ Libro guardado exitosamente.");
                } catch (DataIntegrityViolationException e) {
                    System.out.println("❌ No se pudo guardar el libro: ya existe uno con ese título.");
                }

            } else {
                System.out.println("❌ No se encontró un libro que coincida con el título ingresado.");
            }
        }

        private void listarLibros() {
            System.out.println("📚 Listar libros desde API...");
            var json = consumoAPI.obtenerDatos(URL_BASE);
            var datos = conversor.obtenerDatos(json, Datos.class);
            datos.resultados().forEach(libro ->
                    System.out.println("- " + libro.titulo())
            );
        }

        private void buscarAutorPorNombre() {
            Scanner scanner = new Scanner(System.in);
            System.out.println("🔍 Ingrese el nombre del autor a buscar:");
            String nombreAutor = scanner.nextLine();

            String urlBusqueda = URL_BASE + "?search=" + nombreAutor.replace(" ", "+");
            String json = consumoAPI.obtenerDatos(urlBusqueda);
            Datos datosBusqueda = conversor.obtenerDatos(json, Datos.class);

            List<DatosLibros> librosDelAutor = datosBusqueda.resultados().stream()
                    .filter(libro -> libro.autor().stream()
                            .anyMatch(a -> a.nombre().toLowerCase().contains(nombreAutor.toLowerCase())))
                    .toList();

            if (librosDelAutor.isEmpty()) {
                System.out.println("❌ No se encontraron libros de ese autor.");
            } else {
                System.out.println("📚 Libros encontrados del autor:");
                librosDelAutor.forEach(l -> {
                    System.out.println("- " + l.titulo() + " (" + l.idiomas() + ")");
                });
            }
        }

        private void buscarAutoresVivosEnAnio() {
            System.out.println("📅 Buscar autores vivos en determinado año");
            System.out.print("Ingrese el año: ");

            try {
                int anio = teclado.nextInt();
                teclado.nextLine();

                List<Autor> autoresVivos = repoAutor.findAutoresVivosEnAnio(anio);

                if (autoresVivos.isEmpty()) {
                    System.out.println("❌ No se encontraron autores vivos en el año " + anio);
                } else {
                    System.out.println("✅ Autores vivos en el año " + anio + ":");
                    autoresVivos.forEach(a -> System.out.println("👤 " + a.getNombre()));
                }
            } catch (InputMismatchException e) {
                System.out.println("❌ Entrada inválida. Debe ingresar un número.");
                teclado.nextLine();
            }
        }

        private void buscarLibroPorIdioma() {
            Scanner scanner = new Scanner(System.in);
            System.out.println("🌍 Ingrese el código de idioma (por ejemplo, 'es' para español, 'en' para inglés, 'fr' para francés):");
            String codigoIdioma = scanner.nextLine();

            String urlBusqueda = URL_BASE + "?languages=" + codigoIdioma;
            String json = consumoAPI.obtenerDatos(urlBusqueda);
            Datos datosBusqueda = conversor.obtenerDatos(json, Datos.class);

            List<DatosLibros> librosPorIdioma = datosBusqueda.resultados();

            if (librosPorIdioma.isEmpty()) {
                System.out.println("❌ No se encontraron libros en ese idioma.");
            } else {
                System.out.println("📚 Libros en idioma '" + codigoIdioma + "':");
                librosPorIdioma.forEach(libro -> {
                    String idioma = libro.idiomas().isEmpty() ? "desconocido" : libro.idiomas().get(0);
                    String autor = libro.autor().isEmpty() ? "Autor desconocido" : libro.autor().get(0).nombre();
                    System.out.println("- " + libro.titulo() + " | Autor: " + autor + " | Idioma: " + idioma);
                });

            }

        }

        private void mostrarLibrosBuscados() {
            List<Libro> libros = repoLibro.findAll();


            if (libros.isEmpty()) {
                System.out.println("⚠️ No hay libros guardados en la base de datos.");
                return;
            }

            System.out.println("\n📚 Libros buscados (ordenados por autor):\n");

            libros.stream()
                    .filter(libro -> libro.getAutor() != null)
                    .sorted(Comparator.comparing(libro -> libro.getAutor().getNombre()))
                    .forEach(System.out::println);


        }

        private void mostrarCantidadDeLibrosPorIdioma() {
            Scanner scanner = new Scanner(System.in);
            System.out.println("🌍 ¿De qué idioma quieres ver la cantidad de libros?");
            System.out.println("Ejemplos: en (inglés), es (español), fr (francés)");
            System.out.print("Idioma: ");
            String idioma = scanner.nextLine().trim().toLowerCase();

            Long cantidad = repoLibro.countByIdioma(idioma);

            System.out.println("📚 Hay " + cantidad + " libros en el idioma '" + idioma + "' en la base de datos.");
        }
    }




