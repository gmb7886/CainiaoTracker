package com.marinov.cainiaotracker

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrackingAdapter
    private lateinit var searchView: SearchView
    private lateinit var addItem: MenuItem
    private var allTrackingItems = mutableListOf<TrackingItem>()
    private var archivedTrackingItems = mutableListOf<TrackingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
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
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_add -> {
                    showAddDialog()
                    true
                }
                else -> false
            }
        }

        val searchItem = toolbar.menu.findItem(R.id.action_search)
        addItem = toolbar.menu.findItem(R.id.action_add)
        searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Buscar por nome ou código..."

        // Listener para ocultar/mostrar o botão de adicionar
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                // SearchView expandida, oculta o botão de adicionar
                addItem.isVisible = false
                return true // Retorna true para permitir a expansão
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // SearchView recolhida, mostra o botão de adicionar
                // Usamos post para garantir que a visibilidade seja alterada APÓS a view ser recolhida
                toolbar.post {
                    addItem.isVisible = true
                }
                return true // Retorna true para permitir o recolhimento
            }
        })

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
            allTrackingItems
        } else {
            val locale = Locale.getDefault()
            allTrackingItems.filter {
                it.name.lowercase(locale).contains(query.lowercase(locale)) ||
                        it.code.lowercase(locale).contains(query.lowercase(locale))
            }
        }
        adapter.updateList(filteredList)
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_tracking, null)
        val etName = dialogView.findViewById<EditText>(R.id.et_name)
        val etCode = dialogView.findViewById<EditText>(R.id.et_code)

        AlertDialog.Builder(requireContext())
            .setTitle("Adicionar Encomenda")
            .setView(dialogView)
            .setPositiveButton("Adicionar") { _, _ ->
                val name = etName.text.toString()
                val code = etCode.text.toString()
                if (name.isNotBlank() && code.isNotBlank()) {
                    addTrackingItem(name, code)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addTrackingItem(name: String, code: String) {
        val newItem = TrackingItem(name, code)
        allTrackingItems.add(0, newItem)
        saveLists()
        filterList(searchView.query.toString())
    }

    private fun setupAdapter() {
        adapter = TrackingAdapter(
            items = allTrackingItems,
            mode = AdapterMode.HOME,
            onArchiveButtonClick = { item -> archiveItem(item) },
            onDeleteClick = { item -> deleteItem(item) },
            onUrlClick = { code -> openTrackingUrl(code) }
        )
        recyclerView.adapter = adapter
    }

    private fun deleteItem(item: TrackingItem) {
        val position = allTrackingItems.indexOf(item)
        if (position == -1) return

        allTrackingItems.remove(item)
        saveLists()
        filterList(searchView.query.toString())

        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
        Snackbar.make(requireView(), "${item.name} foi removido", Snackbar.LENGTH_LONG)
            .setAction("Desfazer") {
                allTrackingItems.add(position, item)
                saveLists()
                filterList(searchView.query.toString())
            }
            .setAnchorView(bottomNav)
            .show()
    }

    private fun archiveItem(item: TrackingItem) {
        val position = allTrackingItems.indexOf(item)
        if (position == -1) return

        allTrackingItems.remove(item)
        archivedTrackingItems.add(0, item)
        saveLists()
        filterList(searchView.query.toString())

        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
        Snackbar.make(requireView(), "${item.name} foi arquivado", Snackbar.LENGTH_LONG)
            .setAction("Desfazer") {
                archivedTrackingItems.remove(item)
                allTrackingItems.add(position, item)
                saveLists()
                filterList(searchView.query.toString())
            }
            .setAnchorView(bottomNav)
            .show()
    }

    private fun openTrackingUrl(code: String) {
        val url = "https://global.cainiao.com/newDetail.htm?mailNoList=$code&otherMailNoList="
        val intent = Intent(requireContext(), TrackingActivity::class.java).apply {
            putExtra(TrackingActivity.EXTRA_URL, url)
        }
        startActivity(intent)
    }

    private fun saveLists() {
        val sharedPref = requireActivity().getPreferences(android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            val gson = Gson()
            putString("tracking_items", gson.toJson(allTrackingItems))
            putString("archived_items", gson.toJson(archivedTrackingItems))
            apply()
        }
    }

    private fun loadLists() {
        val sharedPref = requireActivity().getPreferences(android.content.Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<TrackingItem>>() {}.type

        val trackingJson = sharedPref.getString("tracking_items", null)
        allTrackingItems = if (trackingJson != null) gson.fromJson(trackingJson, type) else mutableListOf()

        val archivedJson = sharedPref.getString("archived_items", null)
        archivedTrackingItems = if (archivedJson != null) gson.fromJson(archivedJson, type) else mutableListOf()
    }
}

