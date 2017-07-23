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
val resources = mutableSetOf<Resource>()
val crawler = Crawler(worker = 4,
            onNode = { node -> resources += node.resource },
            linkFilter = { it.startsWith(base) })
val pendigResources = crawler.start(listOf(seed), MilliSecondsTimeout(timeout))
println(resources)
```
