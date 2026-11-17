package com.daxlejo.datoscerca.data.fake

import com.daxlejo.datoscerca.domain.modelo.Coordenada
import com.daxlejo.datoscerca.domain.modelo.TipoDato
import kotlin.math.ceil

/** Lugar de muestra: su contexto y tres frases por tipo, de la más esencial a la más detallada. */
data class LugarMuestra(
    val id: String,
    val nombre: String,
    val coordenada: Coordenada,
    val contexto: String,
    val frases: Map<TipoDato, List<String>>,
)

/**
 * Datos de muestra del centro de Pasto (Nariño, Colombia) para la Sesión 1.
 * Las coordenadas son aproximadas; la Sesión 2 usa fuentes reales.
 */
object DatosPasto {
    const val FUENTE = "muestra"

    // Punto de partida a unos 200 m de la Plaza de Nariño, sin ningún lugar dentro del radio de llegada.
    val inicio = Coordenada(1.21640, -77.27690)

    private val plazaNarino = LugarMuestra(
        id = "muestra:plaza-narino",
        nombre = "Plaza de Nariño",
        coordenada = Coordenada(1.21464, -77.27825),
        contexto = "La Plaza de Nariño es la plaza principal de San Juan de Pasto, capital del departamento " +
            "de Nariño, Colombia. Está en el centro histórico y en su centro se levanta una estatua de Antonio " +
            "Nariño, precursor de la independencia y traductor al español de los Derechos del Hombre. A su " +
            "alrededor están la iglesia de San Juan Bautista y la Gobernación de Nariño. Es el punto de encuentro " +
            "tradicional de la ciudad y uno de los escenarios del Carnaval de Negros y Blancos. En días despejados " +
            "se ve desde allí el volcán Galeras. En los alrededores se venden hervidos (bebida caliente de fruta " +
            "con aguardiente), empanadas de añejo hechas con maíz fermentado y quimbolitos.",
        frases = mapOf(
            TipoDato.INTERESANTE to listOf(
                "La Plaza de Nariño es la plaza principal de Pasto y el punto de encuentro de toda la ciudad.",
                "En su centro se levanta la estatua de Antonio Nariño, precursor de la independencia.",
                "En días despejados, desde aquí se alcanza a ver el volcán Galeras.",
            ),
            TipoDato.HISTORICO to listOf(
                "La plaza honra a Antonio Nariño, quien tradujo al español los Derechos del Hombre.",
                "A un costado está San Juan Bautista, el templo que acompaña a la plaza desde la Colonia.",
                "Frente a ella funciona la Gobernación, así que aquí se sigue decidiendo la vida del departamento.",
            ),
            TipoDato.COMICO to listOf(
                "Antonio Nariño lleva años de pie en el centro de la plaza y todavía no ha pedido silla.",
                "Las palomas lo tratan con una confianza que ningún prócer se merece.",
                "Si en Pasto alguien te dice \"nos vemos en la plaza\", nadie pregunta en cuál.",
            ),
            TipoDato.CURIOSO to listOf(
                "Cuando el cielo se despeja, el volcán Galeras se asoma a la plaza como un vecino más.",
                "El hombre de la estatua es el mismo que le da nombre a todo el departamento.",
                "Cada enero la plaza cambia de oficio y se vuelve escenario del Carnaval de Negros y Blancos.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "Cerca de la plaza se toman hervidos: una bebida caliente de fruta con aguardiente.",
                "También se consiguen empanadas de añejo, hechas con masa de maíz fermentado.",
                "Para cerrar, los quimbolitos son la compañía dulce de la tarde pastusa.",
            ),
        ),
    )

    private val sanJuanBautista = LugarMuestra(
        id = "muestra:san-juan-bautista",
        nombre = "Iglesia de San Juan Bautista",
        coordenada = Coordenada(1.21512, -77.27788),
        contexto = "La iglesia de San Juan Bautista está en un costado de la Plaza de Nariño y es el templo más " +
            "antiguo de Pasto. La primera iglesia del lugar se levantó en el siglo XVI y el edificio actual es del " +
            "siglo XVII. Durante siglos fue la iglesia matriz de la ciudad. Su interior conserva retablos dorados " +
            "y un techo de madera de tradición mudéjar. Allí reposan los restos de Agustín Agualongo, caudillo " +
            "pastuso de la época de la independencia. A la salida de misa es costumbre comprar pan de leche y " +
            "tomar café de Nariño.",
        frases = mapOf(
            TipoDato.INTERESANTE to listOf(
                "San Juan Bautista es el templo más antiguo de Pasto y da directamente a la Plaza de Nariño.",
                "Por dentro conserva retablos dorados y un techo de madera de tradición mudéjar.",
                "Durante siglos fue la iglesia matriz, la más importante de la ciudad.",
            ),
            TipoDato.HISTORICO to listOf(
                "La primera iglesia de este lugar se levantó en el siglo XVI; la actual es del siglo XVII.",
                "Aquí reposan los restos de Agustín Agualongo, caudillo pastuso de la independencia.",
                "Fue la iglesia matriz de Pasto durante siglos, antes de que existiera la catedral actual.",
            ),
            TipoDato.COMICO to listOf(
                "Esta iglesia ha visto pasar tantos siglos que ya nada en la plaza la sorprende.",
                "Cuando se construyó, llegar tarde a misa todavía no se podía culpar al tráfico.",
                "Lleva desde el siglo XVII en la misma esquina: eso sí es estabilidad laboral.",
            ),
            TipoDato.CURIOSO to listOf(
                "El techo de madera del templo sigue una tradición mudéjar, de raíz árabe y española.",
                "Agustín Agualongo, que peleó del lado del rey, descansa hoy dentro de la iglesia.",
                "Aunque parece una sola obra, el edificio actual reemplazó a uno un siglo más viejo.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "A la salida de misa, la costumbre es comprar pan de leche recién hecho.",
                "Se acompaña con café de Nariño, cultivado en las montañas del departamento.",
                "Es un plan sencillo que muchas familias repiten cada domingo frente a la plaza.",
            ),
        ),
    )

    private val catedral = LugarMuestra(
        id = "muestra:catedral",
        nombre = "Catedral de Pasto",
        coordenada = Coordenada(1.21363, -77.28106),
        contexto = "La Catedral de Pasto está dedicada al Sagrado Corazón de Jesús y es la sede de la diócesis " +
            "de Pasto. Su construcción empezó en 1899 y terminó en 1920. Es un edificio de ladrillo de estilo " +
            "románico, con tres naves y dos torres. Reemplazó a San Juan Bautista como iglesia principal de la " +
            "ciudad. En las calles vecinas hay heladerías que preparan helado de paila, batido a mano sobre una " +
            "paila de cobre con hielo.",
        frases = mapOf(
            TipoDato.INTERESANTE to listOf(
                "La Catedral de Pasto está dedicada al Sagrado Corazón de Jesús y es la sede de la diócesis.",
                "Es un edificio de ladrillo de estilo románico, con tres naves y dos torres.",
                "Tomó el lugar de San Juan Bautista como iglesia principal de la ciudad.",
            ),
            TipoDato.HISTORICO to listOf(
                "La construcción de la catedral empezó en 1899 y terminó en 1920.",
                "Fueron más de veinte años de obra para levantar sus tres naves de ladrillo.",
                "Al terminarse, relevó a San Juan Bautista, que había sido la iglesia principal por siglos.",
            ),
            TipoDato.COMICO to listOf(
                "La catedral tardó veintiún años en construirse; tu remodelación ya no parece tan lenta.",
                "Empezó en un siglo y se terminó en otro, sin que nadie cambiara los planos.",
                "Quien puso el primer ladrillo en 1899 necesitó mucha fe para imaginar el último.",
            ),
            TipoDato.CURIOSO to listOf(
                "La obra arrancó en el siglo XIX y se inauguró ya entrado el siglo XX.",
                "A pesar de su aire antiguo, es más joven que la iglesia a la que reemplazó.",
                "Su estilo románico, de arcos redondos, es poco común en templos de ladrillo de la región.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "Cerca de la catedral se prepara helado de paila, batido a mano sobre una paila de cobre.",
                "La paila gira sobre hielo mientras la fruta se va cuajando a punta de cuchara.",
                "Es el postre clásico para después de recorrer el centro.",
            ),
        ),
    )

    private val museoTaminango = LugarMuestra(
        id = "muestra:museo-taminango",
        nombre = "Museo Taminango",
        coordenada = Coordenada(1.21690, -77.28230),
        contexto = "El Museo Taminango de Artes y Tradiciones Populares de Nariño funciona en una casona " +
            "colonial de 1623, una de las más antiguas de Pasto, declarada monumento nacional. Muestra oficios " +
            "tradicionales de la región como el barniz de Pasto o mopa-mopa, una técnica que decora madera con " +
            "láminas de resina vegetal, además de la talla en madera y los tejidos. La casa conserva un patio " +
            "central y una cocina antigua con fogón de leña, donde se explica la preparación del cuy asado y de " +
            "los lapingachos, tortillas de papa.",
        frases = mapOf(
            TipoDato.INTERESANTE to listOf(
                "El Museo Taminango reúne las artes y tradiciones populares de Nariño en una casona colonial.",
                "Allí se ven oficios como el barniz de Pasto, la talla en madera y los tejidos.",
                "La casa conserva su patio central y es monumento nacional.",
            ),
            TipoDato.HISTORICO to listOf(
                "La casona del Museo Taminango es de 1623, una de las más antiguas de Pasto.",
                "Fue declarada monumento nacional y hoy guarda oficios tradicionales de la región.",
                "Ha estado en pie casi cuatro siglos, desde plena época colonial.",
            ),
            TipoDato.COMICO to listOf(
                "Esta casa es de 1623: cuando la estrenaron, nadie preguntó por la clave del wifi.",
                "Lleva cuatro siglos en pie y todavía no ha necesitado una actualización.",
                "Su cocina funciona con fogón de leña, la única que no se apaga cuando se va la luz.",
            ),
            TipoDato.CURIOSO to listOf(
                "El barniz de Pasto no es pintura: son láminas de resina vegetal llamada mopa-mopa.",
                "Las láminas se estiran a mano y se pegan sobre la madera para formar los dibujos.",
                "En el museo se puede ver el proceso junto a piezas de talla y tejidos.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "La cocina antigua del museo conserva su fogón de leña.",
                "Allí se explica cómo se preparan el cuy asado y los lapingachos, tortillas de papa.",
                "Son dos platos centrales de la mesa nariñense.",
            ),
        ),
    )

    private val museoDelOro = LugarMuestra(
        id = "muestra:museo-del-oro",
        nombre = "Museo del Oro Nariño",
        coordenada = Coordenada(1.21290, -77.27640),
        contexto = "El Museo del Oro Nariño pertenece al Banco de la República y funciona en su centro cultural " +
            "del centro de Pasto. Exhibe orfebrería y cerámica prehispánica de las culturas del altiplano " +
            "nariñense: Capulí, Piartal y Tuza. Entre sus piezas hay discos giratorios de oro y figuras de " +
            "personas mascando hoja de coca, llamadas coqueros. La entrada es gratuita. El centro cultural tiene " +
            "además biblioteca y sala de música. Las vasijas expuestas servían para guardar chicha, bebida " +
            "fermentada de maíz.",
        frases = mapOf(
            TipoDato.INTERESANTE to listOf(
                "El Museo del Oro Nariño exhibe orfebrería y cerámica de las culturas prehispánicas de la región.",
                "Pertenece al Banco de la República y la entrada es gratuita.",
                "En el mismo centro cultural hay biblioteca y sala de música.",
            ),
            TipoDato.HISTORICO to listOf(
                "Las piezas del museo vienen de las culturas Capulí, Piartal y Tuza del altiplano nariñense.",
                "Son pueblos que trabajaron el oro y la cerámica mucho antes de la llegada de los españoles.",
                "El Banco de la República las conserva y las muestra en su centro cultural de Pasto.",
            ),
            TipoDato.COMICO to listOf(
                "Aquí hay oro de sobra y aun así la entrada es gratis: el mejor negocio del centro.",
                "Los coqueros de cerámica llevan siglos mascando hoja y siguen igual de serenos.",
                "Eso sí, los discos de oro giran solo en la vitrina: no es una pista de baile.",
            ),
            TipoDato.CURIOSO to listOf(
                "Entre las piezas hay discos de oro hechos para girar y jugar con la luz.",
                "También hay figuras llamadas coqueros, que muestran personas mascando hoja de coca.",
                "Se reconoce el gesto por el pequeño bulto que llevan en la mejilla.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "Varias vasijas del museo servían para guardar chicha, bebida fermentada de maíz.",
                "Los coqueros de cerámica muestran otra costumbre antigua: mascar hoja de coca.",
                "Así, entre oro y barro, el museo también cuenta qué se comía y se bebía en el altiplano.",
            ),
        ),
    )

    private val plazaDelCarnaval = LugarMuestra(
        id = "muestra:plaza-del-carnaval",
        nombre = "Plaza del Carnaval",
        coordenada = Coordenada(1.21075, -77.27560),
        contexto = "La Plaza del Carnaval es una explanada del centro de Pasto y el escenario principal del " +
            "Carnaval de Negros y Blancos, que se celebra los primeros días de enero. El carnaval fue declarado " +
            "Patrimonio Cultural Inmaterial de la Humanidad por la UNESCO en 2009. El 5 de enero es el Día de " +
            "Negros, cuando la gente se pinta la cara de negro, y el 6 de enero es el Día de Blancos, con talco, " +
            "espuma y el Desfile Magno de carrozas gigantes hechas por artesanos. Durante la fiesta se come " +
            "frito pastuso, cerdo frito servido con papa y maíz tostado.",
        frases = mapOf(
            TipoDato.INTERESANTE to listOf(
                "La Plaza del Carnaval es el escenario principal del Carnaval de Negros y Blancos.",
                "La fiesta se celebra los primeros días de enero y llena de gente todo el centro de Pasto.",
                "Por aquí pasa el Desfile Magno, con carrozas gigantes hechas por artesanos.",
            ),
            TipoDato.HISTORICO to listOf(
                "En 2009 la UNESCO declaró el Carnaval de Negros y Blancos patrimonio inmaterial de la humanidad.",
                "El 5 de enero es el Día de Negros y el 6 de enero el Día de Blancos.",
                "Esta plaza es hoy el punto central de esa celebración.",
            ),
            TipoDato.COMICO to listOf(
                "El 6 de enero aquí nadie se salva del talco: venir de negro es una decisión valiente.",
                "Es la única fecha en que salir blanco de pies a cabeza cuenta como ir bien vestido.",
                "Y si alguien te echa espuma, tranquilo: en carnaval eso es un saludo.",
            ),
            TipoDato.CURIOSO to listOf(
                "En el Día de Negros la gente se pinta la cara; al día siguiente todo se cubre de talco y espuma.",
                "Las carrozas del Desfile Magno son esculturas gigantes hechas a mano por artesanos.",
                "El resto del año, la plaza es una explanada tranquila en pleno centro.",
            ),
            TipoDato.GASTRONOMICO to listOf(
                "En carnaval se come frito pastuso: cerdo frito servido con papa y maíz tostado.",
                "Es comida de fiesta, pensada para compartir de pie entre desfile y desfile.",
                "Los puestos se instalan alrededor de la plaza durante los primeros días de enero.",
            ),
        ),
    )

    val lugares: List<LugarMuestra> =
        listOf(plazaNarino, sanJuanBautista, catedral, museoTaminango, museoDelOro, plazaDelCarnaval)

    /** Caminata en bucle que pasa por todos los lugares, con un punto cada [pasoMetros]. */
    fun ruta(pasoMetros: Double = 40.0): List<Coordenada> {
        val paradas = listOf(inicio) + lugares.map(LugarMuestra::coordenada) + inicio
        return paradas.zipWithNext().flatMap { (desde, hasta) -> tramo(desde, hasta, pasoMetros) }
    }

    // Interpola entre dos paradas; incluye el punto de partida y no el de llegada.
    private fun tramo(desde: Coordenada, hasta: Coordenada, pasoMetros: Double): List<Coordenada> {
        val pasos = ceil(desde.distanciaMetrosA(hasta) / pasoMetros).toInt().coerceAtLeast(1)
        return (0 until pasos).map { indice ->
            val avance = indice.toDouble() / pasos
            Coordenada(
                latitud = desde.latitud + (hasta.latitud - desde.latitud) * avance,
                longitud = desde.longitud + (hasta.longitud - desde.longitud) * avance,
            )
        }
    }
}
