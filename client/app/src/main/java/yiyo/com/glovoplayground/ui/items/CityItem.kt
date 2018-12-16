package yiyo.com.glovoplayground.ui.items

import com.xwray.groupie.databinding.BindableItem
import yiyo.com.glovoplayground.R
import yiyo.com.glovoplayground.data.models.CityLite
import yiyo.com.glovoplayground.databinding.ItemCityBinding

class CityItem(val city: CityLite): BindableItem<ItemCityBinding>() {

    override fun bind(viewBinding: ItemCityBinding, position: Int) {
        viewBinding.name = city.name
    }

    override fun getLayout(): Int = R.layout.item_city
}