package com.example.vacacioneseva

// Importación de librerías y paquetes
import android.bluetooth.BluetoothHidDevice
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.ImageProcessor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import android.preference.PreferenceManager
import android.telecom.Call
import android.transition.Transition
import android.util.Log
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.api.IMapController
import org.osmdroid.events.MapAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberImagePainter
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import com.google.android.gms.maps.MapView
import retrofit2.http.GET
import java.io.File

// Enumeración para representar las pantallas de la aplicación
enum class Pantalla {
    ListaLugares,
    DetallesLugar,
    Mapa
}

// ViewModel de la aplicación
class AppVM : ViewModel() {
    // Lista mutable de lugares
    val lugares = mutableStateListOf<Lugar>()

    // Estado para la pantalla actual
    var pantallaActual = mutableStateOf(Pantalla.ListaLugares)

    // Lugar seleccionado
    val lugarSeleccionado = mutableStateOf<Lugar?>(null)

    // Valor del dólar
    var valorDolar = mutableStateOf(0.0)

    // Inicialización, por ejemplo, para obtener el valor del dólar
    init {
        obtenerValorDolar()
    }

    // Método para agregar un lugar a la lista
    fun agregarLugar(lugar: Lugar) {
        lugares.add(lugar)
    }

    // Método para actualizar un lugar en la lista
    fun actualizarLugar(lugarActualizado: Lugar) {
        val index = lugares.indexOfFirst { it.nombre == lugarActualizado.nombre }
        if (index != -1) {
            lugares[index] = lugarActualizado
        }
    }
}

// Método para obtener el valor del dólar (debe ser implementado)
private fun obtenerValorDolar() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://mindicador.cl/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service = retrofit.create(MindicadorService::class.java)
    service.getIndicadoresEconomicos().enqueue(object : Callback<IndicadoresResponse> {
        override fun onResponse(
            call: android.telecom.Call<IndicadoresResponse>,
            response: Response<IndicadoresResponse>
        ) {
            response.body()?.let {
                // Actualizar el valor del dólar en el ViewModel
                valorDolar.value = it.dolar.valor
            }
        }

        override fun onFailure(call: android.telecom.Call<IndicadoresResponse>, t: Throwable) {
            // Manejar error, por ejemplo, mostrar un mensaje de error
            Log.e("AppVM", "Error al obtener el valor del dólar", t)
        }
    })
}

// Clase para representar la respuesta de la API de indicadores económicos
data class IndicadoresResponse(val dolar: Indicador)

// Clase para representar un indicador económico (en este caso, solo el valor del dólar)
data class Indicador(val valor: Double)

// Interfaz Retrofit para la API de indicadores económicos
interface MindicadorService {
    @GET("api")
    fun getIndicadoresEconomicos(): android.telecom.Call<IndicadoresResponse>
}

// Clase para representar un lugar
data class Lugar(
    val nombre: String,
    val costoAlojamientoCLP: Double,
    val comentarios: String,
    val fotos: List<Uri>,
    val imagenReferencia: Uri,
    val latitud: Double,
    val longitud: Double,
    val ordenVisita: Int
)

// Actividad principal de la aplicación
class MainActivity : ComponentActivity() {
    private val appVM: AppVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppPlanificadorVacaciones()
        }
    }
}

// Composable principal de la aplicación
@Composable
fun AppPlanificadorVacaciones() {
    val appVM: AppVM = viewModel()
    when (appVM.pantallaActual.value) {
        Pantalla.ListaLugares -> PantallaListaLugares()
        Pantalla.DetallesLugar -> PantallaDetallesLugar()
        Pantalla.Mapa -> PantallaMapa()
    }
}

// Composable para la pantalla de lista de lugares
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaLugares() {
    val appVM: AppVM = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            items(appVM.lugares) { lugar ->
                LugarCard(lugar = lugar) {
                    appVM.lugarSeleccionado.value = lugar
                    appVM.pantallaActual.value = Pantalla.DetallesLugar
                }
            }
        }

        FloatingActionButton(
            onClick = {
                appVM.pantallaActual.value = Pantalla.DetallesLugar
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetallesLugar() {
    val appVM: AppVM = viewModel()
    val lugar = appVM.lugarSeleccionado.value

    // Variables para almacenar datos del lugar
    var nombre by remember { mutableStateOf(lugar?.nombre ?: "") }
    var ordenVisita by remember { mutableStateOf(lugar?.ordenVisita?.toString() ?: "") }
    var comentarios by remember { mutableStateOf(lugar?.comentarios ?: "") }
    var latitud by remember { mutableStateOf(lugar?.latitud?.toString() ?: "") }
    var longitud by remember { mutableStateOf(lugar?.longitud?.toString() ?: "") }
    var imagenReferenciaUrl by remember { mutableStateOf(lugar?.imagenReferencia?.toString() ?: "") }
    var precioAlojamiento by remember { mutableStateOf(lugar?.costoAlojamientoCLP?.toString() ?: "") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = "Detalles del lugar",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(8.dp)
        )

        // Campo de entrada para el nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Entrada de coordenadas (latitud y longitud) y botón para ver en el mapa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = latitud,
                onValueChange = { latitud = it },
                label = { Text("Latitud") },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = longitud,
                onValueChange = { longitud = it },
                label = { Text("Longitud") },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Botón para ver en el mapa
            IconButton(
                onClick = {
                    if (latitud.isNotBlank() && longitud.isNotBlank()) {
                        val lugarTemporal = Lugar(
                            nombre = "",
                            imagenReferencia = Uri.EMPTY,
                            latitud = latitud.toDouble(),
                            longitud = longitud.toDouble(),
                            ordenVisita = 0,
                            costoAlojamientoCLP = 0.0,
                            comentarios = "",
                            fotos = emptyList()
                        )
                        appVM.lugares.add(0, lugarTemporal)
                        appVM.pantallaActual.value = Pantalla.Mapa
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ver en el mapa",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para el orden de visita
        OutlinedTextField(
            value = ordenVisita,
            onValueChange = { ordenVisita = it },
            label = { Text("Orden de visita") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para la URL de la imagen de referencia
        OutlinedTextField(
            value = imagenReferenciaUrl,
            onValueChange = { imagenReferenciaUrl = it },
            label = { Text("Imagen Referencia URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para el precio de alojamiento en CLP
        OutlinedTextField(
            value = precioAlojamiento,
            onValueChange = { precioAlojamiento = it },
            label = { Text("Precio Alojamiento (CLP)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Usa Coil para mostrar la imagen de referencia
        if (imagenReferenciaUrl.isNotBlank()) {
            Image(
                painter = rememberImagePainter(imagenReferenciaUrl),
                contentDescription = null,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de entrada para los comentarios
        OutlinedTextField(
            value = comentarios,
            onValueChange = { comentarios = it },
            label = { Text("Comentarios") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para guardar los cambios
        Button(
            onClick = {
                // Crear un nuevo objeto Lugar con la información proporcionada
                val nuevoLugar = Lugar(
                    nombre = nombre,
                    ordenVisita = ordenVisita.toIntOrNull() ?: 0,
                    imagenReferencia = Uri.parse(imagenReferenciaUrl),
                    latitud = latitud.toDoubleOrNull() ?: 0.0,
                    longitud = longitud.toDoubleOrNull() ?: 0.0,
                    costoAlojamientoCLP = precioAlojamiento.toDoubleOrNull() ?: 0.0,
                    comentarios = comentarios,
                    fotos = listOf(Uri.parse(imagenReferenciaUrl))
                )
                if (lugar != null) {
                    appVM.actualizarLugar(nuevoLugar)
                } else {
                    appVM.agregarLugar(nuevoLugar)
                }

                // Volver a la pantalla anterior
                appVM.pantallaActual.value = Pantalla.ListaLugares
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Guardar")
        }
    }
}

@Composable
fun AndroidMapView(
    modifier: Modifier = Modifier,
    onMapViewCreated: (com.google.android.gms.maps.MapView) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = {
            com.google.android.gms.maps.MapView(it).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setBuiltInZoomControls(true)
                setMultiTouchControls(true)
                onMapViewCreated(this)
            }
        }
    )
}

@Composable
fun PantallaMapa() {
    val appVM: AppVM = viewModel()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidMapView(modifier = Modifier.fillMaxSize()) { mapView ->
            val ultimoLugar = appVM.lugares.firstOrNull()
            if (ultimoLugar != null) {
                val geoPoint = GeoPoint(ultimoLugar.latitud, ultimoLugar.longitud)
                mapView.controller.setCenter(geoPoint)
                val marker = Marker(mapView)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }

        // Botón de volver en la esquina superior izquierda
        IconButton(
            onClick = {
                appVM.pantallaActual.value = Pantalla.DetallesLugar
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
        }
    }
}
