package com.cute.anime.avatarmaker.ui.tutorial

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cute.anime.avatarmaker.data.model.TutorialModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cute.anime.avatarmaker.databinding.ItemTutorialBinding


class ViewPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var data = arrayListOf<TutorialModel>()
    fun getData(mData: List<TutorialModel>) {
        data = mData as ArrayList<TutorialModel>
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var binding = ItemTutorialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(position)
        }
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(val binding: ItemTutorialBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.mTutorialModel = data[position]
            Glide.with(binding.imv)
                .load(data[position].bg)
                .encodeQuality(50)
                .override(512)
                .dontTransform()
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(binding.imv)
            binding.tv1.isSelected = false
            binding.tv1.postDelayed({
                binding.tv1.isSelected = true
            }, 50)
        }
    }
}