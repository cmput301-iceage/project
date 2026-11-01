# project

## Generating KDocs

API docs are generated from KDoc comments with [Dokka](https://kotlinlang.org/docs/dokka-introduction.html):

```sh
./gradlew :app:dokkaGeneratePublicationHtml
```

Then open `app/build/dokka/html/index.html` in a browser.
