package com.marinov.cainiaotracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class ArchivedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrackingAdapter
    private lateinit var searchView: SearchView
    private var trackingItems = mutableListOf<TrackingItem>()
    private var allArchivedTrackingItems = mutableListOf<TrackingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_archived, container, false)
        recyclerView = view.findViewById(R.id.recycler_view_archived)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setupToolbar(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        loadLists()
        setupAdapter()
        if (::searchView.isInitialized && !searchView.isIconified) {
            searchView.isIconified = true
        }
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar_archived)
        val searchItem = toolbar.menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Buscar por nome ou código..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
    }

    private fun filterList(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allArchivedTrackingItems
        } else {
            val locale = Locale.getDefault()
            allArchivedTrackingItems.filter {
                it.name.lowercase(locale).contains(query.lowercase(locale)) ||
                        it.code.lowercase(locale).contains(query.lowercase(locale))
            }
        }
        adapter.updateList(filteredList)
    }

    private fun setupAdapter() {
        adapter = TrackingAdapter(
            items = allArchivedTrackingItems,
            mode = AdapterMode.ARCHIVED,
            onArchiveButtonClick = { item -> unarchiveItem(item) },
            onDeleteClick = { item -> deleteItem(item) }
        )
        recyclerView.adapter = adapter
    }

    private fun deleteItem(item: TrackingItem) {
        val position = allArchivedTrackingItems.indexOf(item)
        if (position == -1) return

        allArchivedTrackingItems.remove(item)
        saveLists()
        filterList(searchView.query.toString())

        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
        Snackbar.make(requireView(), "${item.name} foi removido", Snackbar.LENGTH_LONG)
            .setAction("Desfazer") {
                allArchivedTrackingItems.add(position, item)
                saveLists()
                filterList(searchView.query.toString())
            }
            .setAnchorView(bottomNav)
            .show()
    }

    private fun unarchiveItem(item: TrackingItem) {
        val position = allArchivedTrackingItems.indexOf(item)
        if (position == -1) return

        allArchivedTrackingItems.remove(item)
        trackingItems.add(0, item)
        saveLists()
        filterList(searchView.query.toString())

        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
        Snackbar.make(requireView(), "${item.name} foi desarquivado", Snackbar.LENGTH_LONG)
            .setAction("Desfazer") {
                trackingItems.remove(item)
                allArchivedTrackingItems.add(position, item)
                saveLists()
                filterList(searchView.query.toString())
            }
            .setAnchorView(bottomNav)
            .show()
    }

    private fun saveLists() {
        val sharedPref = requireActivity().getPreferences(android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            val gson = Gson()
            putString("tracking_items", gson.toJson(trackingItems))
            putString("archived_items", gson.toJson(allArchivedTrackingItems))
            apply()
        }
    }

    private fun loadLists() {
        val sharedPref = requireActivity().getPreferences(android.content.Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<TrackingItem>>() {}.type

        val trackingJson = sharedPref.getString("tracking_items", null)
        trackingItems = if (trackingJson != null) gson.fromJson(trackingJson, type) else mutableListOf()

        val archivedJson = sharedPref.getString("archived_items", null)
        allArchivedTrackingItems = if (archivedJson != null) gson.fromJson(archivedJson, type) else mutableListOf()
    }
}

