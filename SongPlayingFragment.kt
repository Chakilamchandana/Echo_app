package com.internshala.echo.fragments


import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.cleveroad.audiovisualization.AudioVisualization
import com.cleveroad.audiovisualization.DbmHandler
import com.cleveroad.audiovisualization.GLAudioVisualizationView
import com.internshala.echo.CurrentSongHelper
import com.internshala.echo.R
import com.internshala.echo.R.id.seekBar
import com.internshala.echo.Songs
import com.internshala.echo.databases.EchoDatabase
import java.sql.Time
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass.
 */
class SongPlayingFragment : Fragment() {

    var myActivity: Activity?=null
    var mediaplayer: MediaPlayer?=null
    var startTimeText: TextView? = null
    var endTimeText: TextView? = null
    var playPauseImageButton: ImageButton? = null
    var previousImageButton: ImageButton?= null
    var nextImageButton: ImageButton? = null
    var loopImageButton: ImageButton? = null
    var seekbar: SeekBar? = null
    var songArtistView: TextView?= null
    var songTitleView: TextView? = null
    var shuffleImageButton: ImageButton? = null

    var currentPosition: Int = 0
    var fetchSongs: ArrayList<Songs>? = null
    var currentSongHelper: CurrentSongHelper? = null
    var audioVisualization: AudioVisualization?= null
    var glView: GLAudioVisualizationView?=null

    var fab: ImageButton? = null
    object Staticated{
        var MY_PREFS_SHUFFLE = "Shuffle feature"
        var My_PREFS_LOOP = "Loop feature"
    }
    var favoriteContent: EchoDatabase? = null
    var updateSongTime = object :Runnable{
        override fun run() {
           val getCurrent = mediaplayer?.currentPosition
            startTimeText?.setText(String.format("%d:%d",

                    TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long),
                    TimeUnit.MILLISECONDS.toSeconds(getCurrent?.toLong()as Long)-
           TimeUnit.MILLISECONDS.toSeconds( TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong() as Long))))

            seekbar?.setProgress(getCurrent?.toInt() as Int)
            Handler().postDelayed(this, 1000)


        }

    }


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var view = inflater!!.inflate(R.layout.fragment_song_playing,container, false)

        seekbar = view?.findViewById(R.id.seekBar)
        startTimeText = view?.findViewById(R.id.startTime)
        endTimeText = view?.findViewById(R.id.endTime)
        playPauseImageButton  = view?.findViewById(R.id.playPauseButton)
        nextImageButton = view?.findViewById(R.id.nextButton)
        previousImageButton = view?.findViewById(R.id.previousButton)
        loopImageButton = view?.findViewById(R.id.loopButton)
        songArtistView = view?.findViewById(R.id.songArtist)
        songTitleView = view?.findViewById(R.id.songTitle)
        shuffleImageButton = view?.findViewById(R.id.shuffleButton)
        glView = view?.findViewById(R.id.visualizer_view)
        fab = view?.findViewById(R.id.favoriteIcon)
        fab?.alpha = 0.8f
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    audioVisualization = glView as AudioVisualization
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = context as Activity
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        myActivity = activity
    }

    override fun onResume() {
        super.onResume()
    audioVisualization?.onResume()
    }

    override fun onPause() {
        super.onPause()
    audioVisualization?.onPause()
    }

    override fun onDestroyView() {
    audioVisualization?.release()
        super.onDestroyView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        favoriteContent = EchoDatabase(myActivity)
        currentSongHelper = CurrentSongHelper()
        currentSongHelper?.isPlaying = true
        currentSongHelper?.isLoop = false
        currentSongHelper?.isShuffle = false

        var path: String?=null
        var _songTitle: String? = null
        var _songArtist: String? = null
        var songId: Long = 0
        try{
            path = arguments.getString("path")
            _songTitle = arguments.getString("songTitle")
            _songArtist = arguments.getString("songArtist")
            songId = arguments.getInt("songId").toLong()
            currentPosition = arguments.getInt("songPosition")
            fetchSongs = arguments.getParcelableArrayList("songData")

            currentSongHelper?.songPath = path
            currentSongHelper?.songTitle = _songTitle
            currentSongHelper?.songArtist = _songArtist
            currentSongHelper?.songId = songId
            currentSongHelper?.currentPosition = currentPosition

            updateTextView(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

        }catch (e: Exception){
            e.printStackTrace()
        }
        mediaplayer = MediaPlayer()
        mediaplayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        try {
            mediaplayer?.setDataSource(myActivity, Uri.parse(path))
            mediaplayer?.prepare()

        }catch (e: Exception){
            e.printStackTrace()
        }
        mediaplayer?.start()
        processInformation(mediaplayer as MediaPlayer)
        if (currentSongHelper?.isPlaying as Boolean){
            playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        }else{
            playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)
        }
        mediaplayer?.setOnCompletionListener {
onSongComplete()
        }
        clickHandler()
        var visualizationHandler = DbmHandler.Factory.newVisualizerHandler(myActivity as Context, 0)
        audioVisualization?.linkTo(visualizationHandler)


        var prefsForShuffle = myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE,Context.MODE_PRIVATE)
        var isShuffleAllowed = prefsForShuffle?.getBoolean("feature", false)
        if (isShuffleAllowed as Boolean){
            currentSongHelper?.isShuffle = true
            currentSongHelper?.isLoop = false
            shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_icon)
            loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
        }else{
            currentSongHelper?.isShuffle = false
            shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
        }
        var prefsForLoop = myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE,Context.MODE_PRIVATE)
        var isLoopAllowed = prefsForLoop?.getBoolean("feature", false)
        if (isLoopAllowed as Boolean){
            currentSongHelper?.isShuffle = false
            currentSongHelper?.isLoop = true
            shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
            loopImageButton?.setBackgroundResource(R.drawable.loop_icon)
        }else{
            loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            currentSongHelper?.isLoop = false

        }
        if(favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean){
               fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_on))
        }else{
               fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_off))
        }


    }

    fun clickHandler(){

        fab?.setOnClickListener({
            if(favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean){
                fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_off))
                favoriteContent?.deleteFavourite(currentSongHelper?.songId?.toInt() as Int)
                Toast.makeText(myActivity, "Removed from favorites!", Toast.LENGTH_SHORT).show()
            }else{
                fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_on))
                favoriteContent?.storeAsFavorite(currentSongHelper?.songId?.toInt(), currentSongHelper?.songArtist,
                        currentSongHelper?.songTitle, currentSongHelper?.songPath)

                 Toast.makeText(myActivity, "Added to favorites",Toast.LENGTH_SHORT).show()
            }
        })
        shuffleImageButton?.setOnClickListener({
            var editorShuffle = myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)?.edit()
            var editorLoop = myActivity?.getSharedPreferences(Staticated.My_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()

            if (currentSongHelper?.isShuffle as Boolean){
                shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
                currentSongHelper?.isShuffle = false
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()

            }else{
            currentSongHelper?.isShuffle = true
            currentSongHelper?.isLoop = false
            shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_icon)
            loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()
                editorLoop?.putBoolean("feature", true)
                editorLoop?.apply()
        }
        })
        nextImageButton?.setOnClickListener({
            currentSongHelper?.isPlaying = true
            if(currentSongHelper?.isShuffle as Boolean){
                playNext("PlayNextLikeNormalShuffle")
            }else{
                playNext("PlayNextNormal")
            }
        })
        previousImageButton?.setOnClickListener({
            currentSongHelper?.isPlaying = true
            if (currentSongHelper?.isLoop as Boolean){
                loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
            }
            playPrevious()
        })
        loopImageButton?.setOnClickListener({
            var editorShuffle = myActivity?.getSharedPreferences(Staticated.MY_PREFS_SHUFFLE,Context.MODE_PRIVATE)?.edit()
            var editorLoop = myActivity?.getSharedPreferences(Staticated.My_PREFS_LOOP,Context.MODE_PRIVATE)?.edit()

            if(currentSongHelper?.isLoop as Boolean){
                currentSongHelper?.isLoop = false
                loopImageButton?.setBackgroundResource(R.drawable.loop_white_icon)
                editorLoop?.putBoolean("feature", false)
                editorLoop?.apply()
            }else{
                currentSongHelper?.isLoop = true
                currentSongHelper?.isShuffle = false
                loopImageButton?.setBackgroundResource(R.drawable.loop_icon)
                shuffleImageButton?.setBackgroundResource(R.drawable.shuffle_white_icon)
                editorShuffle?.putBoolean("feature", false)
                editorShuffle?.apply()
                editorLoop?.putBoolean("feature", true)
                editorLoop?.apply()
                
            }
        })
        playPauseImageButton?.setOnClickListener({
           if (mediaplayer?.isPlaying as Boolean){
               mediaplayer?.pause()
               currentSongHelper?.isPlaying = false
               playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)
           }else{
               mediaplayer?.start()
               currentSongHelper?.isPlaying = true
               playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
           }
        })
    }

    fun playNext(check: String){
        if(check.equals("PlayNextNormal", true)){
            currentPosition = currentPosition + 1
        }else if(check.equals("PlayNextLikeNormalShuffle", true)){
            var randomObject = Random()
            var randomPosition = randomObject.nextInt(fetchSongs?.size?.plus(1)as Int)
            currentPosition = randomPosition

        }
        if (currentPosition == fetchSongs?.size){
            currentPosition = 0
        }
        currentSongHelper?.isLoop = false
        var nextSong = fetchSongs?.get(currentPosition)
        currentSongHelper?.songTitle = nextSong?.songTitle
        currentSongHelper?.songPath = nextSong?.songData
        currentSongHelper?.currentPosition = currentPosition
        currentSongHelper?.songId = nextSong?.songID as Long

        updateTextView(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)


        mediaplayer?.reset()
        try {
            mediaplayer?.setDataSource(myActivity, Uri.parse(currentSongHelper?.songPath))
            mediaplayer?.prepare()
            mediaplayer?.start()
            processInformation(mediaplayer as MediaPlayer)

        }catch (e: Exception){
            e.printStackTrace()
        }
        if(favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean){
            fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_on))
        }else{
            fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_off))
        }
    }
    fun playPrevious() {
        currentPosition = currentPosition - 1
        if (currentPosition == -1) {
            currentPosition = 0
        }
        if (currentSongHelper?.isPlaying as Boolean) {
            playPauseImageButton?.setBackgroundResource(R.drawable.pause_icon)
        } else {
            playPauseImageButton?.setBackgroundResource(R.drawable.play_icon)
        }
        currentSongHelper?.isLoop = false
        val nextSong = fetchSongs?.get(currentPosition)
        currentSongHelper?.songTitle = nextSong?.songTitle
        currentSongHelper?.songPath = nextSong?.songData
        currentSongHelper?.currentPosition = currentPosition
        currentSongHelper?.songId = nextSong?.songID as Long

        updateTextView(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

        mediaplayer?.reset()
        try {
            mediaplayer?.setDataSource(activity, Uri.parse(currentSongHelper?.songPath))
            mediaplayer?.prepare()
            mediaplayer?.start()
            processInformation(mediaplayer as MediaPlayer)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        if(favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean){
            fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_on))
        }else{
            fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_off))
        }
    }

    fun onSongComplete(){
        if (currentSongHelper?.isShuffle as Boolean) {
            playNext("PlayNextLikeNormalShuffle")
            currentSongHelper?.isPlaying = true

        }else{
            if (currentSongHelper?.isLoop as Boolean){

                currentSongHelper?.isLoop as Boolean
                var nextSong = fetchSongs?.get(currentPosition)

                currentSongHelper?.songTitle = nextSong?.songTitle
                currentSongHelper?.songPath = nextSong?.songData
                currentSongHelper?.currentPosition = currentPosition
                currentSongHelper?.songId = nextSong?.songID as Long

                updateTextView(currentSongHelper?.songTitle as String, currentSongHelper?.songArtist as String)

                mediaplayer?.reset()
                try {
                    mediaplayer?.setDataSource(myActivity,Uri.parse(currentSongHelper?.songPath))
                    mediaplayer?.prepare()
                    mediaplayer?.start()
                    processInformation(mediaplayer as MediaPlayer)

                }catch (e: Exception){
                    e.printStackTrace()
                }

            }else{
                playNext("PlayNextNormal")
                currentSongHelper?.isPlaying = true
            }
        }
        if(favoriteContent?.checkifIdExists(currentSongHelper?.songId?.toInt() as Int) as Boolean){
            fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_on))
        }else{
            fab?.setImageDrawable(ContextCompat.getDrawable(myActivity,R.drawable.favorite_off))
        }

    }
fun updateTextView(songTitle: String, songArtist: String){
    songTitleView?.setText(songTitle)
    songArtistView?.setText(songArtist)
}
    fun processInformation(mediaPlayer: MediaPlayer){
        val finalTime = mediaPlayer.duration
        val startTime = mediaPlayer.currentPosition
        seekbar?.max = finalTime
        startTimeText?.setText(String.format("%d: %d",
                TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(startTime.toLong())- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) )))
        endTimeText?.setText(String.format("%d: %d",
                TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong())- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) )))

         seekbar?.setProgress(startTime)
        Handler().postDelayed(updateSongTime, 1000)
    }

}// Required empty public constructor
