package com.example.lpiem.theelderscrolls.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lpiem.theelderscrolls.R
import com.example.lpiem.theelderscrolls.model.Card
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.view.clicks
import com.squareup.picasso.Picasso
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.item_card.view.*

class ListCardExchangeAdapter : ListAdapter<Card, ListCardExchangeAdapter.CardViewHolder>(DiffCardCallback()) {

    val indexClickPublisher: PublishSubject<String> = PublishSubject.create()
    var idItemSelected : String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view, indexClickPublisher)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = getItem(position)
        holder.bind(getItem(position), card.idCard == idItemSelected)
    }

    inner class CardViewHolder(itemView: View, private val indexClickPublisher: PublishSubject<String>) : RecyclerView.ViewHolder(itemView) {

        fun bind(card: Card, isSelected : Boolean) {
            if (card.imageUrl != null) {
                Picasso.get()
                        .load(card.imageUrl)
                        .placeholder(R.drawable.card_placeholder)
                        .into(itemView.iv_item_card)
            }
            itemView.iv_item_card.isSelected = isSelected
            bindPositionClick(card.idCard)
        }

        private fun bindPositionClick(idCard: String) {
            itemView.setOnClickListener {
                if(idItemSelected == idCard) {
                    indexClickPublisher.onNext("")
                    idItemSelected = ""
                } else {
                    indexClickPublisher.onNext(idCard)
                    idItemSelected = idCard
                }
                notifyDataSetChanged()
            }
        }
    }

    class DiffCardCallback : DiffUtil.ItemCallback<Card>() {
        override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
            return oldItem.idCard == newItem.idCard
        }

        override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
            return oldItem.idCard == newItem.idCard
        }
    }

}