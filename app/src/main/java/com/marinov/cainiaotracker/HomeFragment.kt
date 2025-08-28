package com.marinov.cainiaotracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrackingAdapter
    private var trackingItems = mutableListOf<TrackingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadTrackingItems()
        setupAdapter()
        setupToolbar(view)

        return view
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_add -> {
                    showAddDialog()
                    true
                }
                else -> false
            }
        }
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
        trackingItems.add(newItem)
        saveTrackingItems()
        adapter.notifyItemInserted(trackingItems.size - 1)
    }

    private fun setupAdapter() {
        adapter = TrackingAdapter(trackingItems,
            onItemClick = { item ->
                openTrackingUrl(item.code)
            },
            onDeleteClick = { position ->
                deleteItem(position)
            })
        recyclerView.adapter = adapter
    }

    private fun deleteItem(position: Int) {
        val deletedItem = trackingItems[position]
        trackingItems.removeAt(position)
        adapter.notifyItemRemoved(position)
        saveTrackingItems()

        // Obter a view da bottom navigation da activity para ancorar o snackbar
        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
        Snackbar.make(requireView(), "${deletedItem.name} foi removido", Snackbar.LENGTH_LONG)
            .setAction("Desfazer") {
                trackingItems.add(position, deletedItem)
                adapter.notifyItemInserted(position)
                saveTrackingItems()
            }
            .setAnchorView(bottomNav)
            .show()
    }

    private fun openTrackingUrl(code: String) {
        val url = "https://global.cainiao.com/newDetail.htm?mailNoList=$code&otherMailNoList="
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun saveTrackingItems() {
        val sharedPref = requireActivity().getPreferences(android.content.Context.MODE_PRIVATE)
        val json = Gson().toJson(trackingItems)
        sharedPref.edit().putString("tracking_items", json).apply()
    }

    private fun loadTrackingItems() {
        val sharedPref = requireActivity().getPreferences(android.content.Context.MODE_PRIVATE)
        val json = sharedPref.getString("tracking_items", null)
        if (json != null) {
            val type = object : TypeToken<List<TrackingItem>>() {}.type
            trackingItems = Gson().fromJson(json, type) ?: mutableListOf()
        }
    }

    data class TrackingItem(val name: String, val code: String)

    class TrackingAdapter(
        private var items: List<TrackingItem>,
        private val onItemClick: (TrackingItem) -> Unit,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.Adapter<TrackingAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: androidx.appcompat.widget.AppCompatTextView = itemView.findViewById(R.id.tv_title)
            val tvCode: androidx.appcompat.widget.AppCompatTextView = itemView.findViewById(R.id.tv_tracking_code)
            val btnArchive: ImageButton = itemView.findViewById(R.id.btn_archive)
            val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tracking, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.name
            holder.tvCode.text = item.code

            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.btnDelete.setOnClickListener { onDeleteClick(position) }
            // Botão de arquivar não tem ação por enquanto
            holder.btnArchive.setOnClickListener { }
        }

        override fun getItemCount() = items.size
    }
}