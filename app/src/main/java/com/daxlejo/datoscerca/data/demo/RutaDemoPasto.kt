package com.daxlejo.datoscerca.data.demo

import com.daxlejo.datoscerca.data.ia.CacheDatos
import com.daxlejo.datoscerca.domain.error.Resultado
import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.Intensidad
import com.daxlejo.datoscerca.domain.modelo.Lugar
import com.daxlejo.datoscerca.domain.modelo.Relevancia
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import com.daxlejo.datoscerca.domain.puerto.FuenteLugares
import kotlin.math.roundToInt

/**
 * Sitio verificado de la ruta demo de Pasto (coordenadas de OpenStreetMap e identificadores
 * equivalentes en Wikipedia y Nominatim), con sus textos precargados por tipo e intensidad.
 */
data class ParadaDemo(
    val idPrincipal: String,
    val idsAlternos: List<String> = emptyList(),
    val nombre: String,
    val coordenada: Coordenada,
    val contexto: String,
    val fuente: String,
    val relevancia: Relevancia,
    val frasesPorTipo: Map<TipoDato, List<String>>,
) {
    val todosLosIds: List<String> get() = listOf(idPrincipal) + idsAlternos
}

/**
 * Fuente local para el modo demo: permite recorrer `demo/ruta-pasto.gpx` con el Wi-Fi apagado.
 */
class FuenteLugaresDemo(
    private val paradas: List<ParadaDemo> = RutaDemoPasto.paradas,
) : FuenteLugares {

    override suspend fun buscarCercanos(coordenada: Coordenada, radioMetros: Int): Resultado<List<Lugar>> {
        val cercanos = paradas
            .map { parada ->
                val distancia = coordenada.distanciaMetrosA(parada.coordenada).roundToInt()
                Lugar(
                    id = parada.idPrincipal,
                    nombre = parada.nombre,
                    coordenada = parada.coordenada,
                    distanciaMetros = distancia,
                    contexto = parada.contexto,
                    fuente = parada.fuente,
                    relevancia = parada.relevancia,
                )
            }
            .filter { it.distanciaMetros <= radioMetros }
            .sortedBy(Lugar::distanciaMetros)
        return Resultado.Exito(cercanos)
    }
}

/**
 * Datos verificados de los 8 hitos del centro histórico de Pasto incluidos en `demo/ruta-pasto.gpx`.
 * Al activar el modo demo se precargan en [CacheDatos] para que la ruta funcione sin conexión.
 */
object RutaDemoPasto {

    private val tiendaBarrio = ParadaDemo(
        idPrincipal = "nominatim:node:tienda-barrio",
        nombre = "Tienda El Mirador",
        coordenada = Coordenada(1.21750, -77.28620),
        contexto = "Tienda de barrio con víveres y bebidas.",
        fuente = "nominatim",
        relevancia = Relevancia.NINGUNA,
        frasesPorTipo = emptyMap(),
    )

    private val museoTaminango = ParadaDemo(
        idPrincipal = "nominatim:way:115238095",
        idsAlternos = listOf("wikipedia-es:4511690"),
        nombre = "Museo Taminango",
        coordenada = Coordenada(1.2152045, -77.2842910),
        fuente = "nominatim",
        relevancia = Relevancia.ETIQUETA_OSM_PATRIMONIAL,
        contexto = "El Museo Taminango de Artes y Tradiciones Populares de Nariño funciona en una casona " +
            "colonial de 1623 declarada monumento nacional. Exhibe oficios tradicionales como el barniz de " +
            "Pasto (mopa-mopa), la talla en madera y los tejidos, y conserva su patio empedrado y una cocina " +
            "antigua con fogón de leña donde se explica la preparación del cuy asado y los lapingachos.",
        frasesPorTipo = mapOf(
            TipoDato.INTERESANTE to listOf(
                "El Museo Taminango reúne las artes y tradiciones populares de Nariño en una casona colonial.",
                "Allí se conservan oficios únicos como el barniz de Pasto, la talla en madera y el tejido.",
                "Su patio empedrado y su arquitectura de tapia pisada son monumento nacional.",
            ),
            TipoDato.HISTORICO to listOf(
                "La casona del Museo Taminango data de 1623 y es una de las construcciones civiles más antiguas de Pasto.",
                "Fue declarada monumento nacional en 1971 para proteger su arquitectura colonial del siglo XVII.",
                "Sobrevivió a terremotos y guerras civiles manteniendo intacta su estructura original.",
            ),
            TipoDato.COMICO to listOf(
                "Esta casa es de 1623: cuando la estrenaron, nadie preguntó por la clave del wifi.",
                "Lleva cuatro siglos en pie y todavía no ha necesitado una actualización de sistema.",
                "Su cocina funciona con fogón de leña, la única que nunca falla cuando se va la luz.",
            ),
            TipoDato.CURIOSO to listOf(
                "El barniz de Pasto que se exhibe aquí no es pintura: son láminas de resina vegetal llamada mopa-mopa.",
                "Los artesanos calientan y estiran la resina a mano antes de adherirla sobre la madera.",
                "Es una técnica indígena prehispánica declarada patrimonio inmaterial por la UNESCO.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "La cocina antigua del museo conserva su fogón tradicional de leña.",
                "Allí se explica la preparación tradicional del cuy asado y de los lapingachos de papa.",
                "Son dos platos emblemáticos de la gastronomía del altiplano nariñense.",
            ),
        ),
    )

    private val teatroImperial = ParadaDemo(
        idPrincipal = "nominatim:node:616032279",
        nombre = "Teatro Imperial",
        coordenada = Coordenada(1.2146178, -77.2823158),
        fuente = "nominatim",
        relevancia = Relevancia.ETIQUETA_OSM_PATRIMONIAL,
        contexto = "El Teatro Imperial de Pasto fue construido entre 1922 y 1932 con diseño de Belisario Ruiz " +
            "Wilches en estilo clásico europeo con herradura de palcos de madera. Es bien de interés cultural " +
            "nacional y administrado por la Universidad de Nariño; en sus alrededores son tradicionales el " +
            "café de Nariño y los pasteles de queso.",
        frasesPorTipo = mapOf(
            TipoDato.INTERESANTE to listOf(
                "El Teatro Imperial es el principal escenario clásico de Pasto y tiene forma de herradura con tres niveles de palcos.",
                "Hoy lo administra la Universidad de Nariño como centro de conciertos, teatro y cine.",
                "Fue declarado bien de interés cultural de carácter nacional en 1998.",
            ),
            TipoDato.HISTORICO to listOf(
                "El Teatro Imperial se construyó entre 1922 y 1932 bajo el diseño del ingeniero pastuso Belisario Ruiz Wilches.",
                "Se levantó para conmemorar el cuarto centenario de la fundación de la ciudad.",
                "Sus palcos de madera tallada trajeron al sur de Colombia el estilo de los teatros líricos europeos.",
            ),
            TipoDato.COMICO to listOf(
                "En los palcos del Teatro Imperial hasta un susurro suena con eco de ópera.",
                "Tardó diez años en abrir sus puertas, pero desde 1932 nadie le ha quitado el protagonismo.",
                "Es el único lugar del centro donde llegar con abrigo elegante siempre parece parte del libreto.",
            ),
            TipoDato.CURIOSO to listOf(
                "Su sala en forma de herradura fue calculada para que la voz se escuche sin micrófonos desde la galería más alta.",
                "Gran parte de su estructura interior está labrada en maderas nativas de Nariño.",
                "Además de ópera y teatro, en sus primeros años fue la gran sala de cine mudo y sonoro de la ciudad.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "Al salir de función en el Teatro Imperial es tradición tomar café de altura de Nariño en las cafeterías vecinas.",
                "Se acompaña con pasteles calientes de queso y envueltos de maíz.",
                "Es el ritual clásico para conversar sobre la obra en las noches frías del centro.",
            ),
        ),
    )

    private val catedralPasto = ParadaDemo(
        idPrincipal = "wikipedia-es:7182940",
        idsAlternos = listOf("nominatim:way:91859876", "wikipedia-en:52446252"),
        nombre = "Catedral de Pasto",
        coordenada = Coordenada(1.2157273, -77.2796246),
        fuente = "wikipedia-es",
        relevancia = Relevancia.ARTICULO_WIKIPEDIA,
        contexto = "La Catedral de Pasto, consagrada al Sagrado Corazón de Jesús, es la sede de la diócesis de " +
            "Pasto. Su construcción comenzó el 25 de octubre de 1899 bajo el impulso de fray Ezequiel Moreno y " +
            "fue inaugurada en 1920. Es un templo de ladrillo de estilo románico y neogótico con tres naves y " +
            "dos torres. En las cuadras vecinas se prepara el tradicional helado de paila en paila de cobre.",
        frasesPorTipo = mapOf(
            TipoDato.INTERESANTE to listOf(
                "La Catedral de Pasto está consagrada al Sagrado Corazón de Jesús y es la sede episcopal de la diócesis.",
                "Destaca por su imponente fachada de ladrillo a la vista con tres naves y dos altas torres.",
                "Reemplazó al antiguo templo de San Juan Bautista como iglesia matriz de la ciudad.",
            ),
            TipoDato.HISTORICO to listOf(
                "La primera piedra de la Catedral de Pasto se colocó en octubre de 1899 por iniciativa de san Ezequiel Moreno.",
                "Su construcción se extendió durante más de veinte años hasta su estreno en 1920.",
                "El templo se levantó sobre el solar del antiguo convento franciscano destruido en el siglo XIX.",
            ),
            TipoDato.COMICO to listOf(
                "La catedral tardó veintiún años en construirse; cualquier remodelación casera ya parece rápida.",
                "Empezó en el siglo XIX y se inauguró en el XX sin perder ni un ladrillo de entusiasmo.",
                "Quien puso el primer ladrillo en 1899 necesitó mucha paciencia para ver las torres terminadas.",
            ),
            TipoDato.CURIOSO to listOf(
                "A pesar de su aspecto centenario, es más joven que varios templos coloniales que la rodean.",
                "Su impulsor, fray Ezequiel Moreno, fue canonizado décadas después como santo.",
                "Su diseño combina arcos románicos con vitrales europeos traídos a lomo de mula por la cordillera.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "En las calles vecinas a la catedral es tradición probar el helado de paila artesanal.",
                "Se bate a mano con cuchara de madera en una paila de cobre que gira sobre hielo y sal.",
                "Los sabores más pedidos son mora de Castilla, chilacuán y leche con pasas.",
            ),
        ),
    )

    private val sanJuanBautista = ParadaDemo(
        idPrincipal = "wikipedia-es:9856229",
        idsAlternos = listOf("nominatim:way:115215442"),
        nombre = "Templo de San Juan Bautista",
        coordenada = Coordenada(1.2151401, -77.2786570),
        fuente = "wikipedia-es",
        relevancia = Relevancia.ARTICULO_WIKIPEDIA,
        contexto = "El Templo de San Juan Bautista está en un costado de la Plaza de Nariño y fue la primera " +
            "iglesia matriz de Pasto. Se inició en el siglo XVI y el edificio actual data de 1667. Conserva " +
            "retablos barrocos dorados, un púlpito colonial y los restos del caudillo pastuso Agustín Agualongo. " +
            "A la salida es costumbre comprar pan de leche y café nariñense.",
        frasesPorTipo = mapOf(
            TipoDato.INTERESANTE to listOf(
                "El Templo de San Juan Bautista es la iglesia más antigua de Pasto y asoma directo a la Plaza de Nariño.",
                "En su interior conserva retablos coloniales dorados y un techo de tradición mudéjar.",
                "Durante siglos funcionó como la primera iglesia matriz de la ciudad.",
            ),
            TipoDato.HISTORICO to listOf(
                "El templo original se levantó en el siglo XVI y la edificación actual fue reconstruida en 1667.",
                "En su interior reposan los restos del líder militar pastuso Agustín Agualongo.",
                "Fue el corazón religioso de San Juan de Pasto durante toda la época colonial.",
            ),
            TipoDato.COMICO to listOf(
                "Esta iglesia lleva desde el siglo XVII en la misma esquina: eso sí es estabilidad laboral.",
                "Cuando se inauguró en 1667, nadie podía echarle la culpa al semáforo por llegar tarde a misa.",
                "Ha visto pasar tantos siglos frente a la plaza que ya ningún desfile la toma por sorpresa.",
            ),
            TipoDato.CURIOSO to listOf(
                "Su arco toral y su artesonado de madera siguen técnicas mudéjares de herencia hispanoárabe.",
                "Agustín Agualongo, caudillo realista de la independencia, descansa dentro de este templo.",
                "El patrono del templo, San Juan Bautista, le dio el nombre completo a San Juan de Pasto.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "A la salida del templo es costumbre comprar pan de leche recién horneado en las panaderías del centro.",
                "Se acompaña con café suave cultivado en las laderas volcánicas de Nariño.",
                "Es una tradición dominical que reúne a las familias frente a la plaza principal.",
            ),
        ),
    )

    private val plazaNarino = ParadaDemo(
        idPrincipal = "wikipedia-es:9855193",
        idsAlternos = listOf("nominatim:way:1078759141"),
        nombre = "Plaza de Nariño",
        coordenada = Coordenada(1.2146113, -77.2782881),
        fuente = "wikipedia-es",
        relevancia = Relevancia.ARTICULO_WIKIPEDIA,
        contexto = "La Plaza de Nariño es la plaza mayor e histórica de San Juan de Pasto. En su centro se " +
            "encuentra la estatua en bronce de Antonio Nariño, precursor de la independencia y traductor de " +
            "los Derechos del Hombre. Está rodeada por San Juan Bautista y la Gobernación de Nariño, con vista " +
            "al volcán Galeras. En sus alrededores se sirven hervidos de fruta, empanadas de añejo y quimbolitos.",
        frasesPorTipo = mapOf(
            TipoDato.INTERESANTE to listOf(
                "La Plaza de Nariño es el corazón histórico de Pasto y el punto de encuentro por excelencia de la ciudad.",
                "En su centro se alza la estatua de Antonio Nariño, precursor de la independencia colombiana.",
                "En mañanas despejadas se divisa desde aquí la silueta del volcán Galeras.",
            ),
            TipoDato.HISTORICO to listOf(
                "Durante la Colonia fue la Plaza Mayor de la ciudad y escenario de mercados, proclamas y cabildos.",
                "Recibió su nombre actual en 1910 al inaugurarse el monumento a Antonio Nariño en el centenario de la independencia.",
                "A su costado funciona la Gobernación desde donde se administra el departamento de Nariño.",
            ),
            TipoDato.COMICO to listOf(
                "Antonio Nariño lleva más de un siglo de pie en el centro de la plaza y todavía no ha pedido asiento.",
                "Las palomas del centro lo tratan con una confianza que ningún prócer imaginó en los libros de historia.",
                "Si en Pasto alguien dice \"nos vemos en la plaza\", nadie necesita preguntar en cuál.",
            ),
            TipoDato.CURIOSO to listOf(
                "El prócer del monumento en el centro de la plaza es quien le da nombre a todo el departamento.",
                "El volcán Galeras vigila la plaza a solo nueve kilómetros en línea recta desde su cumbre.",
                "Cada enero la plaza se transforma en punto de encuentro del Carnaval de Negros y Blancos.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "En los alrededores de la plaza se toman hervidos: bebida caliente de mora, lulo o maracuyá con licor.",
                "También son famosas las empanadas de añejo hechas con masa de maíz fermentado y ají de maní.",
                "Para acompañar el frío pastuso no faltan los quimbolitos dulces envueltos en hoja de achira.",
            ),
        ),
    )

    private val temploCristoRey = ParadaDemo(
        idPrincipal = "wikipedia-en:51548567",
        idsAlternos = listOf("nominatim:way:91859872"),
        nombre = "Templo de Cristo Rey",
        coordenada = Coordenada(1.2149664, -77.2766597),
        fuente = "wikipedia-en",
        relevancia = Relevancia.ARTICULO_WIKIPEDIA,
        contexto = "El Templo de Cristo Rey y el antiguo edificio del Colegio San Francisco Javier fueron " +
            "levantados por la Compañía de Jesús en el centro de Pasto en la primera mitad del siglo XX. " +
            "Destaca por su fachada monumental de ladrillo, su cúpula coronada por la imagen de Cristo Rey y " +
            "su tradición educativa y cultural; cerca se venden colaciones, dulces de leche y repostería tradicional.",
        frasesPorTipo = mapOf(
            TipoDato.INTERESANTE to listOf(
                "El Templo de Cristo Rey forma un conjunto arquitectónico histórico con el Colegio San Francisco Javier.",
                "Su fachada de ladrillo y su cúpula coronada por la escultura de Cristo Rey dominan la Carrera 24.",
                "Es uno de los templos más representativos de la presencia jesuita en el sur de Colombia.",
            ),
            TipoDato.HISTORICO to listOf(
                "Fue construido por la Compañía de Jesús en la primera mitad del siglo XX junto a su histórico colegio.",
                "El conjunto fue declarado bien de interés cultural por su valor arquitectónico y educativo.",
                "Por sus aulas vecinas pasaron generaciones de escritores, científicos y artistas nariñenses.",
            ),
            TipoDato.COMICO to listOf(
                "Con el colegio justo al lado, aquí las campanas avisaban tanto la hora de rezar como la del examen.",
                "Más de un estudiante cruzó la puerta del templo pidiendo un milagro de última hora en matemáticas.",
                "La estatua de la cúpula tiene la mejor vista panorámica del centro sin pagar terraza.",
            ),
            TipoDato.CURIOSO to listOf(
                "En la cadena de búsqueda su registro internacional aparece en la Wikipedia en inglés por el colegio jesuita.",
                "Sus muros de ladrillo macizo fueron diseñados para resistir la actividad sísmica del volcán Galeras.",
                "El interior guarda tallas de madera elaboradas por maestros ebanistas pastusos.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "En las calles cercanas al templo se consiguen colaciones y dulces tradicionales de pailita.",
                "También son populares las obleas con dulce de mora y cuajada fresca.",
                "Es una parada clásica de media tarde para los caminantes del centro histórico.",
            ),
        ),
    )

    private val museoDelOro = ParadaDemo(
        idPrincipal = "nominatim:node:1999514293",
        idsAlternos = listOf("wikipedia-es:7783675"),
        nombre = "Museo del Oro Nariño",
        coordenada = Coordenada(1.2116162, -77.2766887),
        fuente = "nominatim",
        relevancia = Relevancia.ETIQUETA_OSM_PATRIMONIAL,
        contexto = "El Museo del Oro Nariño funciona en el Centro Cultural Leopoldo López Álvarez del Banco de " +
            "la República en Pasto, inaugurado en 1984 y renovado en 2016. Exhibe más de 400 piezas prehispánicas " +
            "de oro, cerámica, hueso y concha de los pueblos Capulí, Piartal y Tuza, como discos giratorios y " +
            "figuras de coqueros, además de vasijas rituales para chicha de maíz.",
        frasesPorTipo = mapOf(
            TipoDato.INTERESANTE to listOf(
                "El Museo del Oro Nariño resguarda más de cuatrocientas piezas prehispánicas en el centro cultural del Banco de la República.",
                "Su colección muestra el arte en oro, cerámica y concha de los antiguos pueblos de los Andes del sur.",
                "La entrada es gratuita y el centro cuenta además con biblioteca y jardines.",
            ),
            TipoDato.HISTORICO to listOf(
                "El museo fue inaugurado en 1984 y renovó por completo su guion curatorial en diciembre de 2016.",
                "Sus piezas pertenecen a las sociedades prehispánicas Capulí, Piartal y Tuza que habitaron Nariño.",
                "Muestra el intercambio comercial que conectaba los Andes, el Pacífico y la Amazonía hace siglos.",
            ),
            TipoDato.COMICO to listOf(
                "Aquí hay oro prehispánico por todas partes y aun así la entrada es totalmente gratis.",
                "Las estatuillas de los coqueros llevan más de mil años con la mejilla inflada y sin cansarse.",
                "Eso sí, los discos de oro giran solo en la exhibición: no intentes usarlos de vinilo.",
            ),
            TipoDato.CURIOSO to listOf(
                "Entre sus joyas destacan los discos de oro martillado hechos para girar colgados y reflejar el sol.",
                "Las esculturas llamadas coqueros retratan a caciques mascando hoja de coca con un bulto en la mejilla.",
                "Algunas piezas combinan oro con platino y cobre mediante técnicas metalúrgicas milenarias.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "Las vasijas globulares del museo servían en la antigüedad para fermentar y servir chicha de maíz.",
                "También se conservan platos cerámicos decorados donde se ofrecían tubérculos andinos y maíz tostado.",
                "La exposición revela cómo la cocina ceremonial unía a las comunidades del altiplano.",
            ),
        ),
    )

    private val plazaDelCarnaval = ParadaDemo(
        idPrincipal = "nominatim:way:567219502",
        idsAlternos = listOf("wikipedia-es:1410915"),
        nombre = "Plaza del Carnaval",
        coordenada = Coordenada(1.2105614, -77.2765537),
        fuente = "nominatim",
        relevancia = Relevancia.ETIQUETA_OSM_PATRIMONIAL,
        contexto = "La Plaza del Carnaval y la Cultura es una amplia explanada pública en el centro de Pasto y " +
            "escenario principal del Carnaval de Negros y Blancos, declarado Patrimonio Cultural Inmaterial de " +
            "la Humanidad por la UNESCO en 2009. El 5 de enero se celebra el Día de Negros y el 6 de enero el " +
            "Día de Blancos con el Desfile Magno de carrozas monumentales. En sus fiestas es tradicional el frito pastuso.",
        frasesPorTipo = mapOf(
            TipoDato.INTERESANTE to listOf(
                "La Plaza del Carnaval y la Cultura es la gran explanada donde culmina la Senda del Carnaval en Pasto.",
                "Es el escenario principal del Carnaval de Negros y Blancos cada comienzo de enero.",
                "Por aquí desfilan las carrozas gigantes elaboradas por los maestros artesanos nariñenses.",
            ),
            TipoDato.HISTORICO to listOf(
                "El Carnaval de Negros y Blancos que tiene su epicentro aquí fue declarado Patrimonio de la Humanidad por la UNESCO en 2009.",
                "La plaza se construyó a comienzos del siglo XXI como espacio cultural y urbano para la fiesta.",
                "Cada 5 de enero celebra el juego de negritos y cada 6 de enero el Día de Blancos con el Desfile Magno.",
            ),
            TipoDato.COMICO to listOf(
                "El 6 de enero en esta plaza nadie se salva del talco: venir vestido de negro es deporte extremo.",
                "Es el único lugar donde terminar cubierto de espuma de pies a cabeza significa que hiciste amigos.",
                "Si alguien te dice \"que viva Pasto, carajo\", aquí es obligatorio responder con el mismo entusiasmo.",
            ),
            TipoDato.CURIOSO to listOf(
                "Las carrozas que llegan a la plaza alcanzan varios metros de altura y tienen figuras con movimiento mecánico.",
                "Los artesanos trabajan durante meses en talleres familiares modelando papel maché, icopor y resina.",
                "El resto del año la explanada sirve como punto de encuentro cultural y recreativo del centro.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "Durante los días de fiesta alrededor de la plaza el plato rey es el frito pastuso.",
                "Lleva carne de cerdo dorada acompañada de papa criolla, crispetas, mote y ají de maní.",
                "Es la comida energética ideal para aguantar horas de música andina y baile en la senda.",
            ),
        ),
    )

    val paradas: List<ParadaDemo> = listOf(
        tiendaBarrio,
        museoTaminango,
        teatroImperial,
        catedralPasto,
        sanJuanBautista,
        plazaNarino,
        temploCristoRey,
        museoDelOro,
        plazaDelCarnaval,
    )

    /** Precarga en [cache] todas las combinaciones de lugar, tipo e intensidad de la ruta demo. */
    fun precargarEn(cache: CacheDatos) {
        val lote = mutableMapOf<String, String>()
        for (parada in paradas) {
            if (!parada.relevancia.emblematico) continue
            for (tipo in TipoDato.entries) {
                val frases = parada.frasesPorTipo[tipo] ?: continue
                for (intensidad in Intensidad.entries) {
                    val cantidad = when (intensidad) {
                        Intensidad.SUAVE -> 1
                        Intensidad.NORMAL -> 2
                        Intensidad.INTENSO -> 3
                    }
                    val texto = frases.take(cantidad).joinToString(" ")
                    for (id in parada.todosLosIds) {
                        lote["$id|${tipo.name}|${intensidad.name}"] = texto
                    }
                }
            }
        }
        cache.guardarLote(lote)
    }
}
