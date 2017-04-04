package net.t53k.worm

interface Walker {
    fun run(loader: Loader, pageHandler: (Page) -> Unit)
}