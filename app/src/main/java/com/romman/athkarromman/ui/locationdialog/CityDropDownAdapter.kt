package com.romman.athkarromman.ui.locationdialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.romman.athkarromman.data.model.City
import com.romman.athkarromman.databinding.ItemCityDropDownBinding

/**
 * Created By Batool Mofeed - 08/04/2024.
 **/

abstract class BaseVH(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(position: Int)
}

class CityDropDownAdapter(
    private val cityClicked: (City) -> Unit

) : RecyclerView.Adapter<BaseVH>() {

    val items = ArrayList<City>()

    fun addItems(items: List<City>) {
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CityVH(
            ItemCityDropDownBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: BaseVH, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = items.size

    inner class CityVH(
        private val binding: ItemCityDropDownBinding
    ) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            binding.run {
                item = items[position]
                root.setOnClickListener { cityClicked(items[position]) }
                executePendingBindings()
            }
        }
    }

}