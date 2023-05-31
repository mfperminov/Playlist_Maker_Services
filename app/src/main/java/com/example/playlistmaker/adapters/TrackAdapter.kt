package com.example.playlistmaker.adapters
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.playlistmaker.R
import com.example.playlistmaker.model.Track
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.playlistmaker.history.LinkedRepository
import java.text.SimpleDateFormat
import java.util.Locale

class TrackAdapter(
    private val historyRepository: LinkedRepository<Track>,
    private val tracks: MutableList<Track> = mutableListOf<Track>()
) : RecyclerView.Adapter<TrackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.search_result_item, parent, false)
        return TrackViewHolder(view).listen() { pos, type ->
            performVibration(view.context)
            val track = tracks[pos]
            Log.d("TrackAdapter", "Clicked on track: ${track.trackId}")
            historyRepository.add(track as Track)
            Log.d("HistorySize", "History size: ${historyRepository.getSize()}")
            Log.d("TrackAdapter", "History: $historyRepository")
        }
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(tracks[position])
    }
    override fun getItemCount(): Int = tracks.size

    fun setTracks(newTracks: List<Track>?) {
        tracks.clear()
        if (!newTracks.isNullOrEmpty()) {
            tracks.addAll(newTracks)
        }
        notifyDataSetChanged()
    }

    fun getTracks(): ArrayList<Track> {
        return ArrayList(tracks)
    }
}

class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val trackName: TextView = itemView.findViewById(R.id.track_name)
    private val trackArtist: TextView = itemView.findViewById(R.id.track_artist)
    private val trackTime: TextView = itemView.findViewById(R.id.track_time)
    private val trackCover: ImageView = itemView.findViewById(R.id.track_cover)

    fun bind(track: Track) {
        trackName.text = track.trackName
        trackArtist.text = track.artistName
        trackTime.text = SimpleDateFormat("mm:ss", Locale.getDefault()).format(track.trackTimeMillis)
        val corner_pixel_size =
            itemView.resources.getDimensionPixelSize(R.dimen.album_cover_corner_radius)
        Glide.with(trackCover.context)
            .load(track.artworkUrl100)
            .centerCrop()
            .placeholder(R.drawable.song_cover_placeholder)
            .transform(RoundedCorners(corner_pixel_size))
            .into(trackCover)

    }
}

fun <T : RecyclerView.ViewHolder> T.listen(event: (position: Int, type: Int) -> Unit): T {
    itemView.setOnClickListener {
        event.invoke(getAdapterPosition(), getItemViewType())
    }
    return this
}

private fun performVibration(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val durationInMilliseconds = 100

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
        val vibrationEffect = VibrationEffect.createOneShot(durationInMilliseconds.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationInMilliseconds)
    }
}

private fun Any.vibrate(durationInMilliseconds: Int) {

}
