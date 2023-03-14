package com.example.firebasesns.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.firebasesns.LoginActivity
import com.example.firebasesns.MainActivity
import com.example.firebasesns.R
import com.example.firebasesns.model.ContentDTO
import com.example.firebasesns.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UserFragment: Fragment() {
    private val PICK_PROFILE_FROM_ALBUM = -1
    private var uid: String? = null
    private var currentUserUid: String? = null
    private var followListenerRegistration: ListenerRegistration? = null
    private var followingListenerRegistration: ListenerRegistration? = null
    private var imagefileListenerRegistration: ListenerRegistration? = null
    private var recyclerListenerRegistration: ListenerRegistration? = null
    private var fragmentView: View? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var account_btn_follow_signout: Button
    private lateinit var account_iv_profile: ImageView
    private lateinit var account_recyclerview: RecyclerView
    private lateinit var account_tv_following_count: TextView
    private lateinit var account_tv_follower_counter: TextView
    private lateinit var account_tv_post_count: TextView

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == PICK_PROFILE_FROM_ALBUM) {

        }
        else {
            Log.d("pkw", "openActivityResultLauncher: sign in unsuccess ${result.resultCode}")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView = inflater.inflate(R.layout.fragment_user, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        account_btn_follow_signout = fragmentView!!.findViewById(R.id.account_btn_follow_signout)
        account_iv_profile = fragmentView!!.findViewById(R.id.account_iv_profile)
        account_recyclerview = fragmentView!!.findViewById(R.id.account_recyclerview)
        account_tv_following_count = fragmentView!!.findViewById(R.id.account_tv_following_count)
        account_tv_follower_counter = fragmentView!!.findViewById(R.id.account_tv_follower_count)
        account_tv_post_count = fragmentView!!.findViewById(R.id.account_tv_post_count)
        currentUserUid = auth?.currentUser?.uid

        if (arguments != null) {
            uid = requireArguments().getString("destinationUid")

            if (uid != null && uid == currentUserUid) {
                account_btn_follow_signout.text = getString(R.string.signout)
                account_btn_follow_signout.setOnClickListener {
                    activity?.finish()
                    startActivity(Intent(activity, LoginActivity::class.java))
                    auth.signOut()
                }
            }
            else {
                account_btn_follow_signout.text = getString(R.string.follow)
                var mainActivity = (activity as MainActivity)
                mainActivity.toolbar_title_image.visibility = View.GONE
                mainActivity.toolbar_btn_back.visibility = View.VISIBLE
                mainActivity.toolbar_username.visibility = View.VISIBLE

                mainActivity.toolbar_username.text = requireArguments().getString("userId")

                mainActivity.toolbar_btn_back.setOnClickListener { mainActivity.bottom_navigation.selectedItemId = R.id.action_home }

                account_btn_follow_signout.setOnClickListener{
                    requestFollow()
                }
            }
        }

        account_iv_profile.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                val actionLauncher = resultLauncher
                actionLauncher.launch(photoPickerIntent)
            }
        }

        account_recyclerview.layoutManager = GridLayoutManager(requireActivity(), 3)
        account_recyclerview.adapter = UserFragmentRecyclerviewAdapter()

        return fragmentView
    }

    override fun onResume() {
        super.onResume()
        getProfileImage()
        getFollowing()
        getFollower()
    }

    private fun getProfileImage() {
        imagefileListenerRegistration = firestore.collection("profileImages").document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (documentSnapshot?.data != null) {
                    val url = documentSnapshot.data!!["image"]
                    Glide.with(activity)
                        .load(url)
                        .apply(RequestOptions().circleCrop()).into(account_iv_profile)
                }
            }
    }

    private fun getFollowing() {
        followingListenerRegistration = firestore.collection("users").document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                val followDTO = documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener

                account_tv_following_count.text = followDTO.followingCount.toString()
            }
    }

    private fun getFollower() {
        followListenerRegistration = firestore.collection("users").document(uid!!)
            ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                val followDTO = documentSnapshot?.toObject(FollowDTO::class.java) ?: return@addSnapshotListener

                account_tv_follower_counter.text = followDTO.followingCount.toString()

                if (followDTO.followers.containsKey(currentUserUid)) {
                    account_btn_follow_signout.text = getString(R.string.follow_cancel)
                    account_btn_follow_signout.background
                        .setColorFilter(ContextCompat.getColor(requireActivity(), R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                }
                else {
                    if (uid != currentUserUid) {
                        account_btn_follow_signout.text = getString(R.string.follow)
                        account_btn_follow_signout.background.colorFilter = null
                    }
                }
            }
    }

    private fun requestFollow() {
        var tsDocFollowing = firestore.collection("users").document(currentUserUid!!)
        firestore.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)

            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if (followDTO.followings.containsKey(uid)) {
                followDTO.followerCount = followDTO.followingCount-1
                followDTO.followings.remove(uid)
            }
            else {
                followDTO.followingCount = followDTO.followingCount+1
                followDTO.followings[uid!!] = true
            }

            transaction.set(tsDocFollowing, followDTO)

            return@runTransaction
        }

        var tsDocFollower = firestore.collection("users").document(uid!!)
        firestore.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if (followDTO?.followers?.containsKey(currentUserUid!!)!!) {
                followDTO!!.followerCount = followDTO!!.followerCount-1
                followDTO!!.followers.remove(currentUserUid!!)
            }
            else {
                followDTO!!.followerCount = followDTO!!.followerCount+1
                followDTO!!.followers[currentUserUid!!] = true
            }

            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    inner class UserFragmentRecyclerviewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = ArrayList()

        init {
            recyclerListenerRegistration = firestore.collection("images").whereEqualTo("uid", uid)
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()

                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }

                    account_tv_post_count.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3
            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageViews = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(imageViews)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        inner class CustomViewHolder(var imageView: ImageView): RecyclerView.ViewHolder(imageView)
    }

    override fun onStop() {
        super.onStop()
        followListenerRegistration
    }
}