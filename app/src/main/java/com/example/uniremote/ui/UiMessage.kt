package com.example.uniremote.ui

sealed class UiMessage(val message: String) {
    class Success(msg: String) : UiMessage(msg)
    class Error(msg: String) : UiMessage(msg)
}
