package net.t53k.worm

interface Node {

  fun subNodes(): Collection<Node>

}
