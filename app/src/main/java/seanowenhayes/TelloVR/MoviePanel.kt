/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package seanowenhayes.TelloVR

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import seanowenhayes.TelloVR.R
import com.meta.spatial.toolkit.SpatialActivityManager
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val TAG = "MoviePanel"
const val VIDEO_DIRECTORY = "/sdcard/Oculus/VideoShots/"

data class Movie(val id: Int, val uri: Uri, val title: String) {
  companion object {
    fun fromLocalVideo(id: Int, title: String) =
        Movie(
            id,
            Uri.parse("android.resource://" + TelloVrActivity.appPackageName + "/" + id),
            title)

    fun fromRawVideo(rawName: String, title: String): Movie? {
      val resId =
          TelloVrActivity.appContext.resources.getIdentifier(
              rawName, "raw", TelloVrActivity.appPackageName
          )
      return if (resId != 0) fromLocalVideo(resId, title) else null
    }
  }
}

class MovieViewModel : ViewModel() {

  private val moviesState = mutableStateOf(listOf<Movie>())
  val movieList: State<List<Movie>> = moviesState

  init {
    viewModelScope.launch {
      moviesState.value =
          listOf(
              Movie.fromRawVideo("doggie", "Doggie"),
              Movie.fromRawVideo("mediagiant", "Media Giant"),
              Movie.fromRawVideo("carousel", "Carousel"),
              Movie.fromRawVideo("salmon", "Salmon")
          )
              .filterNotNull()
      // Example of loading from a CDN url
      // Movie(
      //     123,
      //     Uri.parse(""),
      //     "CDN: Popcorn Video")
      // loadLocalVideos()
    }
  }

  fun selectMovie(currentMovie: Movie) {
    SpatialActivityManager.executeOnVrActivity<TelloVrActivity> { activity ->
      activity.setVideo(currentMovie.uri)
      activity.playVideo()
    }
  }

  fun nextVideo(selectedMovieUri: Uri) {
    val movieIndex: Int? =
        movieList.value.indexOfFirst { it.uri == selectedMovieUri }?.let { (it + 1) % movieList.value.size }
    selectMovie(movieList.value[movieIndex ?: 0])
  }

  fun previousVideo(selectedMovieUri: Uri) {
    val movieIndex: Int? =
        movieList.value
            .indexOfFirst { it.uri == selectedMovieUri }
            ?.let { (it - 1 + movieList.value.size) % movieList.value.size }
    selectMovie(movieList.value[movieIndex ?: 0])
  }

  fun loadLocalVideos() {
    viewModelScope.launch {
      val localVideos =
          File(VIDEO_DIRECTORY)
              .listFiles()
              ?.filter {
                // only show certain files to filter down SD card
                it.isFile && it.name.lowercase(Locale.getDefault()).contains("spatial_video")
              }
              ?.map { Movie(it.hashCode(), Uri.fromFile(it), it.nameWithoutExtension) }
              ?: emptyList()
      moviesState.value = moviesState.value + localVideos
    }
  }
}

@Composable
fun MovieListScreen(
    moviesViewModel: MovieViewModel,
) {
  val movieList by moviesViewModel.movieList
  Column(
      modifier =
          Modifier.fillMaxSize()
              .graphicsLayer { alpha = 1.0f }
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xFF1C2E33).copy(alpha = 1.0f))
              .graphicsLayer { alpha = 1.0f }
              .padding(16.2.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
              text = "Spatial Video Library",
              minLines = 1,
              fontFamily = FontFamily(Font(R.font.noto_sans_regular)),
              fontSize = 20.sp,
              lineHeight = 18.88.sp,
              fontWeight = FontWeight(700),
              color = Color(0xFFF0F0F0),
              textAlign = TextAlign.Start,
              modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        }
        LazyVerticalGrid(columns = GridCells.Fixed(1)) {
          items(movieList) { currentMovie -> MovieListItem(currentMovie = currentMovie) { moviesViewModel.selectMovie(it) } }
        }
      }
}

/**
 * This component should be used for media files to generate a preview. It will crop the videoUri in
 * half so it only shows one eye
 */
@Composable
fun VideoThumbnail(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
  val context = LocalContext.current
  val videoThumbnail = remember(videoUri) { mutableStateOf<Bitmap?>(null) }
  LaunchedEffect(videoUri) {
    withContext(Dispatchers.IO) {
      val retriever = MediaMetadataRetriever()
      try {
        if (videoUri.scheme == "http" || videoUri.scheme == "https") {
          Log.d(TAG, "videoUri thumbnail for URI (${videoUri})")
          retriever.setDataSource(videoUri.toString(), HashMap())
        } else {
          retriever.setDataSource(context, videoUri)
        }

        videoThumbnail.value = retriever.getFrameAtTime(0)
      } catch (exception: IllegalArgumentException) {
        Log.e(TAG, "Unable to render videoUri thumbnail for URI (${videoUri})", exception)
      } catch (exception: SecurityException) {
        Log.e(TAG, "Unable to render videoUri thumbnail for URI (${videoUri})", exception)
      } catch (exception: RuntimeException) {
        Log.e(TAG, "Unable to render videoUri thumbnail for URI (${videoUri})", exception)
      } finally {
        retriever.release()
      }
    }
  }
  videoThumbnail.value?.let {
    val croppedImage: ImageBitmap =
        Bitmap.createBitmap(it, 0, 0, it.width / 2, it.height).asImageBitmap()
    Image(
        painter = BitmapPainter(croppedImage),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        alignment = Alignment.Center,
        modifier =
            modifier.clickable(onClick = onClick).graphicsLayer {
              // Set the scale of the images directly so they don't "double animate"
              scaleX = 1.0f
              scaleY = 1.0f
            })
  }
}

@Composable
fun MovieListItem(currentMovie: Movie, onSelectMovie: (Movie) -> Unit) {
  Column() {
    Box(
        modifier =
            Modifier.fillMaxWidth().height(150.dp).padding(8.dp).clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center) {
          VideoThumbnail(currentMovie.uri) { onSelectMovie(currentMovie) }
        }
    Box(
        modifier = Modifier.fillMaxSize().padding(start = (8.dp), bottom = (10.dp)),
        contentAlignment = Alignment.BottomStart) {
          Text(
              text = currentMovie.title,
              minLines = 1,
              style =
                  TextStyle(
                      fontSize = 14.sp,
                      lineHeight = 13.49.sp,
                      fontFamily = FontFamily(Font(R.font.noto_sans_regular)),
                      fontWeight = FontWeight(400),
                      color = Color(0xFFF0F0F0)),
          )
        }
  }
}

class MoviePanel : ComponentActivity() {
  override fun onCreate(savedState: Bundle?) {
    super.onCreate(savedState)
    val moviesViewModel: MovieViewModel by viewModels()
    Companion.moviesViewModel = moviesViewModel
    setContent { MovieListScreen(moviesViewModel) }
  }

  companion object {
    lateinit var moviesViewModel: MovieViewModel
  }
}
