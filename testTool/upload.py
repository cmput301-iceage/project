
import json
import os
import argparse

import firebase_admin
from firebase_admin import credentials, firestore

from datetime import datetime, timezone


DATA_DIR = "campsite-test-data"

COLLECTIONS = [
    "users",
    "campsites",
    "reservations",
    "waitlists"
]


def initialize_firebase(service_account, project_id=None):
    cred = credentials.Certificate(service_account)

    options = {}
    if project_id:
        options["projectId"] = project_id

    firebase_admin.initialize_app(cred, options)
    return firestore.client()



def convert_value(value):
    """Recursively convert JSON values to Firestore types."""

    if isinstance(value, dict):

        # Convert latitude/longitude into GeoPoint
        if "latitude" in value and "longitude" in value:
            return firestore.GeoPoint(
                float(value["latitude"]),
                float(value["longitude"])
            )

        return {
            key: convert_value(item)
            for key, item in value.items()
        }

    if isinstance(value, list):
        return [convert_value(item) for item in value]

    # Convert ISO-8601 date strings into Python datetime objects.
    if isinstance(value, str):
        try:
            parsed = datetime.fromisoformat(
                value.replace("Z", "+00:00")
            )

            # Only convert strings that look like full ISO dates.
            if "T" in value:
                return parsed

        except ValueError:
            pass

    return value



def upload_collection(db, collection_name):
    filepath = os.path.join(
        DATA_DIR,
        f"{collection_name}.json"
    )

    if not os.path.exists(filepath):
        print(f"Missing file: {filepath}")
        return

    with open(filepath, "r", encoding="utf-8") as file:
        documents = json.load(file)

    print(f"\nUploading {collection_name}...")

    batch = db.batch()
    count = 0

    for document_id, document_data in documents.items():
        ref = db.collection(collection_name).document(
            document_id
        )



        if collection_name == "reservations":
            document_data = fix_reservation_dates(document_data)

        converted_data = convert_value(document_data)

        batch.set(ref, converted_data)
        count += 1

        # Firestore batch limit is 500 operations
        if count % 450 == 0:
            batch.commit()
            batch = db.batch()

    if count % 450 != 0:
        batch.commit()

    print(f"Uploaded {count} documents to {collection_name}")

def parse_firestore_date(value):
    if isinstance(value, datetime):
        return value

    return datetime.fromisoformat(
        value.replace("Z", "+00:00")
    ).astimezone(timezone.utc)


def fix_reservation_dates(data):
    # Individual date fields
    data["checkInDate"] = parse_firestore_date(
        data["checkInDate"]
    )

    data["checkOutDate"] = parse_firestore_date(
        data["checkOutDate"]
    )

    # IMPORTANT: Convert EVERY element in the list
    data["reservedDates"] = [
        parse_firestore_date(date)
        for date in data["reservedDates"]
    ]

    data["createdAt"] = parse_firestore_date(
        data["createdAt"]
    )

    data["updatedAt"] = parse_firestore_date(
        data["updatedAt"]
    )

    return data


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--project",
        required=True,
        help="Firebase project ID"
    )

    parser.add_argument(
        "--key",
        default="serviceAccountKey.json",
        help="Service account JSON file"
    )

    args = parser.parse_args()

    if not os.path.exists(args.key):
        print(f"Service account key not found: {args.key}")
        return

    print(f"Target Firebase project: {args.project}")

    confirmation = input(
        "\nAre you sure you want to upload? (yes/no): "
    )

    if confirmation.lower() != "yes":
        print("Upload cancelled.")
        return

    db = initialize_firebase(
        args.key,
        args.project
    )

    for collection in COLLECTIONS:
        upload_collection(db, collection)

    print("\nAll uploads completed!")


if __name__ == "__main__":
    main()
