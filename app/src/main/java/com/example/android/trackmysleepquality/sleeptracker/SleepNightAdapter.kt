package com.example.android.trackmysleepquality.sleeptracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.databinding.ListItemSleepNightBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class SleepNightAdapter(val clickListener: SleepNightListener)
	: ListAdapter<DataItem, RecyclerView.ViewHolder>(DiffNightCallback()) {

	private val adapterScope = CoroutineScope(Dispatchers.Default)

	fun addHeaderAndSubmitList(list: List<SleepNight>?) {
		adapterScope.launch {
			val items = when (list) {
				null -> listOf(DataItem.Header)
				else -> listOf(DataItem.Header) + list.map { DataItem.SleepNightItem(it) }
			}
			withContext(Dispatchers.Main) {
				submitList(items)
			}
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		when (holder) {
			is ViewHolder -> {
				val nightItem = getItem(position) as DataItem.SleepNightItem
				holder.bind(nightItem.sleepNight, clickListener)
			}
		}
	}

	override fun getItemViewType(position: Int): Int {
		return when (getItem(position)) {
			is DataItem.Header -> ITEM_VIEW_TYPE_HEADER
			is DataItem.SleepNightItem -> ITEM_VIEW_TYPE_ITEM
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			ITEM_VIEW_TYPE_HEADER -> TextViewHolder.createFrom(parent)
			ITEM_VIEW_TYPE_ITEM -> ViewHolder.createFrom(parent)
			else -> throw ClassCastException("Unknown viewType ${viewType}")
		}
	}

	class ViewHolder private constructor(private val binding: ListItemSleepNightBinding) : RecyclerView.ViewHolder(binding.root) {
		companion object {
			fun createFrom(parent: ViewGroup): ViewHolder {
				/**
				 *  To inflate a layout from XML, using [LayoutInflater], just like I did in Activities or Fragments.
				 *  I can also create a [LayoutInflater] from any [View] or [ViewGroup] by passing the Context
				 *
				 *  By passing the (parent.context) below, that means I'll create a [LayoutInflater] based on the parent view.
				 *
				 *  It's important to use the right context. [View]s know a lot about themselves, and [LayoutInflater] uses that information to
				 *  inflate new [View]s correctly.
				 *  If you passed a random context here, for example the (application context) you might get views with unexpected colors, fonts,
				 *  or even sizes.
				 *
				 * */
				val layoutInflater = LayoutInflater.from(parent.context)
				val binding = ListItemSleepNightBinding.inflate(layoutInflater, parent, false)
				return ViewHolder(binding)
			}
		}

		fun bind(item: SleepNight, clickListener: SleepNightListener) {
			binding.sleep = item
			binding.clickListener = clickListener
			binding.executePendingBindings()
		}
	}

	class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		companion object {
			fun createFrom(parent: ViewGroup): TextViewHolder {
				val layoutInflater = LayoutInflater.from(parent.context)
				val view = layoutInflater.inflate(R.layout.header, parent, false)
				return TextViewHolder(view)
			}
		}
	}

	/**
	 *  This is a better way to notify the [RecyclerView] that the dataset has changed
	 *  it's a much better that calling [notifyDataSetChanged] method.
	 * */
	class DiffNightCallback : DiffUtil.ItemCallback<DataItem>() {
		override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
			return oldItem.id == newItem.id
		}
	}

	class SleepNightListener(val clickListener: (sleepId: Long) -> Unit) {
		fun onClick(night: SleepNight) = clickListener(night.nightId)
	}


}

sealed class DataItem {
	data class SleepNightItem(val sleepNight: SleepNight) : DataItem() {
		override val id: Long = sleepNight.nightId
	}

	object Header : DataItem() {
		override val id: Long = Long.MIN_VALUE
	}

	abstract val id: Long
}