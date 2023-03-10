package com.example.firebasesns.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
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
    private var mainView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        user = FirebaseAuth.getInstance().currentUser
        firebase = FirebaseFirestore.getInstance()

        mainView = inflater.inflate(R.layout.fragment_detail, container, false)

        return mainView
    }

    override fun onResume() {
        super.onResume()
        val detailviewfragment_recyclerview: RecyclerView = mainView!!.findViewById(R.id.detailviewfragment_recyclerview)
        detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        detailviewfragment_recyclerview.adapter = DetailRecyclerViewAdapter()

        var mainActivity = activity as MainActivity
        mainActivity.progress_bar.visibility = View.INVISIBLE
    }

    override fun onStop() {
        super.onStop()
        imageSnapshot?.remove()
    }

    inner class DetailRecyclerViewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = ArrayList()
        var contentUidList: ArrayList<String> = ArrayList()

        init {
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            firebase?.collection("users")?.document(uid!!)?.get()
                ?.addOnCompleteListener{ task ->
                    if (task.isSuccessful) {
                        var userDTO = task.result.toObject(FollowDTO::class.java)
                        if (userDTO?.followings != null) {
                            getCotents(userDTO?.followings)
                        }
                    }
                }
        }

        fun getCotents(follwers: MutableMap<String, Boolean>?) {
            imageSnapshot = firebase?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()

                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)!!
                        println(item.uid)

                        if (follwers?.keys?.contains(item.uid)!!) {
                            contentDTOs.add(item)
                            contentUidList.add(snapshot.id)
                        }
                    }

                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = (holder as CustomViewHolder).itemView

            val detailviewitem_profile_image: ImageView = viewHolder.findViewById(R.id.detailviewitem_profile_image)
            val detailviewitem_profile_text: TextView = viewHolder.findViewById(R.id.detailviewitem_profile_textview)
            val detailviewitem_imageview_content: ImageView = viewHolder.findViewById(R.id.detailviewitem_imageview_content)
            val detailviewitem_explain_textview: TextView = viewHolder.findViewById(R.id.detailviewitem_explain_textview)
            val detailviewitem_favorite_imageview: ImageView = viewHolder.findViewById(R.id.detailviewitem_favorite_imageview)
            val detailviewitem_favoritecounter_textview: TextView = viewHolder.findViewById(R.id.detailviewitem_favoritecounter_textview)

            firebase?.collection("profileImages")?.document(contentDTOs[position].uid!!)
                ?.get()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .apply(RequestOptions()
                                .circleCrop())
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
            }
            else {
                detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            detailviewitem_favoritecounter_textview.text = "좋아요 ${contentDTOs[position].favoriteCount}개"
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        private fun favoriteEvent(position: Int) {
            var tsDoc = firebase?.collection("images")?.document(contentUidList[position])
            firebase?.runTransaction { transaction ->
                val uid = FirebaseAuth.getInstance().currentUser!!.uid
                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) {
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! - 1
                    contentDTO?.favorites!!.remove(uid)
                }
                else {
                    contentDTO?.favoriteCount = contentDTO.favoriteCount!! + 1
                    contentDTO?.favorites!![uid] = true
                }
                transaction.set(tsDoc, contentDTO)
            }
        }
    }

    inner class CustomViewHolder(itemview: View): RecyclerView.ViewHolder(itemview)
}