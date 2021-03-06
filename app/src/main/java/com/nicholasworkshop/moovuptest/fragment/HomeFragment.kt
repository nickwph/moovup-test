package com.nicholasworkshop.moovuptest.fragment

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nicholasworkshop.moovuptest.MainApplication
import com.nicholasworkshop.moovuptest.R
import com.nicholasworkshop.moovuptest.api.FriendService
import com.nicholasworkshop.moovuptest.databinding.ViewFriendBinding
import com.nicholasworkshop.moovuptest.model.Friend
import com.nicholasworkshop.moovuptest.model.FriendDao
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import javax.inject.Inject


class HomeFragment : Fragment() {

    companion object {
        fun newInstance(): HomeFragment {
            val fragment = HomeFragment()
            fragment.exitTransition = Fade()
            fragment.enterTransition = Fade()
            return fragment
        }
    }

    @Inject lateinit var friendService: FriendService
    @Inject lateinit var friendDao: FriendDao
    @Inject lateinit var viewModel: HomeViewModel
    @Inject lateinit var adapter: HomeRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerHomeComponent
                .builder()
                .mainComponent((activity!!.application as MainApplication).component)
                .homeModule(HomeModule(this))
                .build()
                .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity!!.title = "All friends"
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        viewModel.friendDao.all().observe(this, Observer<List<Friend>> { friendList ->
            adapter.friendList = friendList
            adapter.notifyDataSetChanged()
        })
        friendService.get()
                .subscribeOn(Schedulers.io())
                .subscribe {
                    friendDao.insertAll(it.map {
                        Friend(it._id!!,
                                it.picture,
                                it.name,
                                it.email,
                                it.location!!.latitude,
                                it.location.longitude)
                    })
                }
    }
}

class HomeRecyclerViewAdapter(
        private val fragmentManager: FragmentManager
) : RecyclerView.Adapter<HomeViewHolder>() {

    var friendList: List<Friend>? = null

    override fun getItemCount(): Int {
        return if (friendList != null) friendList!!.size else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewFriendBinding.inflate(inflater, parent, false)
        binding.clickListener = View.OnClickListener {
            fragmentManager
                    .beginTransaction()
                    .addToBackStack("detail")
                    .replace(R.id.containerView, DetailFragment.newInstance(binding.friend!!.id))
                    .commit()
        }
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bind(friendList!![position])
    }
}

class HomeViewHolder(
        private val binding: ViewFriendBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(friend: Friend) {
        binding.friend = friend
        binding.executePendingBindings()
    }
}

class HomeViewModel(
        val friendDao: FriendDao
) : ViewModel()

@Suppress("UNCHECKED_CAST")
class HomeViewModelFactory(
        val friendDao: FriendDao
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return HomeViewModel(friendDao) as T
    }
}