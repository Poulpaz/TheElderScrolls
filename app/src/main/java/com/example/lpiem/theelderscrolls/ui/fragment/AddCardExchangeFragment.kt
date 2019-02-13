package com.example.lpiem.theelderscrolls.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.lpiem.theelderscrolls.R
import com.example.lpiem.theelderscrolls.adapter.ListCardAdapter
import com.example.lpiem.theelderscrolls.adapter.ListPlayersAdapter
import com.example.lpiem.theelderscrolls.datasource.NetworkEvent
import com.example.lpiem.theelderscrolls.viewmodel.AddChatFragmentViewModel
import com.example.lpiem.theelderscrolls.viewmodel.ListExchangeFragmentViewModel
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_add_chat.*
import org.kodein.di.generic.instance
import timber.log.Timber

class AddCardExchangeFragment: BaseFragment() {

    private val viewModel: ListExchangeFragmentViewModel by instance(arg = this)

    companion object {
        const val TAG = "ADDCARDEXCHANGEFRAGMENT"
        fun newInstance(): AddCardExchangeFragment = AddCardExchangeFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDisplayHomeAsUpEnabled(true)
        setDisplayBotomBarNavigation(false)
        setTitleToolbar(getString(R.string.title_add_chat))

        val idExchange = arguments?.let {
            AddCardExchangeFragmentArgs.fromBundle(it).exchange
        }

        val adapter = ListCardAdapter()
        rv_players_fragment_add_chat.setItemAnimator(DefaultItemAnimator())
        rv_players_fragment_add_chat.adapter = adapter

        viewModel.userCardsList
                .subscribe(
                        {
                            adapter.submitList(it)
                        },
                        { Timber.e(it) }
                ).addTo(viewDisposable)

        viewModel.addCardExchangeState.subscribe(
                {
                    when(it){
                        is NetworkEvent.Success -> {
                            fragmentManager?.popBackStack()
                        }
                        is NetworkEvent.Error -> {

                        }
                        is NetworkEvent.InProgress -> {

                        }
                    }
                },
                {
                    Timber.e(it)
                }
        ).addTo(viewDisposable)

        adapter.cardsClickPublisher.subscribe(
                {imageCard ->
                    idExchange?.let {
                        if(imageCard.isNullOrEmpty()){
                            viewModel.getExchange(it, imageCard)
                        } else {
                            viewModel.getExchange(it, null)
                        }
                    }
                },
                { Timber.e(it) }
        ).addTo(viewDisposable)

        viewModel.getAllUsers()
    }

}