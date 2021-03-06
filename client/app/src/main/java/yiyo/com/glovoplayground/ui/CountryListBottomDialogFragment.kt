package yiyo.com.glovoplayground.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import yiyo.com.glovoplayground.R
import yiyo.com.glovoplayground.data.models.ActionsUiModel.CountryList
import yiyo.com.glovoplayground.data.models.ActionsUiModel.ShowError
import yiyo.com.glovoplayground.databinding.FragmentBottomSheetCountryListBinding
import yiyo.com.glovoplayground.helpers.extensions.plusAssign
import yiyo.com.glovoplayground.ui.items.CityItem
import yiyo.com.glovoplayground.viewModels.MapsViewModel

class CountryListBottomDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "CountryListBottomDialogFragment"

        fun newInstance(): CountryListBottomDialogFragment {
            return CountryListBottomDialogFragment()
        }
    }

    private lateinit var binding: FragmentBottomSheetCountryListBinding
    private lateinit var viewModel: MapsViewModel
    private val adapter = GroupAdapter<ViewHolder>()
    private val compositeDisposable by lazy { CompositeDisposable() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_bottom_sheet_country_list, container, true)
        viewModel = ViewModelProviders.of(requireActivity())[MapsViewModel::class.java]
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecyclerView()
        subscribeToActions()
        viewModel.loadFullCountries()
    }

    private fun subscribeToActions() {
        compositeDisposable += viewModel.observeActions()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (it) {
                    is CountryList -> adapter.addAll(it.countryGroups)
                    is ShowError -> dismiss()
                }
            }
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        adapter.setOnItemClickListener { item, _ ->
            if (item is CityItem) {
                viewModel.moveToCity(item.city.code)
            }
            dismiss()
        }
    }
}