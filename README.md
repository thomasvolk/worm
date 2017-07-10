Worm
=====

Worm is a small and simple crawler library written in Kotlin.

Install
-------

```
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile 'com.github.thomasvolk:worm:-SNAPSHOT'
}
```


API
---

```kotlin
val pages = mutableSetOf<Page>()
val crawler = CrawlerBuilder()
        .worker(4)
        .onPage { page -> pages += page }
        .withLinkFilter { it.startsWith("http://example.com") }
        .build()
val pendigPages = crawler.start(listOf("http://example.com"), MilliSecondsTimeout(5000))
println(pages)
```
