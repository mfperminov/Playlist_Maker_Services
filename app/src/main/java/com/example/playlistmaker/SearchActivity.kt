package com.example.playlistmaker

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.playlistmaker.adapters.TrackAdapter
import com.example.playlistmaker.model.Track
import com.example.playlistmaker.model.tracks
import com.example.playlistmaker.networkClient.ITunesApi
import com.example.playlistmaker.networkClient.SongsSearchResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

typealias TrackList = ArrayList<Track>

class SearchActivity : AppCompatActivity() {
    private lateinit var searchEditText: EditText
    private lateinit var clearButton: ImageView
    private lateinit var problemsLayout: LinearLayout
    private lateinit var problemsText: TextView
    private lateinit var problemsIcon: ImageView
    private val tracks = ArrayList<Track>()
    private lateinit var refreshButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var trackAdapter: TrackAdapter

    companion object {
        const val SEARCH_QUERY = "SEARCH_QUERY"
        const val TRACKS = "TRACKS"
        const val API_URL = "https://itunes.apple.com"
        const val PREFS = "my_prefs"
        const val QUERY = "searchQuery"
        const val TRACKS_LIST = "TRACKS_LIST"
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(SearchActivity.API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val itunesService = retrofit.create(ITunesApi::class.java)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        val backButton = findViewById<ImageView>(R.id.return_button)
        recyclerView = findViewById<RecyclerView>(R.id.search_results_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        trackAdapter = TrackAdapter(tracks)
        recyclerView.adapter = trackAdapter
        searchEditText = findViewById(R.id.searchEditText)
        clearButton = findViewById(R.id.clearIcon)
        problemsLayout = findViewById(R.id.problems_layout)
        problemsText = findViewById(R.id.search_placeholder_text)
        problemsIcon = findViewById(R.id.problems_image)
        refreshButton = findViewById(R.id.refresh_button)
        loadingIndicator = findViewById(R.id.loading_indicator)
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (searchEditText.text.isNotEmpty()) {
                    search(searchEditText.text.toString())
                }
            }
            false
        }

        val simpleTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // empty
            }

            @SuppressLint("UseCompatLoadingForDrawables")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    makeClearButtonInvisible()

                } else {
                    makeClearButtonVisible()
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // тут надо будет совершить поиск
            }
        }
        if (savedInstanceState != null) {
            searchEditText.setText(savedInstanceState.getString(SEARCH_QUERY, ""))
        }

        val sharedPref = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val searchQuery = sharedPref.getString(QUERY, "")
        val json = sharedPref.getString(TRACKS_LIST, "")

        if (searchQuery.isNullOrEmpty()) {
            makeClearButtonInvisible()
        } else {
            makeClearButtonVisible()
        }
        searchEditText.setText(searchQuery)

        if (json.isNullOrEmpty()) {
            hideProblemsLayout()
        } else {
            val gson = Gson()
            val type = object : TypeToken<TrackList>() {}.type
            val restoredTracks: TrackList = gson.fromJson(json, type)
            trackAdapter.setTracks(restoredTracks)
            hideProblemsLayout()
        }
        searchEditText.addTextChangedListener(simpleTextWatcher)

        // прослушиватель нажатия на кнопку "очистить"
        clearButton.setOnClickListener {
            searchEditText.setText("")
            val keyboard = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            keyboard.hideSoftInputFromWindow(searchEditText.windowToken, 0) // скрыть клавиатуру
            searchEditText.clearFocus()
            trackAdapter.setTracks(null)
            hideProblemsLayout()
        }

//         прослушиватель нажатия на кнопку "назад"
        backButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(QUERY, searchEditText.text.toString())
            val gson = Gson()
            val json = gson.toJson(tracks)
            editor.putString(TRACKS_LIST, json)
            editor.apply()
            this.finish()
        }
        refreshButton.setOnClickListener {
            search(searchEditText.text.toString())
        }

    }


    private fun makeClearButtonInvisible() {
        clearButton.visibility = View.GONE
        searchEditText.background = getDrawable(R.drawable.rounded_edittext)
    }

    private fun makeClearButtonVisible() {
        clearButton.visibility = View.VISIBLE
        clearButton.background = getDrawable(R.drawable.right_rounded_edittext)
        searchEditText.background = getDrawable(R.drawable.left_rounded_edittext)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SEARCH_QUERY, searchEditText.text.toString())
        outState.putParcelableArrayList(TRACKS, tracks)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Вторым параметром мы передаём значение по умолчанию
        val searchQuery = savedInstanceState.getString(SEARCH_QUERY, "")
        searchEditText.setText(searchQuery)
        // TODO когда метод getParcelableArrayList уберут, надо будет использвать сериализацию в json и восстоновление из json
        val restoredTracks = savedInstanceState.getParcelableArrayList<Track>(TRACKS)
        if (restoredTracks != null) {
            trackAdapter.setTracks(restoredTracks)
        }
    }

    private fun search(searchQuery: String) {
        trackAdapter.setTracks(null)
        loadingIndicator.visibility = View.VISIBLE
        hideProblemsLayout()
        recyclerView.visibility = View.GONE
        val call = itunesService.search(searchQuery)
        call.enqueue(object : Callback<SongsSearchResponse> {
            override fun onResponse(
                call: Call<SongsSearchResponse>,
                response: Response<SongsSearchResponse>
            ) {
                when (response.code()) {
                    200 -> {
                        if (response.body()?.results?.isNotEmpty() == true) {
                            loadingIndicator.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            val sharedPreferences =
                                getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putString(TRACKS_LIST, response.body()?.results!!.toString())
                            trackAdapter.setTracks(response.body()?.results!!)
                        } else {
                            showProblemsLayout("nothing_found")
                        }
                    }

                    else -> {
                        showProblemsLayout("error")
                    }
                }
            }

            override fun onFailure(call: Call<SongsSearchResponse>, t: Throwable) {
                showProblemsLayout()
            }
        })
    }

    private fun showProblemsLayout(state: String = "error") {
        recyclerView.visibility = View.GONE
        problemsLayout.visibility = View.VISIBLE
        when (state) {
            "error" -> {
                recyclerView.visibility = View.GONE
                loadingIndicator.visibility = View.GONE
                problemsText.text = getString(R.string.no_internet)
                problemsIcon.setImageResource(R.drawable.no_internet)
                refreshButton.visibility = View.VISIBLE
                refreshButton.setOnClickListener {
                    search(searchEditText.text.toString())
                }
            }

            "nothing_found" -> {
                recyclerView.visibility = View.GONE
                loadingIndicator.visibility = View.GONE
                problemsText.text = getString(R.string.nothing_found)
                problemsIcon.setImageResource(R.drawable.nothing_found)
                refreshButton.visibility = View.GONE
            }
        }
    }

    private fun hideProblemsLayout() {
        recyclerView.visibility = View.VISIBLE
        problemsLayout.visibility = View.GONE
        refreshButton.visibility = View.GONE
    }

}
