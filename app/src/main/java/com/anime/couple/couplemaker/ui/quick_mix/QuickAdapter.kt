package com.anime.couple.couplemaker.ui.quick_mix

import android.app.Activity
import androidx.recyclerview.widget.RecyclerView
import com.anime.couple.couplemaker.base.AbsBaseAdapter
import com.anime.couple.couplemaker.base.AbsBaseDiffCallBack
import com.anime.couple.couplemaker.data.model.CustomModel
import com.anime.couple.couplemaker.dialog.DialogExit
import com.anime.couple.couplemaker.utils.DataHelper
import com.anime.couple.couplemaker.utils.hide
import com.anime.couple.couplemaker.utils.isInternetAvailable
import com.anime.couple.couplemaker.utils.show
import com.anime.couple.couplemaker.utils.showToast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.anime.couple.couplemaker.R
import com.anime.couple.couplemaker.databinding.ItemMixBinding

class QuickAdapter(private val activity: Activity) : AbsBaseAdapter<CustomModel, ItemMixBinding>(
    R.layout.item_mix, DiffCallBack()
) {
    var arrListImageSortView = arrayListOf<ArrayList<String>>()
    var onCLick: ((Int) -> Unit)? = null
    var listArrayInt = arrayListOf<ArrayList<ArrayList<Int>>>()

    override fun bind(
        binding: ItemMixBinding,
        position: Int,
        data: CustomModel,
        holder: RecyclerView.ViewHolder
    ) {



        val quickActivity = activity as? QuickMixActivity ?: return
        val bitmap = quickActivity.arrBitmap[position]

        // ✅ Kiểm tra xem đã bind position này chưa
        val currentTag = binding.imvImage.tag as? Int

        if (bitmap != null) {
            binding.shimmer.stopShimmer()
            binding.shimmer.hide()

            if (currentTag != position || binding.imvImage.drawable == null) {
                binding.imvImage.tag = position

                binding.imvImage.post {
                    val fixedHeight = binding.imvImage.height.takeIf { it > 0 }
                        ?: binding.root.height  // fallback về height của root

                    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val params = binding.imvImage.layoutParams
                    params.height = fixedHeight
                    params.width = (fixedHeight * aspectRatio).toInt()
                    binding.imvImage.layoutParams = params

                    Glide.with(binding.root.context)
                        .load(bitmap)
                        .dontTransform()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.imvImage)
                }
            }
        }else {
            // ✅ Chưa có bitmap
            // Chỉ hiển thị shimmer nếu position thay đổi hoặc shimmer đang ẩn
            if (currentTag != position) {
                binding.imvImage.tag = position
                binding.shimmer.startShimmer()
                binding.shimmer.show()

                // Clear image cũ
                Glide.with(binding.root.context).clear(binding.imvImage)
                binding.imvImage.setImageDrawable(null)
            }

            // ✅ Request load bitmap
            quickActivity.requestBitmap(position)

            // ✅ Click vào shimmer
            if (binding.shimmer.tag != position) {
                binding.shimmer.tag = position
                binding.shimmer.setOnClickListener {
                    if (!isInternetAvailable(activity) && position < DataHelper.arrBlackCentered.size - 1) {
                        DialogExit(activity, "network").show()
                        return@setOnClickListener
                    }
                    showToast(binding.root.context, R.string.wait_a_few_second)
                }
            }
        }

        // ✅ Set click listeners chỉ một lần
        if (binding.root.tag != position) {
            binding.root.tag = position
            binding.root.setOnClickListener { onCLick?.invoke(position) }
            binding.imvImage.setOnClickListener { onCLick?.invoke(position) }
        }
    }

    class DiffCallBack : AbsBaseDiffCallBack<CustomModel>() {
        override fun itemsTheSame(oldItem: CustomModel, newItem: CustomModel): Boolean {
            return oldItem.avt == newItem.avt
        }

        override fun contentsTheSame(oldItem: CustomModel, newItem: CustomModel): Boolean {
            return oldItem.avt == newItem.avt
        }
    }
}