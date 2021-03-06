package com.example.lpiem.theelderscrolls.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lpiem.theelderscrolls.R
import com.example.lpiem.theelderscrolls.adapter.ListCardAdapter
import com.example.lpiem.theelderscrolls.utils.RxLifecycleDelegate
import com.example.lpiem.theelderscrolls.viewmodel.HomeFragmentViewModel
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_buy_card.*
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_sell_card.*
import org.kodein.di.generic.instance
import timber.log.Timber

class SellCardFragment : BaseFragment() {

    private val viewModel: HomeFragmentViewModel by instance(arg = this)

    companion object {
        const val TAG = "SELLCARDFRAGMENT"
        fun newInstance(): SellCardFragment = SellCardFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sell_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ListCardAdapter()
        val mLayoutManager = GridLayoutManager(this.context, 2)
        rv_cards_sell_fragment.setLayoutManager(mLayoutManager)
        rv_cards_sell_fragment.setItemAnimator(DefaultItemAnimator())
        rv_cards_sell_fragment.adapter = adapter

        swiperefresh_fragment_sell.setOnRefreshListener { viewModel.getCardsForConnectedUser() }

        viewModel.userCardsList
                .subscribe(
                        {
                            if(it.isEmpty()){
                                rv_cards_sell_fragment.visibility = View.INVISIBLE
                                tv_no_user_cards_sell_fragment.visibility = View.VISIBLE
                            } else {
                                rv_cards_sell_fragment.visibility = View.VISIBLE
                                tv_no_user_cards_sell_fragment.visibility = View.INVISIBLE
                                adapter.submitList(it)
                            }
                            swiperefresh_fragment_sell.isRefreshing = false
                        },
                        { Timber.e(it) }
                ).addTo(viewDisposable)

        adapter.cardsClickPublisher
                .subscribe(
                        {
                            val action = HomeFragmentDirections.actionMyHomeFragmentToCardDetailsFragment(it, 0)

                            NavHostFragment.findNavController(this).navigate(action)
                        },
                        { Timber.e(it) }
                ).addTo(viewDisposable)

        viewModel.getCardsForConnectedUser()
    }
}