package com.example.campsitelottery

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

/**
 * Represents a campsite listing in the reservation System.
 */
data class Campsite(
    @DocumentId
    val campsiteId: String = "",
    var ownerId: String = "",
    var name: String = "",
    var description: String = "",
    var address: String = "",
    var location: GeoPoint? = null,
    var maxOccupancy: Int = 1,
    var pricePerNight: Double = 0.0,
    var rating: Double = 0.0,
    var imageUrl: String? = null,
    var rules: String = "",
    var cancellationPolicy: String = "",
    var amenities: List<String> = emptyList(),
    var tags: List<String> = emptyList()
) {
    // Required empty constructor
    constructor() : this(
        campsiteId = "",
        ownerId = "",
        name = "",
        description = "",
        address = "",
        location = null,
        maxOccupancy = 1,
        pricePerNight = 0.0,
        rating = 0.0,
        imageUrl = null,
        rules = "",
        cancellationPolicy = "",
        amenities = emptyList(),
        tags = emptyList()
    )
}