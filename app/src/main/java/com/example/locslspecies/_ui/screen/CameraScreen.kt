package com.example.locslspecies._ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.locslspecies.controller.AuthViewModel
import com.example.locslspecies.helper.requestPermission
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects
import kotlin.math.log


// Supprime les avertissements de lint concernant l'indentation qui peut sembler suspecte.
@SuppressLint("SuspiciousIndentation")
@Composable
fun CameraScreen(navBackStackEntry: NavBackStackEntry, navController: NavHostController) {
    lateinit var generativeModel: GenerativeModel
    // ViewModel pour l'authentification.
    val viewModel: AuthViewModel = viewModel()
    // Liste d'étiquettes d'image à conserver en mémoire.
    val imageLabels = remember { mutableStateListOf<String>() }
    var responses = remember { mutableStateOf("                               reconnaissance...") }

    // Variable pour l'image à traiter, déclarée mais non initialisée ici.
    val image: InputImage
    // Contexte local pour accéder aux ressources et au système de fichiers.
    val context = LocalContext.current
    // Création d'un fichier image qui sera utilisé pour enregistrer l'image de la caméra.
    val file = context.createImageFile()
    // Récupération de l'URI correspondant au fichier créé.
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        "com.example.locslspecies" + ".provider", file
    )
    //val inputContent = remember { mutableStateListOf<String>() }
    // URI de l'image capturée, initialisée à vide et conservée en état.
    var capturedImageUri by remember {
        mutableStateOf<Uri>(Uri.EMPTY)
    }



            generativeModel = GenerativeModel(
            // For text-and-images input (multimodal), use the gemini-pro-vision model
            modelName = "gemini-pro-vision",
            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
            apiKey = "AIzaSyCZfpL4tecZV4z-1-8SPkJ5M-cdSqLLV10"
        )
        val uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, throwable ->


        }

        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)


    // Lanceur d'activité pour prendre une photo avec l'appareil photo.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        // Si la photo a été prise avec succès, mettre à jour l'URI capturé.
        if (it) capturedImageUri = uri
    }
    // Lanceur de permissions pour demander les permissions nécessaires avant de prendre une photo.
    val permissionsLauncher = requestPermission(context, uri, cameraLauncher = cameraLauncher)

    // Disposition en colonne pour l'écran de la caméra.
    Column(
        Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Ligne d'en-tête pour le titre de l'écran de la caméra.
        Row(
            Modifier
                .background(Color(0xFF3B808B))
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
        ) {
            // Texte affichant "Camera".
            Text(
                text = "Camera",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        // Espaceur pour séparer les éléments de l'interface utilisateur.
        Spacer(modifier = Modifier.height(500.dp))
        // Ligne contenant le bouton pour prendre une photo.
        Row(Modifier.weight(1f)) {
            // Bouton pour déclencher la prise de photo.
            Button(onClick = {
                // Lancement de la demande de permissions.
                permissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }, colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B808B),
                contentColor = Color.White)) {
                Text(text = "Prendre une photo")
            }
        }
    }

    // Condition pour vérifier si une image a été capturée et afficher l'aperçu.
    if (capturedImageUri.path?.isNotEmpty() == true) {


        // Colonne pour l'image capturée et les étiquettes d'image.
        Column {
            // Affichage de l'image capturée.
            Image(
                modifier = Modifier
                    .padding(90.dp, 80.dp)
                    .height(175.dp),
                painter = rememberAsyncImagePainter(capturedImageUri),
                contentDescription = null
            )
            // Colonne pour les étiquettes d'image.
            Column(

                verticalArrangement = Arrangement.Center) {
                // Affichage des étiquettes d'image.
             /*   imageLabels.forEach { label ->
                    Text(text = label, color = Color.Black)
                }*/

                Spacer(modifier = Modifier.height(5.dp))
                Row {
                    Text(text = responses.value, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                }

            }
        }

        LaunchedEffect(Unit){
            try {

                val inputContent = content {
                    val imageStream = context.contentResolver.openInputStream(capturedImageUri)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // Scale down by a factor of 2
                    }
                    val bitmap = BitmapFactory.decodeStream(imageStream, null, options)
                    if (bitmap != null) {
                        image(bitmap)
                    }
                    text("tell me the name of this plant or animal and a very little description about it no more than three phrase in french")
                }

                val response = generativeModel.generateContent(inputContent)
                responses.value = response.text.toString()

                Log.d("MYTAG", "CameraScreen: " + responses.value)
                print(response.text)

                viewModel.insertUserPicture(capturedImageUri)

            }
              catch (e: IOException) {

                    e.printStackTrace()
                }
        }

/*        // Bloc try-catch pour le traitement de l'image et l'obtention des étiquettes.
        try {
            // Création d'un objet InputImage à partir de l'URI de l'image capturée.
            image = InputImage.fromFilePath(context, capturedImageUri)
            // Initialisation du labeler d'image.
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            // Traitement de l'image pour l'extraction des étiquettes.
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    // En cas de succès, récupération de la première étiquette et de sa confiance.
                    val text = labels.get(0).text
                    val confidence = labels.get(0).confidence
                    // Ajout des étiquettes à la liste si elle contient moins de deux éléments.
                    if (!(imageLabels.size >= 2)) {
                        imageLabels.add(text)
                        imageLabels.add(confidence.toString())
                    }
                }
                .addOnFailureListener { e ->
                    // En cas d'échec, gestion de l'erreur.
                }
        } catch (e: IOException) {
            // Gestion de l'exception si la création de l'InputImage échoue.
            e.printStackTrace()
        }*/

        // Effet lancé pour insérer l'image capturée dans le ViewModel

    }
}

// Fonction d'extension sur Context pour créer un fichier image temporaire.
fun Context.createImageFile(): File {
    // Création d'un nom de fichier image basé sur un horodatage.
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    // Création d'un fichier temporaire dans le répertoire de cache externe.
    val image = File.createTempFile(
        imageFileName, /* préfixe */
        ".jpg", /* suffixe */
        externalCacheDir /* répertoire */
    )
    // Retour du fichier image créé.
    return image
}
