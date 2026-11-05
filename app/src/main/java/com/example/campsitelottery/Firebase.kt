package com.example.campsitelottery

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.Serializable

open class Firebase_DB(){

    private val db = FirebaseFirestore.getInstance()

    private val collectionMap = mapOf(
        "Campsite" to "Campsites",
        "Owner" to "Owners",
        "Camper" to "Campers",
        "Notification" to "Notifications",
        "Auction" to "Auctions",
        "Room" to "Rooms",
        "Message" to "Messages"
    )

private inline fun <reified T> push2db(collectionKey: String, data: T, crossinline onComplete: (Int) -> Unit) {
        val collectionName = collectionMap[collectionKey] ?: return onComplete(-1)
        db.collection(collectionName)
            .add(data!!)
            .addOnSuccessListener { onComplete(0) }
            .addOnFailureListener { onComplete(-1) }
    }

    fun push_campsite(campsite: Campsites, onComplete: (Int) -> Unit) {
        push2db("Campsite", campsite, onComplete)
    }

    fun get_campsite(campsiteId: String, onResult: (Campsites?) -> Unit) {
        val collectionName = collectionMap["Campsite"] ?: return onResult(null)
        db.collection(collectionName).document(campsiteId).get()
            .addOnSuccessListener { document ->
                val campsite = document.toObject(Campsites::class.java)
                onResult(campsite)
            }
            .addOnFailureListener { onResult(null) }
    }

    fun get_MyRooms(phone_id: String, onResult: (List<Room>) -> Unit) {
        val collectionName = collectionMap["Room"] ?: return onResult(emptyList())
        db.collection(collectionName).get()
            .addOnSuccessListener { snapshot ->
                val myRooms = mutableListOf<Room>()
                for (doc in snapshot.documents) {
                    val room = doc.toObject(Room::class.java)
                    // Check if phone_id exists as a key in the members map
                    if (room != null && room.members.containsKey(phone_id)) {
                        myRooms.add(room)
                    }
                }
                onResult(myRooms)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun push_message(message: Message, onComplete: (Int) -> Unit) {
        push2db("Message", message, onComplete)
    }
}

interface Messenger {
    companion object {
        const val COLLECTION = "Messages"
    }

    fun getDB_messages(room_id: Int, lastpoint: Int, onResult: (List<Message>) -> Unit)
    fun pushDB_message(message: Message, onComplete: (Int) -> Unit)
}


@Serializable
data class Message(
    val room_id: Int,
    val message_id: Int,
    val sender_alias: String,
    val body: String,
    val datetime: Long,
){}

@Serializable
data class Me(
    // This phone
    val messenger_alias: String,
    val saved_rooms: MutableList<Rooms>,
    val saved_messages: MutableList<Messages>,
){}

@Serializable
data class Room(
    // val room_id: Int         // No longer needs room_id, will use map aliases
    val members: Map,            // phone id: alias
    var bottom_index: Int,       // init = 0
    var top_index: Int,          // top_index >= bottomindex; init = 0
    var last_msg: DateTime,

){}

@Serializable
data class Notification(
    val phone_id: String,
    val content: String,
){}

@Serializable
data class Campsites(
    // Please do not confuse a Campsite for Campground (a collection of sites).
    // We are only using Campsite in this implementation
    val owner_phone_id: String,
    val photos: MutableList<Photos>,
    val title: String,
    val discription: String,
    val open_Auctions: MutableList<AuctionSession.sessionset>,
    val address:String,
    val directions:String,
){

interface class AuctionSession(
    val sessionset: Int,
){

}

@Serializable
data class Auction(
    val bid_id: Int,
    val campsite: Campsites,
    val date: Date,
    val members: MutableList<String>,
):AuctionSession{}

@Serializable
data class User(
    val phone_id: String,
    var alias: String,
    var permission_level: Int ,      // 0: Admin, 1: Owner, 2: Camper
    val rooms: MutableList<Rooms>,

){}

@Serializable
data class Owner(
    val campsites: MutableList<Campsites>,
): User{}

@Serializable
data class Camper(
    val my_bids: MutableList<AuctionSession>,
    val my_awards: MutableList<AuctionSession>,
    val my_bookings:MutableList<Auctions>,
):User{}