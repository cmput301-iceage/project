
import argparse
import random
import json
import os
from datetime import datetime, timedelta, timezone, date

import firebase_admin
from firebase_admin import credentials, firestore


# --------------------------------------------------
# Configuration
# --------------------------------------------------

OUTPUT_DIR = "campsite-test-data"

RANDOM_SEED = 42
random.seed(RANDOM_SEED)

NOW = datetime.now(timezone.utc)
TODAY = NOW.date()

# Change these if your Kotlin enum uses different values.
RESERVATION_STATUSES = ["PENDING", "CONFIRMED"]

AMENITIES = [
    "Fire Pit", "Picnic Table", "Parking", "Washroom",
    "Drinking Water", "Lake Access", "Hiking Trails",
    "Pet Friendly", "Fishing", "Shower"
]

TAGS = [
    "Lakefront", "Forest", "Mountain", "Family Friendly",
    "Remote", "Fishing", "Hiking", "RV Friendly",
    "Tent Camping", "Quiet"
]

# --------------------------------------------------
# Firebase initialization
# --------------------------------------------------

def initialize_firebase(project_id=None):
    if not firebase_admin._apps:
        if project_id:
            firebase_admin.initialize_app(
                options={"projectId": project_id}
            )
        else:
            firebase_admin.initialize_app()

    return firestore.client()


# --------------------------------------------------
# Helpers
# --------------------------------------------------

def future_date(days):
    return datetime.combine(
        TODAY + timedelta(days=days),
        datetime.min.time(),
        tzinfo=timezone.utc
    )


def make_dates(start, number_of_nights):
    return [
        datetime.combine(
            start + timedelta(days=i),
            datetime.min.time(),
            tzinfo=timezone.utc
        ).isoformat(timespec="milliseconds").replace("+00:00", "Z")
        for i in range(number_of_nights)
    ]


# --------------------------------------------------
# USERS
# --------------------------------------------------

def generate_users():
    users = {}

    owners = [
        ("owner_01", "Alex Morgan", "alex@example.com"),
        ("owner_02", "Jamie Wilson", "jamie@example.com"),
        ("owner_03", "Taylor Brooks", "taylor@example.com"),
    ]

    for uid, name, email in owners:
        users[uid] = {
            "name": name,
            "email": email,
            "phoneNumber": "780-555-0101",
            "profileImageUrl": None,
            "notificationsEnabled": True,
            "entrant": True,
            "owner": True,
            "admin": False,
        }

    entrants = [
        ("entrant_01", "Jordan Lee"),
        ("entrant_02", "Morgan Smith"),
        ("entrant_03", "Casey Brown"),
        ("entrant_04", "Riley Davis"),
        ("entrant_05", "Avery Wilson"),
        ("entrant_06", "Cameron Taylor"),
        ("entrant_07", "Drew Anderson"),
        ("entrant_08", "Parker Thomas"),
        ("entrant_09", "Quinn Martin"),
    ]

    for uid, name in entrants:
        users[uid] = {
            "name": name,
            "email": f"{uid}@example.com",
            "phoneNumber": None,
            "profileImageUrl": None,
            "notificationsEnabled": True,
            "isEntrant": True,
            "isOwner": False,
            "isAdmin": False,
        }

    return users


# --------------------------------------------------
# CAMPSITES
# --------------------------------------------------

def generate_campsites():
    campsites = {}

    campsite_names = [
        "Pine Ridge Retreat",
        "Eagle Lake Camp",
        "Whispering Pines",
        "Cedar Creek Site",
        "Mountain View Escape",
        "Bear Paw Campground",
        "Hidden Valley",
        "Sunset Lake Retreat",
        "Wildwood Camping",
        "Blue Spruce Haven",
        "Rocky Point Camp",
        "Silver Birch Site",
        "Moose Creek Retreat",
        "Northern Lights Camp",
        "Deer Valley",
        "Crystal Lake Campsite",
        "Aspen Grove",
        "Raven's Nest",
        "Tranquil Shores",
        "Timberline Escape",
    ]

    locations = [
        (53.5461, -113.4938),
        (54.0000, -114.0000),
        (52.9000, -114.5000),
        (53.2000, -112.9000),
        (54.3000, -115.2000),
    ]

    owner_ids = ["owner_01", "owner_02", "owner_03"]

    for i, name in enumerate(campsite_names):
        cid = f"campsite_{i+1:02d}"
        owner_id = owner_ids[min(i // 7, 2)]

        lat, lon = locations[i % len(locations)]

        campsites[cid] = {
            "ownerId": owner_id,
            "name": name,
            "description": (
                f"Enjoy a relaxing outdoor getaway at {name}. "
                "Perfect for camping, hiking, and enjoying nature."
            ),
            "address": f"{100 + i} Forest Road, Alberta, Canada",
            "location": firestore.GeoPoint(
                lat + random.uniform(-0.1, 0.1),
                lon + random.uniform(-0.1, 0.1)
            ),
            "maxOccupancy": random.choice([2, 4, 6, 8]),
            "pricePerNight": random.choice(
                [25.0, 35.0, 45.0, 55.0, 65.0, 80.0]
            ),
            "rating": round(random.uniform(3.5, 5.0), 1),
            "imageUrl": None,
            "rules": (
                "No littering. Respect quiet hours. "
                "Keep campfires within designated fire pits."
            ),
            "cancellationPolicy": (
                "Full refund up to 7 days before check-in."
            ),
            "amenities": random.sample(
                AMENITIES, k=random.randint(3, 6)
            ),
            "tags": random.sample(
                TAGS, k=random.randint(2, 4)
            ),
        }

    return campsites


# --------------------------------------------------
# RESERVATIONS
# --------------------------------------------------

def generate_reservations(campsites):
    reservations = {}

    campsite_ids = list(campsites.keys())
    entrant_ids = [f"entrant_{i:02d}" for i in range(1, 10)]

    for i in range(30):
        rid = f"reservation_{i+1:02d}"

        campsite_id = random.choice(campsite_ids)
        campsite = campsites[campsite_id]

        entrant_id = random.choice(entrant_ids)
        owner_id = campsite["ownerId"]

        check_in = TODAY + timedelta(days=random.randint(-20, 60))
        nights = random.randint(1, 5)
        check_out = check_in + timedelta(days=nights)

        price = campsite["pricePerNight"] * nights

        reservations[rid] = {
            "campsiteId": campsite_id,
            "entrantId": entrant_id,
            "ownerId": owner_id,
            "checkInDate": datetime.combine(
                check_in,
                datetime.min.time(),
                tzinfo=timezone.utc
            ).isoformat(timespec="milliseconds").replace("+00:00", "Z"),

            "checkOutDate": datetime.combine(
                check_out,
                datetime.min.time(),
                tzinfo=timezone.utc
            ).isoformat(timespec="milliseconds").replace("+00:00", "Z"),
            "reservedDates": make_dates(check_in, nights),
            "totalPrice": float(price),
            "status": random.choice(RESERVATION_STATUSES),
            "rulesAccepted": True,
            "createdAt": NOW - timedelta(
                days=random.randint(1, 30)
            ),
            "updatedAt": NOW,
        }

    return reservations


# --------------------------------------------------
# WAITLISTS / LOTTERIES
# --------------------------------------------------

def generate_waitlists():
    waitlists = {}

    configurations = [
        {
            "name": "Summer Camping Lottery",
            "campsites": ["campsite_01", "campsite_02"],
            "winners": 2,
            "capacity": 50,
            "joined": list(range(1, 7)),
            "selected": [1, 3],
            "declined": [3],
            "drawn": True,
        },
        {
            "name": "Long Weekend Getaway",
            "campsites": ["campsite_08"],
            "winners": 1,
            "capacity": 30,
            "joined": list(range(2, 8)),
            "selected": [5],
            "declined": [],
            "drawn": True,
        },
        {
            "name": "Fall Wilderness Retreat",
            "campsites": ["campsite_10", "campsite_11"],
            "winners": 3,
            "capacity": None,
            "joined": list(range(1, 10)),
            "selected": [],
            "declined": [],
            "drawn": False,
        },
        {
            "name": "Winter Cabin Lottery",
            "campsites": ["campsite_15"],
            "winners": 1,
            "capacity": 20,
            "joined": [1, 2, 4, 6],
            "selected": [],
            "declined": [],
            "drawn": False,
        },
    ]

    for i, config in enumerate(configurations):
        wid = f"waitlist_{i+1:02d}"

        # Associate each lottery with the owner of its first campsite.
        # The first campsite's owner is owner_01, owner_02, or owner_03.
        campsite_owner = (
            "owner_01" if i == 0 else
            "owner_02" if i in [1, 2] else
            "owner_03"
        )

        waitlists[wid] = {
            "campsiteIds": config["campsites"],
            "ownerId": campsite_owner,
            "lotteryName": config["name"],
            "registrationStartDate": future_date(-10).isoformat(
                timespec="milliseconds"
            ).replace("+00:00", "Z"),

            "registrationEndDate": future_date(10 + i * 5).isoformat(
                timespec="milliseconds"
            ).replace("+00:00", "Z"),

            "bookingStartDate": future_date(20 + i * 5).isoformat(
                timespec="milliseconds"
            ).replace("+00:00", "Z"),

            "bookingEndDate": future_date(35 + i * 5).isoformat(
                timespec="milliseconds"
            ).replace("+00:00", "Z"),
            "numberOfWinnersToDraw": config["winners"],
            "maxWaitlistCapacity": config["capacity"],
            "isDrawn": config["drawn"],
            "joinedEntrantIds": [
                f"entrant_{n:02d}" for n in config["joined"]
            ],
            "selectedWinnerIds": [
                f"entrant_{n:02d}" for n in config["selected"]
            ],
            "declinedEntrantIds": [
                f"entrant_{n:02d}" for n in config["declined"]
            ],
        }

    return waitlists


# --------------------------------------------------
# UPLOAD
# --------------------------------------------------

def upload_collection(db, collection_name, documents, apply):
    print(f"\n{collection_name}: {len(documents)} documents")

    for doc_id, data in documents.items():
        print(f"  - {doc_id}")

    if not apply:
        print("  [DRY RUN - not uploaded]")
        return

    # Firestore batch limit is 500 writes.
    batch = db.batch()

    for doc_id, data in documents.items():
        ref = db.collection(collection_name).document(doc_id)
        batch.set(ref, data)

    batch.commit()
    print("  [UPLOADED]")

def json_serializer(obj):
    if isinstance(obj, (datetime, date)):
        return obj.isoformat()

    if hasattr(obj, "latitude") and hasattr(obj, "longitude"):
        return {
            "latitude": obj.latitude,
            "longitude": obj.longitude
        }

    raise TypeError(f"Cannot serialize {type(obj)}")


def export_data(data):
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Export each collection separately
    for collection, documents in data.items():
        filename = os.path.join(
            OUTPUT_DIR,
            f"{collection}.json"
        )

        with open(filename, "w", encoding="utf-8") as file:
            json.dump(
                documents,
                file,
                indent=4,
                default=json_serializer,
                ensure_ascii=False
            )

        print(f"Created: {filename}")

    # Export everything into one combined file
    combined_filename = os.path.join(
        OUTPUT_DIR,
        "all_test_data.json"
    )

    with open(combined_filename, "w", encoding="utf-8") as file:
        json.dump(
            data,
            file,
            indent=4,
            default=json_serializer,
            ensure_ascii=False
        )

    print(f"\nCreated combined file: {combined_filename}")

    print("\nExport complete!")
    print(f"Files saved in: {os.path.abspath(OUTPUT_DIR)}")

def generate_all_data():
    """
    Generate all campsite application test data.

    Returns:
        Dictionary containing users, campsites,
        reservations, and waitlists.
    """

    print("Generating users...")
    users = generate_users()

    print("Generating campsites...")
    campsites = generate_campsites()

    print("Generating reservations...")
    reservations = generate_reservations(campsites)

    print("Generating waitlists...")
    waitlists = generate_waitlists()

    data = {
        "users": users,
        "campsites": campsites,
        "reservations": reservations,
        "waitlists": waitlists
    }

    print("\nGeneration summary:")
    for collection, documents in data.items():
        print(f"  {collection}: {len(documents)}")

    return data


def main():
    print("Generating campsite test data...\n")

    # Generate all data locally
    data = generate_all_data()

    # Display summary
    print("Generated:")
    for collection, documents in data.items():
        print(f"  {collection}: {len(documents)} documents")

    # Automatically export JSON
    print("\nExporting JSON files...")
    export_data(data)

    print("\nYou can now inspect the JSON files before uploading!")


if __name__ == "__main__":
    main()
