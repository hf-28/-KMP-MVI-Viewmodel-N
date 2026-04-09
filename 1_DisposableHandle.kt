package com.beno

// Thin wrapper so we don't expose all of kotlinx.coroutines to Swift
interface DisposableHandle : kotlinx.coroutines.DisposableHandle

fun DisposableHandle(block: () -> Unit): DisposableHandle = object : DisposableHandle {
    override fun dispose() = block()
}

operator fun DisposableHandle.plus(other: DisposableHandle): DisposableHandle =
    DisposableHandle { dispose(); other.dispose() }
