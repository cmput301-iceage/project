package com.example.campsitelottery

import kotlinx.serialization.Serializable

open class Firebase_DB(){

    private val collectionMap:Map = Map{
        // Map of Kotlin objects to Firebase collection.
        "Campsite": "Campsites",
        "Owner": "Owners",
        "Camper": "Campers",
        "Notification": "Notifications",
        "Auction": "Auctions"
        "Room":"Rooms",
        "Message": "Messages",
        "Notification":"Notifications"
    }

    private fun push2db(collection:String, package):Int{
        // returns 0 if successful
        // returns not 0 if failed
    }

    fun push_campsite(package:Campsite):Int{
        return push2db(collectionMap["Campsite"],package)
    }

    fun get_campsite(){}

    fun get_MyRooms(phone_id):MutableListOf<Rooms>{
        // select * in Rooms where phone_id in member_keys
        myrooms:MutableList<Rooms>
        for each * in Rooms:
        room = Room(


            fun push_message(package:Message):Int{
                return push2db(collectionMap["Message"], package)
            }


            fun get_

    }

interface class Messenger(){
    val collection = "Messages"

    fun getDB_messages(room_id: Int, lastpoint: Int){}

    fun pushDB_message(room_id: Int, payload:Message){}

    fun
}


@Serializable
data class Message(){
    val room_id: Int
    val message_id: Int
    val sender_alias: String
    val body: String
    val datetime: DateTime
}

@Serializable
data class Me(){
    // This phone
    val messenger_alias: String
    val saved_rooms: MutableList<Rooms>
    val saved_messages: MutableList<Messages>
}

@Serializable
data class Room(){
    // val room_id: Int         // No longer needs room_id, will use map aliases
    val members: Map            // phone id: alias
    var bottom_index: Int       // init = 0
    var top_index: Int          // top_index >= bottomindex; init = 0
    var last_msg: DateTime

}

@Serializable
data class Notification(){
    val phone_id: String
    val content: String
}

@Serializable
data class Campsites(){
    // Please do not confuse a Campsite for Campground (a collection of sites).
    // We are only using Campsite in this implementation
    val owner_phone_id: String
    val photos: MutableList<Photos>
    val title: String
    val discription: String
    val open_Auctions: MutableList<AuctionSession.sessionset>
    val address:String
    val directions:String


interface class AuctionSession(){
    val sessionset: Int

}

@Serializable
data class Auction():AuctionSession{
    val bid_id: Int
    val campsite: Campsites
    val date: Date
    val members: MutableList<String>
}

@Serializable
data class User(){
    val phone_id: String
    var alias: String
    var permission_level: Int       // 0: Admin, 1: Owner, 2: Camper
    val rooms: MutableList<Rooms>

}

@Serializable
data class Owner(): User{
    val campsites: MutableList<Campsites>
}

@Serializable
data class Camper():User{
    val my_bids: MutableList<AuctionSession>
    val my_awards: MutableList<AuctionSession>
    val my_bookings:MutableList<Auctions>
