package net.t53k.worm

interface Page {

  fun url(): String

  fun subPages(): Collection<Page>

}
