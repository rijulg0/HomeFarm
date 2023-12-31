package com.rijulg.homefarm

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.rijulg.homefarm.databinding.FragmentChatBinding
import com.rijulg.homefarm.models.Message
import com.rijulg.homefarm.models.Room
import com.rijulg.homefarm.models.User
import com.rijulg.homefarm.recyclerView.MessageAdapter

class ChatFragment : Fragment() {

    // View binding variables
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    // Firestore variable
    private lateinit var firestoreDb: FirebaseFirestore

    // Firebase auth variable
    private lateinit var auth: FirebaseAuth

    private lateinit var roomId: String

    private lateinit var recyclerView: RecyclerView

    private lateinit var messageListener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firestore variable initialized
        firestoreDb = FirebaseFirestore.getInstance()

        //Firebase auth variable initialized
        auth = Firebase.auth

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // View binding variables initialized
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = this.arguments
        val toUser = args?.getSerializable("toUser") as User
        val uid1 = auth.currentUser?.uid as String
        firestoreDb.collection("users").document(uid1).get()
            .addOnSuccessListener { userSnapshot ->
                val fromUser = userSnapshot.toObject(User::class.java)
                val roomData = Room(fromUser, toUser)

                val room = firestoreDb.collection("rooms").where(Filter.or(
                    Filter.and(
                        Filter.equalTo("fromUser", fromUser),
                        Filter.equalTo("toUser", toUser)
                    ),
                    Filter.and(
                        Filter.equalTo("fromUser", toUser),
                        Filter.equalTo("toUser", fromUser)
                    )
                ))

                room.get()
                    .addOnSuccessListener { documents ->
                        if(documents.isEmpty) {
                            roomId = firestoreDb.collection("rooms").document().id

                            binding.sendButton.setOnClickListener {

                                firestoreDb.collection("rooms").document(roomId).set(roomData)
                                val messageText = binding.chatText.text.toString()
                                binding.chatText.text.clear()

                                val message = Message(messageText, fromUser, System.currentTimeMillis())
                                firestoreDb.collection("rooms").document(roomId).collection("messages").document().set(message)
                                val roomLastMessage = Room(fromUser, toUser, message)
                                firestoreDb.collection("rooms").document(roomId).set(roomLastMessage)

                                return@setOnClickListener

                            }

                        } else {
                            for(document in documents) {
                                roomId = document.id
                            }

                            binding.sendButton.setOnClickListener {

                                val messageText = binding.chatText.text.toString()
                                binding.chatText.text.clear()

                                val message = Message(messageText, fromUser, System.currentTimeMillis())
                                firestoreDb.collection("rooms").document(roomId)
                                    .collection("messages").document().set(message)
                                firestoreDb.collection("rooms").document(roomId)
                                    .update("lastMessage", message)
                            }

                        }

                        val messageQuery = firestoreDb.collection("rooms")
                            .document(roomId)
                            .collection("messages")
                            .orderBy("sentAt", Query.Direction.DESCENDING)

                             messageListener = messageQuery.addSnapshotListener { snapshot, exception ->

                                if(exception != null || snapshot == null) {
                                    return@addSnapshotListener
                                }


                                val messageList = snapshot.toObjects(Message::class.java)

                                 // TODO: only get appropriate messages
                                for(document in snapshot) {
                                    val messageId = document.id
                                    firestoreDb.collection("rooms")
                                        .document(roomId)
                                        .collection("messages")
                                        .document(messageId)
                                        .get()
                                        .addOnSuccessListener { messageSnap ->
                                            val messageSnapshot = messageSnap.toObject(Message::class.java)

                                            if(messageSnapshot?.fromUser?.uid != auth.currentUser?.uid){

                                                firestoreDb.collection("rooms")
                                                    .document(roomId)
                                                    .collection("messages")
                                                    .document(messageId)
                                                    .update("seen", true)

                                            }
                                        }
                                }

                                val manager = LinearLayoutManager(activity)
                                manager.reverseLayout = true
                                recyclerView = view.findViewById(R.id.chatRecyclerView)!!
                                recyclerView.layoutManager = manager
                                recyclerView.setHasFixedSize(true)
                                recyclerView.adapter = activity?.let { MessageAdapter(it, messageList) }

                            }

                    }

            }

    }

    override fun onStop() {
        super.onStop()
        messageListener.remove()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}