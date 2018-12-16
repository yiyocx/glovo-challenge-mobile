package yiyo.com.glovoplayground.ui.items

import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.databinding.BindableItem
import yiyo.com.glovoplayground.R
import yiyo.com.glovoplayground.databinding.ItemExpandableHeaderBinding

class ExpandableHeaderItem(private val title: String) : BindableItem<ItemExpandableHeaderBinding>(), ExpandableItem {

    private lateinit var expandableGroup: ExpandableGroup

    override fun bind(viewBinding: ItemExpandableHeaderBinding, position: Int) {
        viewBinding.title = title
        viewBinding.root.setOnClickListener {
            expandableGroup.onToggleExpanded()
            viewBinding.imageViewIcon.setImageResource(getToggleIcon())
        }
    }

    override fun getLayout(): Int = R.layout.item_expandable_header

    override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
        expandableGroup = onToggleListener
    }

    private fun getToggleIcon(): Int {
        return if (expandableGroup.isExpanded) R.drawable.ic_round_expand_less else R.drawable.ic_round_expand_more
    }
}