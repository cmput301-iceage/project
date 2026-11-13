# project

## Generating KDocs

API docs are generated from KDoc comments with [Dokka](https://kotlinlang.org/docs/dokka-introduction.html):

```sh
./gradlew :app:dokkaGeneratePublicationHtml
```

Then open `app/build/dokka/html/index.html` in a browser.

## Winner booking

Winners open an invitation from **My lotteries**, choose an included campsite and
check-in/checkout dates, read its rules and cancellation policy, then explicitly
consent and confirm. They can instead decline after a confirmation prompt.
Successful bookings have a persisted confirmation ID and can be reopened.

- The lottery stay period includes its final **night**, matching campsite date
  search. Checkout is exclusive and may be the next day.
- Calendar dates in new reservations use UTC midnight, and reservation displays
  use UTC so device timezone changes cannot move a stay to the previous day.
  Legacy Canadian-local-midnight timestamps normalize to the same date. Arbitrary
  legacy timezone intent cannot be recovered without an original timezone field.
- Each win creates at most one immutable reservation. Cancellation preserves that
  history rather than granting a second invitation. Other winners can use nights
  released by an entrant cancellation or owner rejection.
- Confirmation checks the current winner, campsite ownership, availability,
  price, and the exact terms displayed for consent. It atomically writes the
  reservation, per-night claims in `campsites/{id}/bookingDays/{yyyy-MM-dd}`, and
  the lottery's `bookedEntrantIds`.
- Every booking writer must follow this claim protocol to serialize competing
  bookings. Existing reservations are checked too, but a separate legacy writer
  bypassing the protocol cannot be made race-safe by a client-side query.
  Firebase rules must permit the intended claim operations and independently
  enforce authorization; device IDs and client validation alone are not a
  server-side security boundary.

### Booking tests

Run JVM policy tests with `bash ./gradlew :app:testDebugUnitTest`.
`BookingScreenTest` uses an injected fake repository and never accesses Firebase.
`FirestoreBookingRepositoryTest` is skipped unless explicitly given
`bookingEmulatorHost` (`127.0.0.1`, `localhost`, or `10.0.2.2`) and optionally
`bookingEmulatorPort` (default `8188`) as instrumentation arguments. It always
creates a named Firebase app for project `demo-winner-booking`, never a live project.

For a USB phone, forward port `8188` with `adb reverse tcp:8188 tcp:8188`, start a
local Firestore emulator on that port, and run the test class with:

```sh
bash ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.campsitelottery.FirestoreBookingRepositoryTest \
  -Pandroid.testInstrumentationRunnerArguments.bookingEmulatorHost=127.0.0.1 \
  -Pandroid.testInstrumentationRunnerArguments.bookingEmulatorPort=8188
```

Use a separately identified test APK when testing on a phone with an existing app
installation, to avoid replacing its local data. Emulator tests cover competing
winners, retries, decline/confirm races, legacy reservations, stale terms,
cancelled claims, malformed records, and persisted confirmations.
