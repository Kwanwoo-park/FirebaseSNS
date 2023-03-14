package com.example.firebasesns.navigation

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.firebasesns.MainActivity
import com.example.firebasesns.R
import com.example.firebasesns.model.ContentDTO
import com.example.firebasesns.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DetailViewFragment: Fragment() {
    private var user: FirebaseUser? = null
    private var firebase: FirebaseFirestore? = null
    private var imageSnapshot: ListenerRegistration? = null
    private lateinit var mainView: View
    private lateinit var detailviewfragment_recyclerview: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        user = FirebaseAuth.getInstance().currentUser
        firebase = FirebaseFirestore.getInstance()

        mainView = inflater.inflate(R.layout.fragment_detail, container, false)

        detailviewfragment_recyclerview = mainView.findViewById(R.id.detailviewfragment_recyclerview)

        val linearLayoutManager = LinearLayoutManager(requireActivity())
        val detailRecyclerViewAdapter = DetailRecyclerViewAdapter()

        detailviewfragment_recyclerview.apply {
            this.layoutManager = linearLayoutManager
            this.adapter = detailRecyclerViewAdapter
        }

        Log.d("pkw", "onCreateView: 1")

        return mainView
    }

    override fun onResume() {
        super.onResume()

        Log.d("pkw", "onResume: 2")

        val mainActivity = activity as MainActivity
        mainActivity.progress_bar.visibility = View.INVISIBLE
    }

    override fun onStop() {
        Log.d("pkw", "onStop: 3")
        super.onStop()
        imageSnapshot?.remove()
    }

    inner class DetailRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = ArrayList()
        var contentUidList: ArrayList<String> = ArrayList()

        lateinit var detailviewitem_profile_image: ImageView
        lateinit var detailviewitem_profile_text: TextView
        lateinit var detailviewitem_imageview_content: ImageView
        lateinit var detailviewitem_explain_textview: TextView
        lateinit var detailviewitem_favorite_imageview: ImageView
        lateinit var detailviewitem_favoritecounter_textview: TextView

        init {
            Log.d("pkw", ": 5, ${view?.context}")
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            firebase?.collection("users")?.document(uid!!)?.get()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userDTO = task.result.toObject(FollowDTO::class.java)
                        if (userDTO?.followings != null) {
                            getCotents(userDTO.followings)
                        }
                    }
                }
        }

        inner class CustomViewHolder(itemview: View): RecyclerView.ViewHolder(itemview) {
            init {
                Log.d("pkw", "CustomViewHoloder: 8")
                detailviewitem_profile_image = itemview.findViewById(R.id.detailviewitem_profile_image)
                detailviewitem_profile_text = itemview.findViewById(R.id.detailviewitem_profile_textview)
                detailviewitem_imageview_content = itemview.findViewById(R.id.detailviewitem_imageview_content)
                detailviewitem_explain_textview = itemview.findViewById(R.id.detailviewitem_explain_textview)
                detailviewitem_favorite_imageview = itemview.findViewById(R.id.detailviewitem_favorite_imageview)
                detailviewitem_favoritecounter_textview = itemview.findViewById(R.id.detailviewitem_favoritecounter_textview)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            Log.d("pkw", "onCreateViewHolder: 4")
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = (holder as CustomViewHolder).itemView

            Log.d("pkw", "onBindViewHolder: 5")

            firebase?.collection("profileImages")?.document(contentDTOs[position].uid!!)
                ?.get()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(
                                RequestOptions()
                                    .circleCrop()
                            )
                            .into(detailviewitem_profile_image)
                    }
                }

            detailviewitem_profile_image.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()

                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)

                fragment.arguments = bundle

                activity!!.supportFragmentManager.beginTransaction()
                    .replace(R.id.main_content, fragment)
                    .commit()
            }

            detailviewitem_profile_text.text = contentDTOs[position].userId

            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .into(detailviewitem_imageview_content)

            detailviewitem_explain_textview.text = contentDTOs[position].explain
            detailviewitem_favorite_imageview.setOnClickListener { favoriteEvent(position) }

            if (contentDTOs[position].favorites.containsKey(FirebaseAuth.getInstance().currentUser!!.uid)) {
                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            } else {
                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            detailviewitem_favoritecounter_textview.text =
                "좋아요 ${contentDTOs[position].favoriteCount}개"
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        fun getCotents(follwers: MutableMap<String, Boolean>?) {
            Log.d("pkw", "getCotents: 6")
            imageSnapshot = firebase?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()

                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        val item = snapshot.toObject(ContentDTO::class.java)!!
                        println(item.uid)

                        if (follwers?.keys?.contains(item.uid)!!) {
                            contentDTOs.add(item)
                            contentUidList.add(snapshot.id)
                        }
                    }

                    notifyDataSetChanged()
                }
        }

        private fun favoriteEvent(position: Int) {
            Log.d("pkw", "favoriteEvent: 7")
            val tsDoc = firebase?.collection("images")?.document(contentUidList[position])
            firebase?.runTransaction { transaction ->
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) {
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO.favorites.remove(uid)
                } else {
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                    contentDTO.favorites[uid] = true
                }
                transaction.set(tsDoc, contentDTO)
            }
        }
    }
}